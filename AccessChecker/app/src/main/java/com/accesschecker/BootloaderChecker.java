package com.accesschecker;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

public class BootloaderChecker {

    public enum Status           { LOCKED, UNLOCKED, UNKNOWN }
    public enum VerifiedBootState { GREEN, YELLOW, ORANGE, RED, UNKNOWN }

    public static class Result {
        public Status            status          = Status.UNKNOWN;
        public VerifiedBootState verifiedBoot    = VerifiedBootState.UNKNOWN;
        public boolean           dmVerityEnabled = false;
        public String            encryptionState = "unknown";
        public String            encryptionType  = "unknown";
        public boolean           debuggable      = false;
        public boolean           propertiesMasked = false;
        public boolean           testKeys        = false;
        public String            hwAttestation   = "unknown";  // "hardware-backed" / "software-only" / "unavailable"
        public int               confidence      = 0;          // 0-100
        public final List<String> lines          = new ArrayList<>();
    }

    public static Result check(RootChecker.Result rootResult) {
        Result r = new Result();

        // ── 1. /proc/cmdline ──────────────────────────────────────────────
        // Magisk patches the property system via resetprop but does NOT rewrite
        // the raw kernel cmdline. On DTB-based devices this field may be absent.
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

            if      (cmdline.contains("androidboot.flash.locked=0")) r.status = Status.UNLOCKED;
            else if (cmdline.contains("androidboot.flash.locked=1")) r.status = Status.LOCKED;

            if (r.status == Status.UNKNOWN && r.verifiedBoot == VerifiedBootState.ORANGE)
                r.status = Status.UNLOCKED;

            r.dmVerityEnabled = cmdline.contains("dm-verity")
                    || cmdline.contains("androidboot.veritymode");
        }

