package com.accesschecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ZygiskChecker {

    public enum Status { DETECTED, SUSPECTED, CLEAN, UNKNOWN }

    public static class Result {
        public Status  status            = Status.UNKNOWN;
        public boolean zygiskInMaps      = false;
        public boolean riruInMaps        = false;
        public boolean shamikoInMaps     = false;
        public boolean lsposedProcess    = false;
        public boolean magiskSocket      = false;
        public boolean magiskMounts      = false;
        public boolean proc1Maps         = false;
        public boolean suspiciousFd      = false;
        public boolean tracingActive     = false;
        public int     tracerPid         = -1;
        public int     confidence        = 0;  // 0-100 — certainty that hooking is active
        public final List<String> lines  = new ArrayList<>();
    }

    public static Result check() {
        Result r = new Result();

        // ── 1. /proc/self/maps for injected native libraries ──────────────
        r.zygiskInMaps  = NativeChecker.safeZygiskInMaps();
        String mapsHits = NativeChecker.safeMapsMatches();
        if (!mapsHits.isEmpty()) {
            r.riruInMaps    = mapsHits.contains("riru");
            r.shamikoInMaps = mapsHits.contains("shamiko");
        }

        // ── 2. /proc/1/maps (init process — Magisk runs as init child) ────
        r.proc1Maps = NativeChecker.safeProc1Maps();

        // ── 3. Magisk abstract socket ─────────────────────────────────────
        r.magiskSocket = NativeChecker.safeMagiskSocket();

        // ── 4. Suspicious bind-mounts ─────────────────────────────────────
        r.magiskMounts = NativeChecker.safeMagiskMounts();

        // ── 5. LSPosed daemon process scan ────────────────────────────────
        r.lsposedProcess = detectProcess("lspd", "lsposed");

        // ── 6. TracerPid ──────────────────────────────────────────────────
        r.tracerPid    = NativeChecker.safeTracerPid();
        r.tracingActive = r.tracerPid > 0;

        // ── 7. Suspicious file descriptors ────────────────────────────────
        r.suspiciousFd = checkSuspiciousFd();

        // ── Confidence score ──────────────────────────────────────────────
        int conf = 0;
        if (r.zygiskInMaps)   conf += 35;
        if (r.magiskSocket)   conf += 25;
        if (r.magiskMounts)   conf += 20;
        if (r.lsposedProcess) conf += 20;
        if (r.riruInMaps)     conf += 20;
        if (r.proc1Maps)      conf += 15;
        if (r.shamikoInMaps)  conf += 15;
        if (r.suspiciousFd)   conf += 10;
        if (r.tracingActive)  conf += 10;
        r.confidence = Math.min(100, conf);

        // ── Status ────────────────────────────────────────────────────────
        if (r.zygiskInMaps || r.lsposedProcess || r.riruInMaps) {
            r.status = Status.DETECTED;
        } else if (r.magiskSocket || r.magiskMounts || r.shamikoInMaps
                || r.proc1Maps || r.suspiciousFd) {
            r.status = Status.SUSPECTED;
        } else {
            r.status = Status.CLEAN;
        }

        // ── Detail lines ──────────────────────────────────────────────────
        r.lines.add(fmt("zygisk in maps   ", !r.zygiskInMaps));
        r.lines.add(fmt("riru in maps     ", !r.riruInMaps));
        r.lines.add(fmt("shamiko in maps  ", !r.shamikoInMaps));
        r.lines.add(fmt("lspd process     ", !r.lsposedProcess));
        r.lines.add(fmt("magisk socket    ", !r.magiskSocket));
        r.lines.add(fmt("magisk mounts    ", !r.magiskMounts));
        r.lines.add(fmt("proc/1 maps      ", !r.proc1Maps));
        r.lines.add(fmt("suspicious fd    ", !r.suspiciousFd));
        r.lines.add("tracer pid       : " + (r.tracingActive ? "ACTIVE pid=" + r.tracerPid : "none"));
        if (!NativeChecker.isAvailable())
            r.lines.add("NOTE: native library unavailable — map/socket checks skipped");

        return r;
    }

    /* Walks /proc looking for a process whose comm or cmdline matches any keyword. */
    private static boolean detectProcess(String... keywords) {
        File[] entries = new File("/proc").listFiles(
                f -> f.isDirectory() && f.getName().matches("\\d+"));
        if (entries == null) return false;

        for (File pidDir : entries) {
            try {
                String comm    = readFirstLine(new File(pidDir, "comm"));
                String cmdline = readFirstLine(new File(pidDir, "cmdline"));
                if (cmdline != null) cmdline = cmdline.replace('\0', ' ');

                for (String kw : keywords) {
                    if ((comm    != null && comm.contains(kw))
                     || (cmdline != null && cmdline.contains(kw)))
                        return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    /* Resolves each fd symlink under /proc/self/fd and flags suspicious targets. */
    private static boolean checkSuspiciousFd() {
        File[] fds = new File("/proc/self/fd").listFiles();
        if (fds == null) return false;

        String[] suspects = { "magisk", "zygisk", "riru", "lspd", "shamiko" };
        for (File fd : fds) {
            try {
                String target = fd.getCanonicalPath();
                for (String s : suspects) {
                    if (target.contains(s)) return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static String readFirstLine(File f) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return br.readLine();
        } catch (Exception e) { return null; }
    }

    private static String fmt(String label, boolean clean) {
        return (clean ? "[PASS] " : "[FAIL] ") + label + ": " + (clean ? "clean" : "detected");
    }
}
