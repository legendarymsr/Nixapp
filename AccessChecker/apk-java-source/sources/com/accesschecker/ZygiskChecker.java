package com.accesschecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes3.dex */
public class ZygiskChecker {

    public static class Result {
        public Status status = Status.UNKNOWN;
        public boolean zygiskInMaps = false;
        public boolean riruInMaps = false;
        public boolean shamikoInMaps = false;
        public boolean lsposedProcess = false;
        public boolean magiskSocket = false;
        public boolean magiskMounts = false;
        public boolean proc1Maps = false;
        public boolean suspiciousFd = false;
        public boolean tracingActive = false;
        public int tracerPid = -1;
        public int confidence = 0;
        public final List<String> lines = new ArrayList();
    }

    public enum Status {
        DETECTED,
        SUSPECTED,
        CLEAN,
        UNKNOWN
    }

    public static Result check() {
        Result r = new Result();
        r.zygiskInMaps = NativeChecker.safeZygiskInMaps();
        String mapsHits = NativeChecker.safeMapsMatches();
        if (!mapsHits.isEmpty()) {
            r.riruInMaps = mapsHits.contains("riru");
            r.shamikoInMaps = mapsHits.contains("shamiko");
        }
        r.proc1Maps = NativeChecker.safeProc1Maps();
        r.magiskSocket = NativeChecker.safeMagiskSocket();
        r.magiskMounts = NativeChecker.safeMagiskMounts();
        r.lsposedProcess = detectProcess("lspd", "lsposed");
        r.tracerPid = NativeChecker.safeTracerPid();
        r.tracingActive = r.tracerPid > 0;
        r.suspiciousFd = checkSuspiciousFd();
        int conf = r.zygiskInMaps ? 0 + 35 : 0;
        if (r.magiskSocket) {
            conf += 25;
        }
        if (r.magiskMounts) {
            conf += 20;
        }
        if (r.lsposedProcess) {
            conf += 20;
        }
        if (r.riruInMaps) {
            conf += 20;
        }
        if (r.proc1Maps) {
            conf += 15;
        }
        if (r.shamikoInMaps) {
            conf += 15;
        }
        if (r.suspiciousFd) {
            conf += 10;
        }
        if (r.tracingActive) {
            conf += 10;
        }
        r.confidence = Math.min(100, conf);
        if (r.zygiskInMaps || r.lsposedProcess || r.riruInMaps) {
            r.status = Status.DETECTED;
        } else if (r.magiskSocket || r.magiskMounts || r.shamikoInMaps || r.proc1Maps || r.suspiciousFd) {
            r.status = Status.SUSPECTED;
        } else {
            r.status = Status.CLEAN;
        }
        r.lines.add(fmt("zygisk in maps   ", !r.zygiskInMaps));
        r.lines.add(fmt("riru in maps     ", !r.riruInMaps));
        r.lines.add(fmt("shamiko in maps  ", !r.shamikoInMaps));
        r.lines.add(fmt("lspd process     ", !r.lsposedProcess));
        r.lines.add(fmt("magisk socket    ", !r.magiskSocket));
        r.lines.add(fmt("magisk mounts    ", !r.magiskMounts));
        r.lines.add(fmt("proc/1 maps      ", !r.proc1Maps));
        r.lines.add(fmt("suspicious fd    ", true ^ r.suspiciousFd));
        r.lines.add("tracer pid       : " + (r.tracingActive ? "ACTIVE pid=" + r.tracerPid : "none"));
        if (!NativeChecker.isAvailable()) {
            r.lines.add("NOTE: native library unavailable — map/socket checks skipped");
        }
        return r;
    }

    private static boolean detectProcess(String... keywords) {
        File[] entries = new File("/proc").listFiles(new FileFilter() { // from class: com.accesschecker.ZygiskChecker$$ExternalSyntheticLambda0
            @Override // java.io.FileFilter
            public final boolean accept(File file) {
                return ZygiskChecker.lambda$detectProcess$0(file);
            }
        });
        if (entries == null) {
            return false;
        }
        for (File pidDir : entries) {
            try {
                String comm = readFirstLine(new File(pidDir, "comm"));
                String cmdline = readFirstLine(new File(pidDir, "cmdline"));
                if (cmdline != null) {
                    cmdline = cmdline.replace((char) 0, ' ');
                }
                for (String kw : keywords) {
                    if (comm == null || !comm.contains(kw)) {
                        if (cmdline != null && cmdline.contains(kw)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    static /* synthetic */ boolean lambda$detectProcess$0(File f) {
        return f.isDirectory() && f.getName().matches("\\d+");
    }

    private static boolean checkSuspiciousFd() {
        File[] fds = new File("/proc/self/fd").listFiles();
        if (fds == null) {
            return false;
        }
        String[] suspects = {"magisk", "zygisk", "riru", "lspd", "shamiko"};
        for (File fd : fds) {
            try {
                String target = fd.getCanonicalPath();
                for (String s : suspects) {
                    if (target.contains(s)) {
                        return true;
                    }
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    private static String readFirstLine(File f) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            try {
                String readLine = br.readLine();
                br.close();
                return readLine;
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String fmt(String label, boolean clean) {
        return (clean ? "[PASS] " : "[FAIL] ") + label + ": " + (clean ? "clean" : "detected");
    }
}
