package com.accesschecker;

import java.io.BufferedReader;
import java.io.File;
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
        public boolean propertiesMayBeMasked = false;
        public final List<String> lines = new ArrayList<>();
    }

    public static Result check() {
        Result r = new Result();

        // ── 1. /proc/cmdline first — Magisk patches the property system ──
        // but does NOT rewrite the raw kernel cmdline that init reads from.
        // Reading it here gives us the ground truth before any prop hooks.
        String cmdline = readProcCmdline();
        if (cmdline != null) {
            // Verified boot state from cmdline
            if (cmdline.contains("androidboot.verifiedbootstate=green"))
                r.verifiedBoot = VerifiedBootState.GREEN;
            else if (cmdline.contains("androidboot.verifiedbootstate=yellow"))
                r.verifiedBoot = VerifiedBootState.YELLOW;
            else if (cmdline.contains("androidboot.verifiedbootstate=orange"))
                r.verifiedBoot = VerifiedBootState.ORANGE;
            else if (cmdline.contains("androidboot.verifiedbootstate=red"))
                r.verifiedBoot = VerifiedBootState.RED;

            // Bootloader lock from cmdline
            if (cmdline.contains("androidboot.flash.locked=1"))
                r.status = Status.LOCKED;
            else if (cmdline.contains("androidboot.flash.locked=0"))
                r.status = Status.UNLOCKED;

            // Infer lock from verified boot state (orange = custom/unlocked)
            if (r.status == Status.UNKNOWN) {
                if (r.verifiedBoot == VerifiedBootState.GREEN ||
                        r.verifiedBoot == VerifiedBootState.YELLOW)
                    r.status = Status.LOCKED;
                else if (r.verifiedBoot == VerifiedBootState.ORANGE)
                    r.status = Status.UNLOCKED;
            }

            r.dmVerityEnabled = cmdline.contains("dm-verity")
                    || cmdline.contains("androidboot.veritymode");
        }

        // ── 2. SystemProperties via reflection (fill gaps / compare) ──────
        String propVerifiedBoot = getProp("ro.boot.verifiedbootstate");
        String propFlashLocked  = getProp("ro.boot.flash.locked");
        String cryptoState      = getProp("ro.crypto.state");
        String cryptoType       = getProp("ro.crypto.type");
        String verityMode       = getProp("ro.verity.mode");
        String buildType        = getProp("ro.build.type");
        String debuggableProp   = getProp("ro.debuggable");

        // Fill in verified boot from props only if cmdline gave nothing
        if (r.verifiedBoot == VerifiedBootState.UNKNOWN && propVerifiedBoot != null) {
            switch (propVerifiedBoot.toLowerCase()) {
                case "green":  r.verifiedBoot = VerifiedBootState.GREEN;  break;
                case "yellow": r.verifiedBoot = VerifiedBootState.YELLOW; break;
                case "orange": r.verifiedBoot = VerifiedBootState.ORANGE; break;
                case "red":    r.verifiedBoot = VerifiedBootState.RED;    break;
            }
        }

        // Fill in lock status from props only if cmdline gave nothing
        if (r.status == Status.UNKNOWN) {
            if ("1".equals(propFlashLocked))      r.status = Status.LOCKED;
            else if ("0".equals(propFlashLocked)) r.status = Status.UNLOCKED;
            if (r.status == Status.UNKNOWN) {
                if (r.verifiedBoot == VerifiedBootState.GREEN ||
                        r.verifiedBoot == VerifiedBootState.YELLOW)
                    r.status = Status.LOCKED;
                else if (r.verifiedBoot == VerifiedBootState.ORANGE)
                    r.status = Status.UNLOCKED;
            }
        }

        // ── 3. Detect Magisk property masking ────────────────────────────
        // If cmdline says one thing and props say another, props are masked.
        boolean cmdlineSaysLocked   = cmdline != null && cmdline.contains("androidboot.flash.locked=1");
        boolean cmdlineSaysUnlocked = cmdline != null && cmdline.contains("androidboot.flash.locked=0");
        boolean propSaysLocked      = "1".equals(propFlashLocked);
        if (cmdlineSaysUnlocked && propSaysLocked) {
            r.propertiesMayBeMasked = true;
        }
        // Also flag if root is known but verified boot claims green via props yet cmdline says orange
        if ("orange".equalsIgnoreCase(propVerifiedBoot) == false
                && r.verifiedBoot == VerifiedBootState.ORANGE) {
            r.propertiesMayBeMasked = true;
        }

        // ── 4. Additional signals ─────────────────────────────────────────
        // Magisk data dir — existence confirms root+Magisk (almost always unlocked BL)
        boolean magiskDataExists = new File("/data/adb/magisk.db").exists()
                || new File("/data/adb/magisk").isDirectory();
        if (magiskDataExists && r.status == Status.LOCKED) {
            // Magisk present but BL claims locked — high likelihood of prop masking
            r.propertiesMayBeMasked = true;
            r.status = Status.UNLOCKED;
        }

        if (!r.dmVerityEnabled && "enforcing".equalsIgnoreCase(verityMode))
            r.dmVerityEnabled = true;

        r.encryptionState = cryptoState != null ? cryptoState : "unknown";
        if (cryptoType != null)
            r.encryptionType = "file".equalsIgnoreCase(cryptoType) ? "FBE" :
                               "block".equalsIgnoreCase(cryptoType) ? "FDE" : cryptoType;
        r.debuggable = "1".equals(debuggableProp) || "userdebug".equals(buildType)
                || "eng".equals(buildType);

        // ── Detail lines ──────────────────────────────────────────────────
        r.lines.add("cmdline flash.locked  : " + extractCmdlineValue(cmdline, "androidboot.flash.locked"));
        r.lines.add("prop flash.locked     : " + nvl(propFlashLocked));
        r.lines.add("cmdline verifiedboot  : " + extractCmdlineValue(cmdline, "androidboot.verifiedbootstate"));
        r.lines.add("prop verifiedboot     : " + nvl(propVerifiedBoot));
        r.lines.add("verified boot state   : " + r.verifiedBoot.name().toLowerCase());
        r.lines.add("dm-verity             : " + (r.dmVerityEnabled ? "enabled" : "not confirmed"));
        r.lines.add("encryption            : " + r.encryptionState
                + (!"unknown".equals(r.encryptionType) ? " (" + r.encryptionType + ")" : ""));
        r.lines.add("build type            : " + nvl(buildType));
        r.lines.add("debuggable            : " + r.debuggable);
        if (r.propertiesMayBeMasked)
            r.lines.add("NOTE: props may be masked by Magisk");

        return r;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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

    private static String extractCmdlineValue(String cmdline, String key) {
        if (cmdline == null) return "unavailable";
        int idx = cmdline.indexOf(key + "=");
        if (idx < 0) return "not present";
        int start = idx + key.length() + 1;
        int end = cmdline.indexOf(' ', start);
        return end < 0 ? cmdline.substring(start) : cmdline.substring(start, end);
    }

    private static String nvl(String s) { return s != null ? s : "unavailable"; }
}
