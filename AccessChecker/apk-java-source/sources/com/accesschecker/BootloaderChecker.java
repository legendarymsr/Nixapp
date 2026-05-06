package com.accesschecker;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import androidx.core.os.EnvironmentCompat;
import com.accesschecker.RootChecker;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
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
        public boolean testKeys = false;
        public String hwAttestation = EnvironmentCompat.MEDIA_UNKNOWN;
        public int confidence = 0;
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
        boolean rootNativeConfirmed = rootResult != null && (rootResult.nativeSuPassed || rootResult.kernelSuVfs || rootResult.apatchVfs);
        if (rootManagerConfirmed || rootNativeConfirmed) {
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
        r.testKeys = "test-keys".equals(Build.TAGS);
        r.hwAttestation = checkHardwareAttestation();
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
        r.debuggable = "1".equals(debuggableProp) || "userdebug".equals(buildType) || "eng".equals(buildType);
        int conf = 0;
        if (r.status == Status.UNLOCKED) {
            conf = 0 + 40;
            if (rootManagerConfirmed || rootNativeConfirmed) {
                conf += 30;
            } else if (rootManagerDetected) {
                conf += 20;
            }
            if (r.propertiesMasked) {
                conf += 20;
            }
            if (r.verifiedBoot == VerifiedBootState.ORANGE) {
                conf += 10;
            }
        }
        r.confidence = Math.min(100, conf);
        r.lines.add("[" + (cmdline != null ? "INFO" : "MISS") + "] /proc/cmdline         : " + (cmdline != null ? "readable" : "unavailable (DTB device)"));
        r.lines.add("cmdline flash.locked   : " + extractCmdline(cmdline, "androidboot.flash.locked"));
        r.lines.add("prop flash.locked      : " + nvl(propFlashLocked));
        r.lines.add("cmdline verifiedboot   : " + extractCmdline(cmdline, "androidboot.verifiedbootstate"));
        r.lines.add("prop verifiedboot      : " + nvl(propVerifiedBoot));
        if (r.propertiesMasked) {
            r.lines.add("[FAIL] prop masking      : Magisk/Tricky Store detected — props corrected");
        }
        r.lines.add("verified boot state    : " + r.verifiedBoot.name().toLowerCase());
        r.lines.add("dm-verity              : " + (r.dmVerityEnabled ? "enabled" : "not confirmed"));
        r.lines.add("encryption             : " + r.encryptionState + (!EnvironmentCompat.MEDIA_UNKNOWN.equals(r.encryptionType) ? " (" + r.encryptionType + ")" : ""));
        r.lines.add("build type             : " + nvl(buildType));
        r.lines.add("build tags             : " + Build.TAGS + (r.testKeys ? "  ← CUSTOM/UNSIGNED" : ""));
        r.lines.add("debuggable             : " + r.debuggable);
        r.lines.add("hw attestation         : " + r.hwAttestation);
        if (rootManagerDetected) {
            r.lines.add("root manager           : " + rootResult.rootManager + (rootResult.rootManagerVersion != null ? " v" + rootResult.rootManagerVersion : "") + " (requires unlocked BL)");
        }
        return r;
    }

    private static String checkHardwareAttestation() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
            boolean hwBacked = true;
            kpg.initialize(new KeyGenParameterSpec.Builder("ac_attest_tmp", 12).setDigests("SHA-256").build());
            kpg.generateKeyPair();
            PrivateKey pk = (PrivateKey) ks.getKey("ac_attest_tmp", null);
            KeyFactory kf = KeyFactory.getInstance(pk.getAlgorithm(), "AndroidKeyStore");
            KeyInfo ki = (KeyInfo) kf.getKeySpec(pk, KeyInfo.class);
            if (Build.VERSION.SDK_INT >= 31) {
                if (ki.getSecurityLevel() == 0) {
                    hwBacked = false;
                }
            } else {
                hwBacked = ki.isInsideSecureHardware();
            }
            ks.deleteEntry("ac_attest_tmp");
            return hwBacked ? "hardware-backed (TEE/StrongBox)" : "software-only";
        } catch (Exception e) {
            return "unavailable: " + e.getClass().getSimpleName();
        }
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
