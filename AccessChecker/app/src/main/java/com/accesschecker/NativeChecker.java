package com.accesschecker;

public class NativeChecker {

    private static boolean loaded = false;

    static {
        try {
            System.loadLibrary("accesschecker_native");
            loaded = true;
        } catch (UnsatisfiedLinkError ignored) {}
    }

    public static boolean isAvailable() { return loaded; }

    // Magisk abstract unix socket (@magisk_service / @magiskd)
    public static native boolean hasMagiskSocket();

    // /proc/self/mounts + /proc/mounts scan for magisk/mirror/worker paths
    public static native boolean hasMagiskMounts();
    public static native String  getSuspiciousMounts();

    // /proc/self/maps scan for injected zygisk/riru/shamiko/lspd libraries
    public static native boolean hasZygiskInMaps();
    public static native String  getMapsMatches();

    // /proc/1/maps — init process maps (best-effort; may be denied on hardened kernels)
    public static native boolean hasProc1MagiskMaps();

    // TracerPid from /proc/self/status (non-zero = process is being traced)
    public static native int     getTracerPid();

    // KernelSU kernel VFS nodes (/sys/kernel/su, /proc/kernelsu, /dev/ksud)
    public static native boolean hasKernelSU();

    // APatch VFS nodes (/proc/apd, /data/adb/apatch)
    public static native boolean hasAPatch();

    // /proc/filesystems fuse entry (Magisk bind-mounts use FUSE)
    public static native boolean hasFuseFilesystem();

    // fork+execve su -c id (bypasses Zygisk Java hooks on Runtime.exec)
    public static native boolean nativeSuExec();

    /* Safe wrappers — return safe defaults if native library failed to load */

    public static boolean safeMagiskSocket()  { return loaded && hasMagiskSocket(); }
    public static boolean safeMagiskMounts()  { return loaded && hasMagiskMounts(); }
    public static boolean safeZygiskInMaps()  { return loaded && hasZygiskInMaps(); }
    public static int     safeTracerPid()     { return loaded ? getTracerPid() : -1; }
    public static boolean safeKernelSU()      { return loaded && hasKernelSU(); }
    public static boolean safeAPatch()        { return loaded && hasAPatch(); }
    public static boolean safeFuse()          { return loaded && hasFuseFilesystem(); }
    public static boolean safeNativeSuExec()  { return loaded && nativeSuExec(); }
    public static boolean safeProc1Maps()     { return loaded && hasProc1MagiskMaps(); }

    public static String safeSuspiciousMounts() {
        if (!loaded) return "";
        String s = getSuspiciousMounts();
        return s != null ? s.trim() : "";
    }

    public static String safeMapsMatches() {
        if (!loaded) return "";
        String s = getMapsMatches();
        return s != null ? s.trim() : "";
    }
}
