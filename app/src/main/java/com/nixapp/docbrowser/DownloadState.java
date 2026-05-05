package com.nixapp.docbrowser;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DownloadState {

    public static final class Progress {
        public final long bytes;
        public final long total;
        public final int  percent;
        public final String phase;

        Progress(long bytes, long total, String phase) {
            this.bytes   = bytes;
            this.total   = total;
            this.percent = total > 0 ? (int) Math.min(99, (bytes * 100L) / total) : -1;
            this.phase   = phase;
        }

        public String bytesLabel() {
            return toMb(bytes) + (total > 0 ? " / " + toMb(total) : " MB");
        }

        private static String toMb(long b) {
            return String.format("%.1f MB", b / 1_048_576.0);
        }
    }

    private static final Map<String, Progress> map = new ConcurrentHashMap<>();

    public static void update(String title, long bytes, long total, String phase) {
        map.put(title, new Progress(bytes, total, phase));
    }

    public static void remove(String title) {
        map.remove(title);
    }

    public static Progress get(String title) {
        return map.get(title);
    }

    public static boolean isActive(String title) {
        return map.containsKey(title);
    }
}
