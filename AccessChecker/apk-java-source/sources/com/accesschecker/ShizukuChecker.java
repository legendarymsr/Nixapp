package com.accesschecker;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import androidx.core.os.EnvironmentCompat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import rikka.shizuku.Shizuku;

/* loaded from: classes3.dex */
public class ShizukuChecker {
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";

    public static class Result {
        public Status status = Status.UNKNOWN;
        public boolean installed = false;
        public boolean running = false;
        public boolean hasPermission = false;
        public int version = -1;
        public String runMode = null;
        public final List<String> lines = new ArrayList();
    }

    public enum Status {
        AVAILABLE_PERMITTED,
        AVAILABLE_DENIED,
        INSTALLED_STOPPED,
        NOT_INSTALLED,
        UNKNOWN
    }

    public static Result check(Context ctx) {
        Result r = new Result();
        boolean z = true;
        try {
            ctx.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
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
        try {
            r.running = Shizuku.pingBinder();
        } catch (Throwable th) {
            r.running = false;
        }
        r.lines.add("service   : " + (r.running ? "running" : "stopped"));
        if (!r.running) {
            r.status = Status.INSTALLED_STOPPED;
            r.lines.add("version   : service not running");
            r.lines.add("permission: n/a");
            return r;
        }
        try {
            r.version = Shizuku.getVersion();
        } catch (Throwable th2) {
            r.version = -1;
        }
        r.lines.add("version   : " + (r.version > 0 ? "v" + r.version : EnvironmentCompat.MEDIA_UNKNOWN));
        try {
            if (Shizuku.isPreV11()) {
                r.hasPermission = true;
            } else {
                if (Shizuku.checkSelfPermission() != 0) {
                    z = false;
                }
                r.hasPermission = z;
            }
        } catch (Throwable th3) {
            r.hasPermission = false;
        }
        r.lines.add("permission: " + (r.hasPermission ? "granted" : "not granted"));
        r.runMode = detectRunMode();
        r.status = r.hasPermission ? Status.AVAILABLE_PERMITTED : Status.AVAILABLE_DENIED;
        return r;
    }

    static String detectRunMode() {
        boolean isShizuku;
        File procDir = new File("/proc");
        File[] entries = procDir.listFiles(new FileFilter() { // from class: com.accesschecker.ShizukuChecker$$ExternalSyntheticLambda0
            @Override // java.io.FileFilter
            public final boolean accept(File file) {
                return ShizukuChecker.lambda$detectRunMode$0(file);
            }
        });
        String str = null;
        if (entries == null) {
            return null;
        }
        for (File pidDir : entries) {
            try {
                String comm = readFirstLine(new File(pidDir, "comm"));
                String cmdline = readFirstLine(new File(pidDir, "cmdline"));
                if (cmdline != null) {
                    cmdline = cmdline.replace((char) 0, ' ');
                }
                isShizuku = (comm != null && comm.contains("shizuku")) || (cmdline != null && cmdline.contains("shizuku"));
            } catch (Exception e) {
            }
            if (isShizuku) {
                int uid = readUid(new File(pidDir, NotificationCompat.CATEGORY_STATUS));
                return uid == 0 ? "root" : uid == 2000 ? "adb" : "uid:" + uid;
            }
        }
        return str;
    }

    static /* synthetic */ boolean lambda$detectRunMode$0(File f) {
        return f.isDirectory() && f.getName().matches("\\d+");
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

    private static int readUid(File statusFile) throws Exception {
        String line;
        BufferedReader br = new BufferedReader(new FileReader(statusFile));
        do {
            try {
                line = br.readLine();
                if (line == null) {
                    br.close();
                    return -1;
                }
            } catch (Throwable th) {
                try {
                    br.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } while (!line.startsWith("Uid:"));
        String[] parts = line.split("\\s+");
        int parseInt = Integer.parseInt(parts[1]);
        br.close();
        return parseInt;
    }
}
