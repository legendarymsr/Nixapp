package com.accesschecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RootChecker {

    public enum Status { GRANTED, DENIED, UNKNOWN }

    public static class Result {
        public Status  status              = Status.UNKNOWN;
        public String  suPath              = null;
        public String  rootManager         = null;
        public String  rootManagerVersion  = null;
        public boolean execTestPassed      = false;
        public String  execOutput          = null;

        // Native / advanced checks
        public boolean nativeSuPassed      = false;
        public boolean magiskSocketFound   = false;
        public boolean suspiciousMounts    = false;
        public boolean kernelSuVfs         = false;
        public boolean apatchVfs           = false;
        public boolean fuseMounts          = false;
        public String  mountDetails        = "";

        public int     confidence          = 0;   // 0-100
        public final List<String> lines    = new ArrayList<>();
    }

    private static final String[] SU_PATHS = {
        "/su/bin/su", "/system/bin/su", "/system/xbin/su",
        "/sbin/su", "/data/local/xbin/su", "/data/local/bin/su"
    };

    private static final String[][] ROOT_MANAGERS = {
        { "com.topjohnwu.magisk", "Magisk"   },
        { "me.weishu.kernelsu",   "KernelSU" },
        { "me.bmax.apatch",       "APatch"   },
    };

    public static Result check(Context ctx) {
        Result r = new Result();

        // ── 1. su binary scan ─────────────────────────────────────────────
        for (String path : SU_PATHS) {
            if (new File(path).exists()) {
                r.suPath = path;
                r.status = Status.GRANTED;
                break;
            }
        }
        r.lines.add(fmt("su binary       ", r.suPath == null)
                + (r.suPath != null ? " (" + r.suPath + ")" : ""));

        // ── 2. Java Runtime.exec su -c id (hookable by Zygisk) ────────────
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{ "su", "-c", "id" });
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String out = br.readLine();
            proc.waitFor(4, TimeUnit.SECONDS);
            proc.destroy();
            r.execOutput = (out != null) ? out.trim() : "(no output)";
            if (out != null && out.contains("uid=0")) {
                r.execTestPassed = true;
                r.status = Status.GRANTED;
            }
        } catch (Exception e) {
            r.execOutput = "exception: " + e.getMessage();
        }
        r.lines.add(fmt("su exec (Java)  ", !r.execTestPassed)
                + " → " + r.execOutput);

        // ── 3. Native fork+execve su (bypasses Zygisk Java hooks) ────────
        r.nativeSuPassed = NativeChecker.safeNativeSuExec();
        if (r.nativeSuPassed) r.status = Status.GRANTED;
        r.lines.add(fmt("su exec (native)", !r.nativeSuPassed));

        // ── 4. Root manager packages ───────────────────────────────────────
        PackageManager pm = ctx.getPackageManager();
        for (String[] mgr : ROOT_MANAGERS) {
            try {
                PackageInfo pi = pm.getPackageInfo(mgr[0], 0);
                r.rootManager        = mgr[1];
                r.rootManagerVersion = pi.versionName;
                r.status = Status.GRANTED;
                break;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        r.lines.add(fmt("root manager    ", r.rootManager == null)
                + (r.rootManager != null ? " (" + r.rootManager + " v" + r.rootManagerVersion + ")" : ""));

        // ── 5. Magisk abstract socket (weak signal — does not alone confirm root) ──
        r.magiskSocketFound = NativeChecker.safeMagiskSocket();
        r.lines.add(fmt("magisk socket   ", !r.magiskSocketFound));

        // ── 6. KernelSU VFS nodes ─────────────────────────────────────────
        r.kernelSuVfs = NativeChecker.safeKernelSU();
        if (r.kernelSuVfs) r.status = Status.GRANTED;
        r.lines.add(fmt("kernelsu vfs    ", !r.kernelSuVfs));

        // ── 7. APatch VFS nodes ───────────────────────────────────────────
        r.apatchVfs = NativeChecker.safeAPatch();
        if (r.apatchVfs) r.status = Status.GRANTED;
        r.lines.add(fmt("apatch vfs      ", !r.apatchVfs));

        // ── 8. Suspicious bind-mounts (weak signal — does not alone confirm root) ──
        r.suspiciousMounts = NativeChecker.safeMagiskMounts();
        r.mountDetails = NativeChecker.safeSuspiciousMounts();
        r.lines.add(fmt("magisk mounts   ", !r.suspiciousMounts));

        // ── 9. FUSE filesystem ────────────────────────────────────────────
        r.fuseMounts = NativeChecker.safeFuse();
        r.lines.add(fmt("fuse fs         ", !r.fuseMounts));

        if (!NativeChecker.isAvailable())
            r.lines.add("NOTE: native library not loaded — socket/mount/maps checks skipped");

        // ── Finalise status ───────────────────────────────────────────────
        // Strong signals alone confirm root. Weak signals (socket, mounts) are
        // corroborating evidence — a false positive on either must not flip the
        // result on a non-rooted device.
        boolean strongRoot = r.suPath != null || r.execTestPassed || r.nativeSuPassed
                || r.rootManager != null || r.kernelSuVfs || r.apatchVfs;
        boolean weakRoot = r.magiskSocketFound || r.suspiciousMounts;

        if (strongRoot)    r.status = Status.GRANTED;
        else if (weakRoot) r.status = Status.UNKNOWN;
        else               r.status = Status.DENIED;

        // ── Confidence score ──────────────────────────────────────────────
        int conf = 0;
        if (r.execTestPassed)      conf += 30;
        if (r.nativeSuPassed)      conf += 25;
        if (r.rootManager != null) conf += 20;
        if (r.suPath != null)      conf += 15;
        if (r.kernelSuVfs)         conf += 15;
        if (r.apatchVfs)           conf += 15;
        if (r.magiskSocketFound)   conf += 10;
        if (r.suspiciousMounts)    conf += 10;
        r.confidence = Math.min(100, conf);

        return r;
    }

    private static String fmt(String label, boolean clean) {
        return (clean ? "[PASS] " : "[FAIL] ") + label + ":";
    }
}
