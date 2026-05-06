package com.accesschecker;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

public class ShizukuChecker {

    public enum Status {
        AVAILABLE_PERMITTED,   // service running, permission granted
        AVAILABLE_DENIED,      // service running, permission not yet granted
        INSTALLED_STOPPED,     // app installed but service not running
        NOT_INSTALLED,         // Shizuku not installed
        UNKNOWN
    }

    public static class Result {
        public Status status      = Status.UNKNOWN;
        public boolean installed  = false;
        public boolean running    = false;
        public boolean hasPermission = false;
        public int version        = -1;
        public String runMode     = null;  // "adb" | "root" | null
        public final List<String> lines = new ArrayList<>();
    }

    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";

    public static Result check(Context ctx) {
        Result r = new Result();

        // ── 1. Is Shizuku installed? ──────────────────────────────────────
        try {
            ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            r.installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            r.installed = false;
        }
        r.lines.add("installed : " + r.installed);

        if (!r.installed) {
            r.status = Status.NOT_INSTALLED;
            r.lines.add("service   : n/a");
            r.lines.add("version   : n/a");
            r.lines.add("permission: n/a");
            return r;
        }

        // ── 2. Is the Shizuku binder alive? ───────────────────────────────
        try {
            r.running = Shizuku.pingBinder();
        } catch (Throwable t) {
            r.running = false;
        }
        r.lines.add("service   : " + (r.running ? "running" : "stopped"));

        if (!r.running) {
            r.status = Status.INSTALLED_STOPPED;
            r.lines.add("version   : service not running");
            r.lines.add("permission: n/a");
            return r;
        }

        // ── 3. Version ────────────────────────────────────────────────────
        try {
            r.version = Shizuku.getVersion();
        } catch (Throwable t) {
            r.version = -1;
        }
        r.lines.add("version   : " + (r.version > 0 ? "v" + r.version : "unknown"));

        // ── 4. Permission ─────────────────────────────────────────────────
        try {
            if (Shizuku.isPreV11()) {
                r.hasPermission = true;
            } else {
                r.hasPermission = Shizuku.checkSelfPermission()
                        == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Throwable t) {
            r.hasPermission = false;
        }
        r.lines.add("permission: " + (r.hasPermission ? "granted" : "not granted"));

        // ── 5. Run mode: scan /proc for the Shizuku server process ────────
        // Shizuku running via ADB  → server process UID = 2000 (shell)
        // Shizuku running via root → server process UID = 0    (root)
        r.runMode = detectRunMode();

        // ── 6. Status ─────────────────────────────────────────────────────
        r.status = r.hasPermission ? Status.AVAILABLE_PERMITTED : Status.AVAILABLE_DENIED;

        return r;
    }

    /**
     * Walks /proc looking for a process whose comm or cmdline contains "shizuku",
     * then reads its real UID from /proc/[pid]/status.
     */
    static String detectRunMode() {
        File procDir = new File("/proc");
        File[] entries = procDir.listFiles(f -> f.isDirectory() && f.getName().matches("\\d+"));
        if (entries == null) return null;

        for (File pidDir : entries) {
            try {
                // /proc/[pid]/comm — short process name (max 15 chars, no nulls)
                String comm = readFirstLine(new File(pidDir, "comm"));
                // /proc/[pid]/cmdline — full argv, args separated by null bytes
                String cmdline = readFirstLine(new File(pidDir, "cmdline"));
                if (cmdline != null) cmdline = cmdline.replace('\0', ' ');

                boolean isShizuku = (comm != null && comm.contains("shizuku"))
                        || (cmdline != null && cmdline.contains("shizuku"));
                if (!isShizuku) continue;

                int uid = readUid(new File(pidDir, "status"));
                if (uid == 0)    return "root";
                if (uid == 2000) return "adb";
                // Some other uid — still Shizuku, report it
                return "uid:" + uid;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String readFirstLine(File f) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    /** Reads the first (real) UID from /proc/[pid]/status — the "Uid:" line. */
    private static int readUid(File statusFile) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(statusFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Uid:")) {
                    // Format: "Uid:\t<real>\t<effective>\t<saved>\t<fs>"
                    String[] parts = line.split("\\s+");
                    return Integer.parseInt(parts[1]);
                }
            }
        }
        return -1;
    }
}