        // ── 2. System properties (may be masked by Magisk/Tricky Store) ──
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
            if      ("0".equals(propFlashLocked)) r.status = Status.UNLOCKED;
            else if ("1".equals(propFlashLocked)) r.status = Status.LOCKED;
            if (r.status == Status.UNKNOWN && r.verifiedBoot == VerifiedBootState.ORANGE)
                r.status = Status.UNLOCKED;
        }

        // ── 3. Root manager inference ─────────────────────────────────────
        // Magisk / KernelSU / APatch all require an unlocked bootloader to install.
        boolean rootManagerConfirmed = rootResult != null
                && rootResult.execTestPassed && rootResult.rootManager != null;
        boolean rootManagerDetected  = rootResult != null && rootResult.rootManager != null;
        // Native checks — only use signals that prove kernel-level root beyond doubt.
        // magiskSocketFound is deliberately excluded: it's a weak signal with known
        // false positives on non-rooted devices and must not cascade to unlock inference.
        boolean rootNativeConfirmed  = rootResult != null
                && (rootResult.nativeSuPassed
                    || rootResult.kernelSuVfs
                    || rootResult.apatchVfs);

        if (rootManagerConfirmed || rootNativeConfirmed) {
            if (r.status == Status.LOCKED) r.propertiesMasked = true;
            r.status = Status.UNLOCKED;
            if (r.verifiedBoot == VerifiedBootState.GREEN) {
                r.verifiedBoot   = VerifiedBootState.ORANGE;
                r.propertiesMasked = true;
            }
        } else if (rootManagerDetected && r.status == Status.LOCKED) {
            r.propertiesMasked = true;
            r.status = Status.UNLOCKED;
            if (r.verifiedBoot == VerifiedBootState.GREEN)
                r.verifiedBoot = VerifiedBootState.ORANGE;
        }

        // ── 4. Build.TAGS (test-keys = custom/unsigned build) ────────────
        r.testKeys = "test-keys".equals(Build.TAGS);

        // ── 5. Hardware-backed key attestation ────────────────────────────
        // Works offline — no internet or Play Services required.
        // On devices with an unlocked bootloader the TEE is still accessible,
        // but the attestation certificate won't chain to Google's root CA.
        r.hwAttestation = checkHardwareAttestation();

        // ── 6. Remaining fields ───────────────────────────────────────────
        if (!r.dmVerityEnabled && "enforcing".equalsIgnoreCase(verityMode))
            r.dmVerityEnabled = true;

        r.encryptionState = cryptoState != null ? cryptoState : "unknown";
        if (cryptoType != null)
            r.encryptionType = "file".equalsIgnoreCase(cryptoType)  ? "FBE" :
                               "block".equalsIgnoreCase(cryptoType) ? "FDE" : cryptoType;

        r.debuggable = "1".equals(debuggableProp)
                || "userdebug".equals(buildType) || "eng".equals(buildType);

        // ── Confidence score ──────────────────────────────────────────────
        int conf = 0;
        if (r.status == Status.UNLOCKED) {
            conf += 40;
            if (rootManagerConfirmed || rootNativeConfirmed) conf += 30;
            else if (rootManagerDetected)                    conf += 20;
            if (r.propertiesMasked)                         conf += 20;
            if (r.verifiedBoot == VerifiedBootState.ORANGE) conf += 10;
        }
        r.confidence = Math.min(100, conf);

        // ── Detail lines ──────────────────────────────────────────────────
        r.lines.add("[" + (cmdline != null ? "INFO" : "MISS") + "] /proc/cmdline         : "
                + (cmdline != null ? "readable" : "unavailable (DTB device)"));
        r.lines.add("cmdline flash.locked   : " + extractCmdline(cmdline, "androidboot.flash.locked"));
        r.lines.add("prop flash.locked      : " + nvl(propFlashLocked));
        r.lines.add("cmdline verifiedboot   : " + extractCmdline(cmdline, "androidboot.verifiedbootstate"));
        r.lines.add("prop verifiedboot      : " + nvl(propVerifiedBoot));
        if (r.propertiesMasked)
            r.lines.add("[FAIL] prop masking      : Magisk/Tricky Store detected — props corrected");
        r.lines.add("verified boot state    : " + r.verifiedBoot.name().toLowerCase());
        r.lines.add("dm-verity              : " + (r.dmVerityEnabled ? "enabled" : "not confirmed"));
        r.lines.add("encryption             : " + r.encryptionState
                + (!"unknown".equals(r.encryptionType) ? " (" + r.encryptionType + ")" : ""));
        r.lines.add("build type             : " + nvl(buildType));
        r.lines.add("build tags             : " + Build.TAGS
                + (r.testKeys ? "  ← CUSTOM/UNSIGNED" : ""));
        r.lines.add("debuggable             : " + r.debuggable);
        r.lines.add("hw attestation         : " + r.hwAttestation);
        if (rootManagerDetected)
            r.lines.add("root manager           : " + rootResult.rootManager
                    + (rootResult.rootManagerVersion != null ? " v" + rootResult.rootManagerVersion : "")
                    + " (requires unlocked BL)");

        return r;
    }

    // ── Hardware-backed key attestation ───────────────────────────────────────
    // Generates a temporary EC key pair in AndroidKeyStore and checks whether
    // it is stored inside the device's TEE or StrongBox hardware module.
    private static String checkHardwareAttestation() {
        try {
            String alias = "ac_attest_tmp";
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build());
            kpg.generateKeyPair();

            PrivateKey pk = (PrivateKey) ks.getKey(alias, null);
            KeyFactory kf = KeyFactory.getInstance(pk.getAlgorithm(), "AndroidKeyStore");
            KeyInfo ki = kf.getKeySpec(pk, KeyInfo.class);

            boolean hwBacked;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                hwBacked = ki.getSecurityLevel() != KeyProperties.SECURITY_LEVEL_SOFTWARE;
            } else {
                hwBacked = ki.isInsideSecureHardware();
            }

            ks.deleteEntry(alias);
            return hwBacked ? "hardware-backed (TEE/StrongBox)" : "software-only";
        } catch (Exception e) {
            return "unavailable: " + e.getClass().getSimpleName();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
