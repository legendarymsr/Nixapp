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
        r.lines.add("su binary    : " + (r.suPath != null ? r.suPath : "not found"));
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
        r.lines.add("su -c id     : " + r.execOutput);
        r.lines.add("uid=0 test   : " + (r.execTestPassed ? "PASS" : "FAIL"));
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
        r.lines.add("root manager : " + (r.rootManager != null ? r.rootManager + " v" + r.rootManagerVersion : "none detected"));
        if (r.status == Status.UNKNOWN) {
            r.status = Status.DENIED;
        }
        return r;
    }
}
