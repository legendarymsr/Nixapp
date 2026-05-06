package com.accesschecker;

import androidx.core.os.EnvironmentCompat;
import com.accesschecker.RootChecker;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes3.dex */
public class BootloaderChecker {

    public static class Result {
        public Status status = Status.UNKNOWN;
        public VerifiedBootState verifiedBoot = VerifiedBootState.UNKNOWN;
        public boolean dmVerityEnabled = false;
        public String encryptionState = EnvironmentCompat.MEDIA_UNKNOWN;
        public String encryptionType = EnvironmentCompat.MEDIA_UNKNOWN;
        public boolean debuggable = false;
        public boolean propertiesMasked = false;
        public final List<String> lines = new ArrayList();
    }

    public enum Status {
        LOCKED,
        UNLOCKED,
        UNKNOWN
    }

    public enum VerifiedBootState {
        GREEN,
        YELLOW,
        ORANGE,
        RED,
        UNKNOWN
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static Result check(RootChecker.Result rootResult) {
        String str;
        char c;
        Result r = new Result();
        String cmdline = readProcCmdline();
        boolean z = true;
        if (cmdline != null) {
            if (cmdline.contains("androidboot.verifiedbootstate=green")) {
                r.verifiedBoot = VerifiedBootState.GREEN;
            } else if (cmdline.contains("androidboot.verifiedbootstate=yellow")) {
                r.verifiedBoot = VerifiedBootState.YELLOW;
            } else if (cmdline.contains("androidboot.verifiedbootstate=orange")) {
                r.verifiedBoot = VerifiedBootState.ORANGE;
            } else if (cmdline.contains("androidboot.verifiedbootstate=red")) {
                r.verifiedBoot = VerifiedBootState.RED;
            }
            if (cmdline.contains("androidboot.flash.locked=0")) {
                r.status = Status.UNLOCKED;
            } else if (cmdline.contains("androidboot.flash.locked=1")) {
                r.status = Status.LOCKED;
            }
            if (r.status == Status.UNKNOWN && r.verifiedBoot == VerifiedBootState.ORANGE) {
                r.status = Status.UNLOCKED;
            }
            r.dmVerityEnabled = cmdline.contains("dm-verity") || cmdline.contains("androidboot.veritymode");
        }
        String propVerifiedBoot = getProp("ro.boot.verifiedbootstate");
        String propFlashLocked = getProp("ro.boot.flash.locked");
        String cryptoState = getProp("ro.crypto.state");
        String cryptoType = getProp("ro.crypto.type");
        String verityMode = getProp("ro.verity.mode");
        String buildType = getProp("ro.build.type");
        String debuggableProp = getProp("ro.debuggable");
        if (r.verifiedBoot == VerifiedBootState.UNKNOWN && propVerifiedBoot != null) {
            String lowerCase = propVerifiedBoot.toLowerCase();
            switch (lowerCase.hashCode()) {
                case -1008851410:
                    if (lowerCase.equals("orange")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -734239628:
                    if (lowerCase.equals("yellow")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 112785:
                    if (lowerCase.equals("red")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 98619139:
                    if (lowerCase.equals("green")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    r.verifiedBoot = VerifiedBootState.GREEN;
                    break;
                case 1:
                    r.verifiedBoot = VerifiedBootState.YELLOW;
                    break;
                case 2:
                    r.verifiedBoot = VerifiedBootState.ORANGE;
                    break;
                case 3:
                    r.verifiedBoot = VerifiedBootState.RED;
                    break;
            }
        }
        if (r.status == Status.UNKNOWN) {
            if ("0".equals(propFlashLocked)) {
                r.status = Status.UNLOCKED;
            } else if ("1".equals(propFlashLocked)) {
                r.status = Status.LOCKED;
            }
            if (r.status == Status.UNKNOWN && r.verifiedBoot == VerifiedBootState.ORANGE) {
                r.status = Status.UNLOCKED;
            }
        }
        boolean rootManagerConfirmed = (rootResult == null || !rootResult.execTestPassed || rootResult.rootManager == null) ? false : true;
        boolean rootManagerDetected = (rootResult == null || rootResult.rootManager == null) ? false : true;
        if (rootManagerConfirmed) {
            if (r.status == Status.LOCKED) {
                r.propertiesMasked = true;
            }
            r.status = Status.UNLOCKED;
            if (r.verifiedBoot == VerifiedBootState.GREEN) {
                r.verifiedBoot = VerifiedBootState.ORANGE;
                r.propertiesMasked = true;
            }
        } else if (rootManagerDetected && r.status == Status.LOCKED) {
            r.propertiesMasked = true;
            r.status = Status.UNLOCKED;
            if (r.verifiedBoot == VerifiedBootState.GREEN) {
                r.verifiedBoot = VerifiedBootState.ORANGE;
            }
        }
        if (!r.dmVerityEnabled && "enforcing".equalsIgnoreCase(verityMode)) {
            r.dmVerityEnabled = true;
        }
        r.encryptionState = cryptoState != null ? cryptoState : EnvironmentCompat.MEDIA_UNKNOWN;
        if (cryptoType != null) {
            if ("file".equalsIgnoreCase(cryptoType)) {
                str = "FBE";
            } else {
                str = "block".equalsIgnoreCase(cryptoType) ? "FDE" : cryptoType;
            }
            r.encryptionType = str;
        }
        if (!"1".equals(debuggableProp) && !"userdebug".equals(buildType) && !"eng".equals(buildType)) {
            z = false;
        }
        r.debuggable = z;
        r.lines.add("cmdline flash.locked  : " + extractCmdline(cmdline, "androidboot.flash.locked"));
        r.lines.add("prop flash.locked     : " + nvl(propFlashLocked));
        r.lines.add("cmdline verifiedboot  : " + extractCmdline(cmdline, "androidboot.verifiedbootstate"));
        r.lines.add("prop verifiedboot     : " + nvl(propVerifiedBoot));
        r.lines.add("verified boot state   : " + r.verifiedBoot.name().toLowerCase());
        r.lines.add("dm-verity             : " + (r.dmVerityEnabled ? "enabled" : "not confirmed"));
        r.lines.add("encryption            : " + r.encryptionState + (!EnvironmentCompat.MEDIA_UNKNOWN.equals(r.encryptionType) ? " (" + r.encryptionType + ")" : ""));
        r.lines.add("build type            : " + nvl(buildType));
        r.lines.add("debuggable            : " + r.debuggable);
        if (rootManagerDetected) {
            r.lines.add("root manager          : " + rootResult.rootManager + (rootResult.rootManagerVersion != null ? " v" + rootResult.rootManagerVersion : "") + " (requires unlocked BL)");
        }
        if (r.propertiesMasked) {
            r.lines.add("NOTE: props masked by Magisk — BL + verified boot corrected");
        }
        return r;
    }

    static String getProp(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method m = cls.getMethod("get", String.class, String.class);
            String val = (String) m.invoke(null, key, "");
            if (val == null) {
                return null;
            }
            if (val.isEmpty()) {
                return null;
            }
            return val;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readProcCmdline() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cmdline"));
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

    private static String extractCmdline(String cmdline, String key) {
        if (cmdline == null) {
            return "unavailable";
        }
        int idx = cmdline.indexOf(key + "=");
        if (idx < 0) {
            return "not present in cmdline";
        }
        int start = key.length() + idx + 1;
        int end = cmdline.indexOf(32, start);
        return end < 0 ? cmdline.substring(start) : cmdline.substring(start, end);
    }

    private static String nvl(String s) {
        return s != null ? s : "unavailable";
    }
}
