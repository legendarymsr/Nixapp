package com.accesschecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BootloaderChecker {

    public enum Status { LOCKED, UNLOCKED, UNKNOWN }

    public enum VerifiedBootState { GREEN, YELLOW, ORANGE, RED, UNKNOWN }

    public static class Result {
        public Status status = Status.UNKNOWN;
        public VerifiedBootState verifiedBoot = VerifiedBootState.UNKNOWN;
        public boolean dmVerityEnabled = false;
        public String encryptionState = "unknown";
        public String encryptionType  = "unknown";
        public boolean debuggable = false;
        public final List<String> lines = new ArrayList<>();
    }

    public static Result check() {
        Result r = new Result();

        // ── SystemProperties via reflection ───────────────────────────────
        String verifiedBootState = getProp("ro.boot.verifiedbootstate");
        String flashLocked       = getProp("ro.boot.flash.locked");
        String cryptoState       = getProp("ro.crypto.state");
        String cryptoType        = getProp("ro.crypto.type");
        String verityMode        = getProp("ro.verity.mode");
        String buildType         = getProp("ro.build.type");
        String debuggableProp    = getProp("ro.debuggable");

        // ── Verified boot state ───────────────────────────────────────────
        if (verifiedBootState != null) {
            switch (verifiedBootState.toLowerCase()) {
                case "green":  r.verifiedBoot = VerifiedBootState.GREEN;  break;
                case "yellow": r.verifiedBoot = VerifiedBootState.YELLOW; break;
                case "orange": r.verifiedBoot = VerifiedBootState.ORANGE; break;
                case "red":    r.verifiedBoot = VerifiedBootState.RED;    break;
            }
        }

        // ── Bootloader lock state ─────────────────────────────────────────
        if ("1".equals(flashLocked)) {
            r.status = Status.LOCKED;
        } else if ("0".equals(flashLocked)) {
            r.status = Status.UNLOCKED;
        }
        // Infer from verified boot if flash.locked unavailable
        if (r.status == Status.UNKNOWN) {
            if (r.verifiedBoot == VerifiedBootState.GREEN ||
                    r.verifiedBoot == VerifiedBootState.YELLOW) {
                r.status = Status.LOCKED;
            } else if (r.verifiedBoot == VerifiedBootState.ORANGE) {
                r.status = Status.UNLOCKED;
            }
        }

        // ── /proc/cmdline ─────────────────────────────────────────────────
        String cmdline = readProcCmdline();
        if (cmdline != null) {
            if (r.verifiedBoot == VerifiedBootState.UNKNOWN) {
                if (cmdline.contains("androidboot.verifiedbootstate=green"))
                    r.verifiedBoot = VerifiedBootState.GREEN;
                else if (cmdline.contains("androidboot.verifiedbootstate=yellow"))
                    r.verifiedBoot = VerifiedBootState.YELLOW;
                else if (cmdline.contains("androidboot.verifiedbootstate=orange"))
                    r.verifiedBoot = VerifiedBootState.ORANGE;
                else if (cmdline.contains("androidboot.verifiedbootstate=red"))
                    r.verifiedBoot = VerifiedBootState.RED;
            }
            if (r.status == Status.UNKNOWN) {
                if (cmdline.contains("androidboot.flash.locked=1"))
                    r.status = Status.LOCKED;
                else if (cmdline.contains("androidboot.flash.locked=0"))
                    r.status = Status.UNLOCKED;
            }
            r.dmVerityEnabled = cmdline.contains("dm-verity")
                    || cmdline.contains("androidboot.veritymode");
        }

        // dm-verity from property
        if (!r.dmVerityEnabled && "enforcing".equalsIgnoreCase(verityMode)) {
            r.dmVerityEnabled = true;
        }

        // Encryption
        r.encryptionState = cryptoState != null ? cryptoState : "unknown";
        if (cryptoType != null) {
            r.encryptionType = "file".equalsIgnoreCase(cryptoType) ? "FBE" :
                               "block".equalsIgnoreCase(cryptoType) ? "FDE" : cryptoType;
        }

        // Debuggable build
        r.debuggable = "1".equals(debuggableProp) || "userdebug".equals(buildType)
                || "eng".equals(buildType);

        // ── Detail lines ──────────────────────────────────────────────────
        r.lines.add("flash.locked      : " + nvl(flashLocked));
        r.lines.add("verifiedbootstate : " + nvl(verifiedBootState));
        r.lines.add("verified boot     : " + r.verifiedBoot.name().toLowerCase());
        r.lines.add("dm-verity         : " + (r.dmVerityEnabled ? "enabled" : "not confirmed"));
        r.lines.add("encryption        : " + r.encryptionState
                + (!"unknown".equals(r.encryptionType) ? " (" + r.encryptionType + ")" : ""));
        r.lines.add("build type        : " + nvl(buildType));
        r.lines.add("debuggable        : " + r.debuggable);

        return r;
    }

    static String getProp(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method m = cls.getMethod("get", String.class, String.class);
            String val = (String) m.invoke(null, key, "");
            return (val == null || val.isEmpty()) ? null : val;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readProcCmdline() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cmdline"))) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private static String nvl(String s) { return s != null ? s : "unavailable"; }
}
