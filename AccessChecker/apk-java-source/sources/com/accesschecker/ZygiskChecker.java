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
        Result result = new Result();
        result.zygiskInMaps = NativeChecker.safeZygiskInMaps();
        String safeMapsMatches = NativeChecker.safeMapsMatches();
        if (!safeMapsMatches.isEmpty()) {
            result.riruInMaps = safeMapsMatches.contains("riru");
            result.shamikoInMaps = safeMapsMatches.contains("shamiko");
        }
        result.proc1Maps = NativeChecker.safeProc1Maps();
        result.magiskSocket = NativeChecker.safeMagiskSocket();
        result.magiskMounts = NativeChecker.safeMagiskMounts();
        result.lsposedProcess = detectProcess("lspd", "lsposed");
        result.tracerPid = NativeChecker.safeTracerPid();
        result.tracingActive = result.tracerPid > 0;
        result.suspiciousFd = checkSuspiciousFd();
        int i = result.zygiskInMaps ? 0 + 35 : 0;
        if (result.magiskSocket) {
            i += 25;
        }
        if (result.magiskMounts) {
            i += 20;
        }
        if (result.lsposedProcess) {
            i += 20;
        }
        if (result.riruInMaps) {
            i += 20;
        }
        if (result.proc1Maps) {
            i += 15;
        }
        if (result.shamikoInMaps) {
            i += 15;
        }
        if (result.suspiciousFd) {
            i += 10;
        }
        if (result.tracingActive) {
            i += 10;
        }
        result.confidence = Math.min(100, i);
        int i2 = (result.magiskSocket ? 1 : 0) + (result.magiskMounts ? 1 : 0) + (result.shamikoInMaps ? 1 : 0) + (result.proc1Maps ? 1 : 0) + (result.suspiciousFd ? 1 : 0);
        if (result.zygiskInMaps || result.lsposedProcess || result.riruInMaps) {
            result.status = Status.DETECTED;
        } else if (i2 >= 2) {
            result.status = Status.SUSPECTED;
        } else {
            result.status = Status.CLEAN;
        }
        result.lines.add(fmt("zygisk in maps   ", !result.zygiskInMaps));
        result.lines.add(fmt("riru in maps     ", !result.riruInMaps));
        result.lines.add(fmt("shamiko in maps  ", !result.shamikoInMaps));
        result.lines.add(fmt("lspd process     ", !result.lsposedProcess));
        result.lines.add(fmt("magisk socket    ", !result.magiskSocket));
        result.lines.add(fmt("magisk mounts    ", !result.magiskMounts));
        result.lines.add(fmt("proc/1 maps      ", !result.proc1Maps));
        result.lines.add(fmt("suspicious fd    ", true ^ result.suspiciousFd));
        result.lines.add("tracer pid       : " + (result.tracingActive ? "ACTIVE pid=" + result.tracerPid : "none"));
        if (!NativeChecker.isAvailable()) {
            result.lines.add("NOTE: native library unavailable — map/socket checks skipped");
        }
        return result;
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
