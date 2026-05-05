package com.nixapp.docbrowser;

import android.content.Context;
import java.io.File;

public class OfflineStorage {

    public static File getDocDir(Context context, String docTitle) {
        String dirName = docTitle.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return new File(context.getFilesDir(), "offline_docs/" + dirName);
    }

    public static boolean isDownloaded(Context context, String docTitle) {
        File dir = getDocDir(context, docTitle);
        return dir.exists() && dir.isDirectory() && dir.list() != null
                && dir.list().length > 0;
    }

    public static void deleteDoc(Context context, String docTitle) {
        File dir = getDocDir(context, docTitle);
        deleteRecursive(dir);
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
