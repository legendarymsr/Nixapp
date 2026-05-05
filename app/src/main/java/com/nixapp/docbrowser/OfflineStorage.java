package com.nixapp.docbrowser;

import android.content.Context;
import java.io.File;
import java.io.IOException;

public class OfflineStorage {

    private static final String SENTINEL = ".complete";

    public static File getDocDir(Context context, String docTitle) {
        String dirName = docTitle.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return new File(context.getFilesDir(), "offline_docs/" + dirName);
    }

    /** True only when the sentinel file exists (download fully finished). */
    public static boolean isDownloaded(Context context, String docTitle) {
        return new File(getDocDir(context, docTitle), SENTINEL).exists();
    }

    /** Called by DownloadService on successful completion. */
    public static void markComplete(Context context, String docTitle) {
        try {
            File dir = getDocDir(context, docTitle);
            dir.mkdirs();
            new File(dir, SENTINEL).createNewFile();
        } catch (IOException ignored) {}
    }

    /** Size of the offline copy in bytes, or 0 if not downloaded. */
    public static long sizeBytes(Context context, String docTitle) {
        return dirSize(getDocDir(context, docTitle));
    }

    public static String sizeMb(Context context, String docTitle) {
        long bytes = sizeBytes(context, docTitle);
        if (bytes == 0) return "";
        if (bytes < 1_048_576) return String.format("%.0f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / 1_048_576.0);
    }

    public static void deleteDoc(Context context, String docTitle) {
        deleteRecursive(getDocDir(context, docTitle));
    }

    private static long dirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long total = 0;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            total += f.isDirectory() ? dirSize(f) : f.length();
        }
        return total;
    }

    private static void deleteRecursive(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        file.delete();
    }
}
