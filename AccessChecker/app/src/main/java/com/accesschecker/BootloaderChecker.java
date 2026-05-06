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
        public boolean propertiesMasked = false;
        public final List<String> lines = new ArrayList<>();
    }

    /**
     * @param rootResult pass the already-completed RootChecker result so we can
     *                   use confirmed root as a bootloader-unlock signal. Magisk,
     *                   KernelSU, and APatch all require an unlocked bootloader.
     */
    public static Result check(RootChecker.Result rootResult) {
        Result r = new Result();

        // ── 1. /proc/cmdline ─────────────────────────────────────────────
        // Magisk patches the property system via resetprop but does NOT rewrite
        // the raw kernel cmdline. On devices that pass bootloader state via cmdline
        // (not DTB) this is the ground truth.
        String cmdline = readProcCmdline();
        if (cmdline != null) {
            if      (cmdline.contains("androidboot.verifiedbootstate=green"))
                r.verifiedBoot = VerifiedBootState.GREEN;
            else if (cmdline.contains("androidboot.verifiedbootstate=yellow"))
                r.verifiedBoot = VerifiedBootState.YELLOW;
            else if (cmdline.contains("androidboot.verifiedbootstate=orange"))
                r.verifiedBoot = VerifiedBootState.ORANGE;
            else if (cmdline.contains("androidboot.verifiedbootstate=red"))
                r.verifiedBoot = VerifiedBootState.RED;

            if      (cmdline.contains("androidboot.flash.locked=0"))
                r.status = Status.UNLOCKED;
            else if (cmdline.contains("androidboot.flash.locked=1"))
                r.status = Status.LOCKED;

            // orange verified boot state → user-signed / custom → unlocked
            if (r.status == Status.UNKNOWN && r.verifiedBoot == VerifiedBootState.ORANGE)
                r.status = Status.UNLOCKED;

            r.dmVerityEnabled = cmdline.contains("dm-verity")
                    || cmdline.contains("androidboot.veritymode");
        }

        // ── 2. System properties (fill gaps; unreliable when Magisk is present) ──
        String propVerifiedBoot = getProp("ro.boot.verifiedbootstate");
        String propFlashLocked  = getProp("ro.boot.flash.locked");
        String cryptoState      = getProp("ro.crypto.state");
        String cryptoType       = getProp("ro.crypto.type");
        String verityMode       = getProp("ro.verity.mode");
        String buildType        = getProp("ro.build.type");
        String debuggableProp   = getProp("ro.debuggable");

        if (r.verifiedBoot == VerifiedBootState.UNKNOWN && propVerifiedBoot != null) {
            switch (propVerifiedBoot.toLowerCase()) {
                case "green":  r.verifiedBoot = VerifiedBootState.GREEN;  break;
                case "yellow": r.verifiedBoot = VerifiedBootState.YELLOW; break;
                case "orange": r.verifiedBoot = VerifiedBootState.ORANGE; break;
                case "red":    r.verifiedBoot = VerifiedBootState.RED;    break;
            }
        }
        if (r.status == Status.UNKNOWN) {
            if ("0".equals(propFlashLocked))      r.status = Status.UNLOCKED;
            else if ("1".equals(propFlashLocked)) r.status = Status.LOCKED;
            if (r.status == Status.UNKNOWN && r.verifiedBoot == VerifiedBootState.ORANGE)
                r.status = Status.UNLOCKED;
        }

        // ── 3. Root manager inference (most reliable on Magisk-patched devices) ─
        // Magisk / KernelSU / APatch all require an unlocked bootloader to install.
        // If su -c id returned uid=0 AND a root manager package is present, the
        // bootloader is provably unlocked regardless of what properties/cmdline say.
        boolean rootManagerConfirmed = rootResult != null
                && rootResult.execTestPassed
                && rootResult.rootManager != null;

        // Even just a detected root manager package (even without exec test passing,
        // e.g. if the app is on Magisk Denylist) is a strong signal.
        boolean rootManagerDetected = rootResult != null
                && rootResult.rootManager != null;

        if (rootManagerConfirmed) {
            if (r.status == Status.LOCKED) r.propertiesMasked = true;
            r.status = Status.UNLOCKED;
        } else if (rootManagerDetected && r.status == Status.LOCKED) {
            // Package present but exec test failed (Magisk Denylist hiding su from us).
            // Still treat as unlocked — the package can't install without it.
            r.propertiesMasked = true;
            r.status = Status.UNLOCKED;
        }

        // ── 4. Remaining fields ───────────────────────────────────────────
        if (!r.dmVerityEnabled && "enforcing".equalsIgnoreCase(verityMode))
            r.dmVerityEnabled = true;

        r.encryptionState = cryptoState != null ? cryptoState : "unknown";
        if (cryptoType != null)
            r.encryptionType = "file".equalsIgnoreCase(cryptoType)  ? "FBE" :
                               "block".equalsIgnoreCase(cryptoType) ? "FDE" : cryptoType;

        r.debuggable = "1".equals(debuggableProp)
                || "userdebug".equals(buildType)
                || "eng".equals(buildType);

        // ── Detail lines ──────────────────────────────────────────────────
        r.lines.add("cmdline flash.locked  : " + extractCmdline(cmdline, "androidboot.flash.locked"));
        r.lines.add("prop flash.locked     : " + nvl(propFlashLocked));
        r.lines.add("cmdline verifiedboot  : " + extractCmdline(cmdline, "androidboot.verifiedbootstate"));
        r.lines.add("prop verifiedboot     : " + nvl(propVerifiedBoot));
        r.lines.add("verified boot state   : " + r.verifiedBoot.name().toLowerCase());
        r.lines.add("dm-verity             : " + (r.dmVerityEnabled ? "enabled" : "not confirmed"));
        r.lines.add("encryption            : " + r.encryptionState
                + (!"unknown".equals(r.encryptionType) ? " (" + r.encryptionType + ")" : ""));
        r.lines.add("build type            : " + nvl(buildType));
        r.lines.add("debuggable            : " + r.debuggable);
        if (rootManagerDetected)
            r.lines.add("root manager          : " + rootResult.rootManager
                    + (rootResult.rootManagerVersion != null
                       ? " v" + rootResult.rootManagerVersion : "")
                    + " (requires unlocked BL)");
        if (r.propertiesMasked)
            r.lines.add("NOTE: props masked by Magisk — BL inferred from root manager");

        return r;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    static String getProp(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method m = cls.getMethod("get", String.class, String.class);
            String val = (String) m.invoke(null, key, "");
            return (val == null || val.isEmpty()) ? null : val;
        } catch (Exception e) { return null; }
    }

    private static String readProcCmdline() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cmdline"))) {
            return br.readLine();
        } catch (Exception e) { return null; }
    }

    private static String extractCmdline(String cmdline, String key) {
        if (cmdline == null) return "unavailable";
        int idx = cmdline.indexOf(key + "=");
        if (idx < 0) return "not present in cmdline";
        int start = idx + key.length() + 1;
        int end   = cmdline.indexOf(' ', start);
        return end < 0 ? cmdline.substring(start) : cmdline.substring(start, end);
    }

    private static String nvl(String s) { return s != null ? s : "unavailable"; }
}
