package com.accesschecker;

/* loaded from: classes3.dex */
public class NativeChecker {
    private static boolean loaded;

    public static native String getMapsMatches();

    public static native String getSuspiciousMounts();

    public static native int getTracerPid();

    public static native boolean hasAPatch();

    public static native boolean hasFuseFilesystem();

    public static native boolean hasKernelSU();

    public static native boolean hasMagiskMounts();

    public static native boolean hasMagiskSocket();

    public static native boolean hasProc1MagiskMaps();

    public static native boolean hasZygiskInMaps();

    public static native boolean nativeSuExec();

    static {
        loaded = false;
        try {
            System.loadLibrary("accesschecker_native");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
        }
    }

    public static boolean isAvailable() {
        return loaded;
    }

    public static boolean safeMagiskSocket() {
        return loaded && hasMagiskSocket();
    }

    public static boolean safeMagiskMounts() {
        return loaded && hasMagiskMounts();
    }

    public static boolean safeZygiskInMaps() {
        return loaded && hasZygiskInMaps();
    }

    public static int safeTracerPid() {
        if (loaded) {
            return getTracerPid();
        }
        return -1;
    }

    public static boolean safeKernelSU() {
        return loaded && hasKernelSU();
    }

    public static boolean safeAPatch() {
        return loaded && hasAPatch();
    }

    public static boolean safeFuse() {
        return loaded && hasFuseFilesystem();
    }

    public static boolean safeNativeSuExec() {
        return loaded && nativeSuExec();
    }

    public static boolean safeProc1Maps() {
        return loaded && hasProc1MagiskMaps();
    }

    public static String safeSuspiciousMounts() {
        String s;
        return (loaded && (s = getSuspiciousMounts()) != null) ? s.trim() : "";
    }

    public static String safeMapsMatches() {
        String s;
        return (loaded && (s = getMapsMatches()) != null) ? s.trim() : "";
    }
}
