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

/* loaded from: classes3.dex */
public class RootChecker {
    private static final String[] SU_PATHS = {"/su/bin/su", "/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su", "/data/local/bin/su"};
    private static final String[][] ROOT_MANAGERS = {new String[]{"com.topjohnwu.magisk", "Magisk"}, new String[]{"me.weishu.kernelsu", "KernelSU"}, new String[]{"me.bmax.apatch", "APatch"}};

    public static class Result {
        public Status status = Status.UNKNOWN;
        public String suPath = null;
        public String rootManager = null;
        public String rootManagerVersion = null;
        public boolean execTestPassed = false;
        public String execOutput = null;
        public boolean nativeSuPassed = false;
        public boolean magiskSocketFound = false;
        public boolean suspiciousMounts = false;
        public boolean kernelSuVfs = false;
        public boolean apatchVfs = false;
        public boolean fuseMounts = false;
        public String mountDetails = "";
        public int confidence = 0;
        public final List<String> lines = new ArrayList();
    }

    public enum Status {
        GRANTED,
        DENIED,
        UNKNOWN
    }

    public static Result check(Context ctx) {
        Result r = new Result();
        String[] strArr = SU_PATHS;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String path = strArr[i];
            if (!new File(path).exists()) {
                i++;
            } else {
                r.suPath = path;
                r.status = Status.GRANTED;
                break;
            }
        }
        r.lines.add(fmt("su binary       ", r.suPath == null) + (r.suPath != null ? " (" + r.suPath + ")" : ""));
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String out = br.readLine();
            proc.waitFor(4L, TimeUnit.SECONDS);
            proc.destroy();
            r.execOutput = out != null ? out.trim() : "(no output)";
            if (out != null && out.contains("uid=0")) {
                r.execTestPassed = true;
                r.status = Status.GRANTED;
            }
        } catch (Exception e) {
            r.execOutput = "exception: " + e.getMessage();
        }
        r.lines.add(fmt("su exec (Java)  ", !r.execTestPassed) + " → " + r.execOutput);
        r.nativeSuPassed = NativeChecker.safeNativeSuExec();
        if (r.nativeSuPassed) {
            r.status = Status.GRANTED;
        }
        r.lines.add(fmt("su exec (native)", !r.nativeSuPassed));
        PackageManager pm = ctx.getPackageManager();
        for (String[] mgr : ROOT_MANAGERS) {
            try {
                PackageInfo pi = pm.getPackageInfo(mgr[0], 0);
                r.rootManager = mgr[1];
                r.rootManagerVersion = pi.versionName;
                r.status = Status.GRANTED;
                break;
            } catch (PackageManager.NameNotFoundException e2) {
            }
        }
        r.lines.add(fmt("root manager    ", r.rootManager == null) + (r.rootManager != null ? " (" + r.rootManager + " v" + r.rootManagerVersion + ")" : ""));
        r.magiskSocketFound = NativeChecker.safeMagiskSocket();
        if (r.magiskSocketFound) {
            r.status = Status.GRANTED;
        }
        r.lines.add(fmt("magisk socket   ", !r.magiskSocketFound));
        r.kernelSuVfs = NativeChecker.safeKernelSU();
        if (r.kernelSuVfs) {
            r.status = Status.GRANTED;
        }
        r.lines.add(fmt("kernelsu vfs    ", !r.kernelSuVfs));
        r.apatchVfs = NativeChecker.safeAPatch();
        if (r.apatchVfs) {
            r.status = Status.GRANTED;
        }
        r.lines.add(fmt("apatch vfs      ", !r.apatchVfs));
        r.suspiciousMounts = NativeChecker.safeMagiskMounts();
        if (r.suspiciousMounts) {
            r.status = Status.GRANTED;
        }
        r.mountDetails = NativeChecker.safeSuspiciousMounts();
        r.lines.add(fmt("magisk mounts   ", !r.suspiciousMounts));
        r.fuseMounts = NativeChecker.safeFuse();
        r.lines.add(fmt("fuse fs         ", !r.fuseMounts));
        if (!NativeChecker.isAvailable()) {
            r.lines.add("NOTE: native library not loaded — socket/mount/maps checks skipped");
        }
        int conf = r.execTestPassed ? 0 + 30 : 0;
        if (r.nativeSuPassed) {
            conf += 25;
        }
        if (r.magiskSocketFound) {
            conf += 20;
        }
        if (r.rootManager != null) {
            conf += 20;
        }
        if (r.suPath != null) {
            conf += 15;
        }
        if (r.kernelSuVfs) {
            conf += 15;
        }
        if (r.apatchVfs) {
            conf += 15;
        }
        if (r.suspiciousMounts) {
            conf += 10;
        }
        r.confidence = Math.min(100, conf);
        if (r.status == Status.UNKNOWN) {
            r.status = Status.DENIED;
        }
        return r;
    }

    private static String fmt(String label, boolean clean) {
        return (clean ? "[PASS] " : "[FAIL] ") + label + ":";
    }
}
