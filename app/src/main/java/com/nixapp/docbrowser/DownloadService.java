package com.nixapp.docbrowser;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

import okhttp3.*;

@SuppressWarnings("deprecation")
public class DownloadService extends IntentService {

    public static final String EXTRA_DOC_TITLE   = "doc_title";
    public static final String EXTRA_DOC_URL     = "doc_url";
    public static final String ACTION_PROGRESS   = "com.nixapp.docbrowser.DOWNLOAD_PROGRESS";
    public static final String ACTION_COMPLETE   = "com.nixapp.docbrowser.DOWNLOAD_COMPLETE";
    public static final String EXTRA_SUCCESS     = "success";
    public static final String EXTRA_BYTES       = "bytes";
    public static final String EXTRA_TOTAL       = "total";
    public static final String EXTRA_PHASE       = "phase";

    private static final String CHANNEL_ID = "nixdoc_dl";
    private static final int    NOTIF_ID   = 2001;
    private static final String TAG        = "DownloadService";

    // OkHttp client shared across requests in this service run
    private OkHttpClient http;

    public DownloadService() { super("DownloadService"); }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        String title  = intent.getStringExtra(EXTRA_DOC_TITLE);
        String docUrl = intent.getStringExtra(EXTRA_DOC_URL);
        if (title == null || docUrl == null) return;

