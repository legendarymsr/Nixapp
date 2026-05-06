package com.accesschecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

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
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
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
                // Pre-v11: permission is always granted if binder is alive
                r.hasPermission = true;
            } else {
                r.hasPermission = Shizuku.checkSelfPermission()
                        == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Throwable t) {
            r.hasPermission = false;
        }
        r.lines.add("permission: " + (r.hasPermission ? "granted" : "not granted"));

        // ── 5. Status ─────────────────────────────────────────────────────
        r.status = r.hasPermission ? Status.AVAILABLE_PERMITTED : Status.AVAILABLE_DENIED;

        return r;
    }
}
