package com.nixapp.docbrowser;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class DownloadService extends IntentService {

    public static final String EXTRA_DOC_TITLE = "doc_title";
    public static final String EXTRA_DOC_URL = "doc_url";
    public static final String ACTION_DOWNLOAD_COMPLETE = "com.nixapp.docbrowser.DOWNLOAD_COMPLETE";
    public static final String EXTRA_SUCCESS = "success";

    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIF_ID = 1001;
    private static final String TAG = "DownloadService";

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        String docTitle = intent.getStringExtra(EXTRA_DOC_TITLE);
        String docUrl = intent.getStringExtra(EXTRA_DOC_URL);

        createNotificationChannel();
        showProgressNotification(docTitle);

        boolean success = false;
        try {
            success = downloadDoc(docTitle, docUrl);
        } catch (Exception e) {
            Log.e(TAG, "Download failed", e);
        }

        showCompleteNotification(docTitle, success);

        Intent broadcast = new Intent(ACTION_DOWNLOAD_COMPLETE);
        broadcast.putExtra(EXTRA_DOC_TITLE, docTitle);
        broadcast.putExtra(EXTRA_SUCCESS, success);
        sendBroadcast(broadcast);
    }

    private boolean downloadDoc(String docTitle, String docUrl) throws IOException {
        File destDir = OfflineStorage.getDocDir(this, docTitle);
        if (!destDir.exists() && !destDir.mkdirs()) {
            return false;
        }

        // Download the main HTML page
        String html = fetchHtml(docUrl);
        if (html == null) return false;

        // Save index.html
        File indexFile = new File(destDir, "index.html");
        try (FileOutputStream fos = new FileOutputStream(indexFile)) {
            fos.write(html.getBytes("UTF-8"));
        }

        // Inject offline indicator and dark mode into the HTML
        html = injectOfflineStyles(html);
        try (FileOutputStream fos = new FileOutputStream(indexFile)) {
            fos.write(html.getBytes("UTF-8"));
        }

        return true;
    }

    private String fetchHtml(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Android; NixDocBrowser/1.0)");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) return null;

            try (InputStream in = conn.getInputStream()) {
                byte[] buffer = new byte[8192];
                StringBuilder sb = new StringBuilder();
                int read;
                while ((read = in.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, read, "UTF-8"));
                }
                return sb.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchHtml failed: " + urlStr, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String injectOfflineStyles(String html) {
        String darkCss = "<style>" +
                "body{background:#1a1a1a!important;color:#e0e0e0!important}" +
                "a{color:#7cb3ff!important}" +
                "pre,code{background:#2a2a2a!important;color:#c5f0a4!important}" +
                ".offline-banner{position:fixed;top:0;left:0;right:0;padding:6px;" +
                "background:#333;color:#aaa;text-align:center;font-size:12px;z-index:9999}" +
                "</style>" +
                "<div class='offline-banner'>Offline copy - NixDocBrowser</div>";
        return html.replace("</head>", darkCss + "</head>");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void showProgressNotification(String docTitle) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading " + docTitle)
                .setContentText("Please wait...")
                .setProgress(0, 0, true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, builder.build());
    }

    private void showCompleteNotification(String docTitle, boolean success) {
        String msg = success ? docTitle + " downloaded for offline use"
                : "Failed to download " + docTitle;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(success ? android.R.drawable.stat_sys_download_done
                        : android.R.drawable.stat_notify_error)
                .setContentTitle(success ? "Download complete" : "Download failed")
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, builder.build());
    }
}