        http = new OkHttpClient.Builder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                             .header("User-Agent",
                                     "Mozilla/5.0 (Linux; Android 12; NixDocBrowser/1.0)")
                             .build()))
                .build();

        createChannel();
        showNotif(title, "Starting…", 0, -1);
        DownloadState.update(title, 0, -1, "Starting…");

        boolean ok = false;
        try {
            ok = downloadDoc(title, docUrl);
        } catch (Exception e) {
            Log.e(TAG, "Download failed", e);
        }

        DownloadState.remove(title);
        showCompleteNotif(title, ok);
        broadcast(ACTION_COMPLETE, title, 0, 0, ok ? "done" : "failed");

        Intent done = new Intent(ACTION_COMPLETE);
        done.putExtra(EXTRA_DOC_TITLE, title);
        done.putExtra(EXTRA_SUCCESS, ok);
        sendBroadcast(done);
    }

    // ── Main orchestration ────────────────────────────────────────────────

    private boolean downloadDoc(String title, String docUrl) throws Exception {
        File dir = OfflineStorage.getDocDir(this, title);
        deleteRecursive(dir);
        dir.mkdirs();
        new File(dir, "css").mkdirs();

        // Phase 1 — download main HTML (streaming, memory-safe)
        File htmlFile = new File(dir, "index.html");
        progress(title, 0, -1, "Downloading page…");
        long htmlSize = streamToFile(docUrl, htmlFile, (bytes, total) -> {
            // Phase 1 = 0-60% of total progress
            long scaled = total > 0 ? (bytes * 60) / total : -1;
            progress(title, bytes, total, "Downloading page…");
        });
        if (htmlFile.length() == 0) return false;

        // Phase 2 — extract CSS + JS resource URLs from HTML
        progress(title, 0, -1, "Processing resources…");
        List<String> cssUrls = extractResourceUrls(htmlFile, docUrl);
        Log.d(TAG, "Found " + cssUrls.size() + " CSS resources");

        // Phase 3 — download CSS files, build URL→local-path map
        Map<String, String> urlToLocal = new LinkedHashMap<>();
        int idx = 0;
        for (String url : cssUrls) {
            idx++;
            String label = "CSS " + idx + "/" + cssUrls.size();
            progress(title, idx, cssUrls.size(), label);
            String local = downloadCssResource(url, dir);
            if (local != null) urlToLocal.put(url, local);
        }

        // Phase 4 — stream-rewrite HTML: replace CSS URLs + inject dark mode
        progress(title, 0, -1, "Injecting dark theme…");
        File rewritten = new File(dir, "index_rw.html");
        rewriteHtml(htmlFile, rewritten, docUrl, urlToLocal);
        if (!rewritten.renameTo(htmlFile)) {
            // fallback: copy
            copyFile(rewritten, htmlFile);
            rewritten.delete();
        }

        // Done
        OfflineStorage.markComplete(this, title);
        return true;
    }

    // ── Streaming download ────────────────────────────────────────────────

    interface ProgressListener { void onProgress(long bytes, long total); }

    private long streamToFile(String url, File dest, ProgressListener cb) throws IOException {
        Request req = new Request.Builder().url(url).build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code() + " for " + url);
            ResponseBody body = resp.body();
            if (body == null) return 0;
            long contentLen = body.contentLength();
            long total = 0;
            byte[] buf = new byte[65536]; // 64 KB chunks
            try (InputStream in = body.byteStream();
                 FileOutputStream out = new FileOutputStream(dest)) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    total += n;
                    cb.onProgress(total, contentLen);
                }
            }
            return total;
        }
    }

    // ── CSS extraction ────────────────────────────────────────────────────

    private static final Pattern CSS_HREF = Pattern.compile(
            "<link[^>]+rel=[\"']stylesheet[\"'][^>]*href=[\"']([^\"']+)[\"']|" +
            "<link[^>]+href=[\"']([^\"']+\\.css(?:[^\"']*)?)[\"'][^>]*rel=[\"']stylesheet[\"']",
            Pattern.CASE_INSENSITIVE);

    private List<String> extractResourceUrls(File htmlFile, String baseUrl) throws IOException {
        List<String> urls = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(htmlFile), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = CSS_HREF.matcher(line);
                while (m.find()) {
                    String raw = m.group(1) != null ? m.group(1) : m.group(2);
                    if (raw == null) continue;
                    String abs = resolveUrl(baseUrl, raw.trim());
                    if (abs != null && seen.add(abs)) urls.add(abs);
                }
            }
        }
        return urls;
    }

    private String downloadCssResource(String url, File baseDir) {
        try {
            // Skip obviously external/large CDN resources > 2 MB
            String filename = "css_" + Math.abs(url.hashCode()) + ".css";
            File dest = new File(baseDir, "css/" + filename);
            streamToFile(url, dest, (b, t) -> {});
            if (dest.length() == 0 || dest.length() > 4_000_000L) {
                dest.delete();
                return null;
            }
            return "css/" + filename;
        } catch (Exception e) {
            Log.w(TAG, "Skipping CSS " + url + ": " + e.getMessage());
            return null;
        }
    }

    // ── HTML rewriting ─────────────────────────────────────────────────────

    private static final String DARK_CSS =
            "<style id='nixdoc-dark'>\n" +
            "html,body,*{background:#111!important;color:#ddd!important;" +
            "border-color:#333!important;scrollbar-color:#444 #111}\n" +
            "a{color:#7cb3ff!important}a:visited{color:#b088ff!important}\n" +
            "code,pre,tt,.verbatim,.programlisting,.screen{background:#1e1e1e!important;" +
            "color:#c5f0a4!important;border:1px solid #333!important}\n" +
            "table{border-collapse:collapse}th{background:#222!important}\n" +
            "img{opacity:.85;filter:brightness(.9)}\n" +
            "input,select,textarea{background:#1e1e1e!important;color:#ddd!important;" +
            "border:1px solid #444!important}\n" +
            ".note,.warning,.tip,.caution,.important{background:#1a1a0a!important;" +
            "border-left:4px solid #666!important}\n" +
            "nav,header,footer,.sidebar,.toc{background:#161616!important}\n" +
            ".nixdoc-offline-banner{position:fixed;top:0;left:0;right:0;padding:4px;" +
            "background:#1e2a1e;color:#4caf82;text-align:center;font-size:11px;" +
            "font-family:monospace;z-index:9999;border-bottom:1px solid #2a3a2a}\n" +
            "</style>\n" +
            "<div class='nixdoc-offline-banner'>NixDoc Browser — offline copy</div>\n";

    private void rewriteHtml(File src, File dst, String baseUrl,
                              Map<String, String> urlToLocal) throws IOException {
        boolean headClosed  = false;
        boolean bannerAdded = false;
        try (BufferedReader  br = new BufferedReader(
                     new InputStreamReader(new FileInputStream(src), "UTF-8"), 65536);
             PrintWriter pw = new PrintWriter(
                     new BufferedWriter(
                             new OutputStreamWriter(new FileOutputStream(dst), "UTF-8"), 65536))) {

            String line;
            while ((line = br.readLine()) != null) {
                // Replace CSS URLs with local paths
                for (Map.Entry<String, String> e : urlToLocal.entrySet()) {
                    line = line.replace(e.getKey(), e.getValue());
                }
                // Make remaining absolute hrefs stay absolute (don't break them)
                // Inject dark CSS just before </head>
                if (!headClosed && line.toLowerCase().contains("</head>")) {
                    pw.println(DARK_CSS);
                    headClosed  = true;
                    bannerAdded = true;
                }
                pw.println(line);
            }
            // Fallback: inject at end of file if </head> wasn't found
            if (!bannerAdded) {
                pw.println(DARK_CSS);
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static String resolveUrl(String baseUrl, String ref) {
        if (ref.isEmpty() || ref.startsWith("data:") || ref.startsWith("javascript:"))
            return null;
        try {
            if (ref.startsWith("http://") || ref.startsWith("https://")) return ref;
            if (ref.startsWith("//")) return "https:" + ref;
            URL base = new URL(baseUrl);
            return new URL(base, ref).toString();
        } catch (Exception e) { return null; }
    }

    private static void deleteRecursive(File f) {
        if (f == null) return;
        if (f.isDirectory()) {
            File[] ch = f.listFiles();
            if (ch != null) for (File c : ch) deleteRecursive(c);
        }
        f.delete();
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    // ── Notifications & broadcasts ─────────────────────────────────────────

    private void progress(String title, long bytes, long total, String phase) {
        DownloadState.update(title, bytes, total, phase);
        broadcast(ACTION_PROGRESS, title, bytes, total, phase);
        int pct = total > 0 ? (int) Math.min(99, (bytes * 100L) / total) : -1;
        String mbLabel = total > 0
                ? String.format("%.1f / %.1f MB", bytes/1e6, total/1e6)
                : String.format("%.1f MB", bytes/1e6);
        showNotif(title, phase + (total > 0 ? "  " + mbLabel : ""),
                  pct >= 0 ? pct : 0, pct >= 0 ? 100 : -1);
    }

    private void broadcast(String action, String title, long bytes, long total, String phase) {
        Intent i = new Intent(action);
        i.putExtra(EXTRA_DOC_TITLE, title);
        i.putExtra(EXTRA_BYTES, bytes);
        i.putExtra(EXTRA_TOTAL, total);
        i.putExtra(EXTRA_PHASE, phase);
        sendBroadcast(i);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void showNotif(String title, String text, int progress, int max) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading " + title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
        if (max > 0)   b.setProgress(max, progress, false);
        else           b.setProgress(0, 0, true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, b.build());
    }

    private void showCompleteNotif(String title, boolean success) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(success ? android.R.drawable.stat_sys_download_done
                                      : android.R.drawable.stat_notify_error)
                .setContentTitle(success ? "Downloaded: " + title : "Failed: " + title)
                .setContentText(success ? "Available for offline reading" : "Check connection and retry")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, b.build());
    }
}
