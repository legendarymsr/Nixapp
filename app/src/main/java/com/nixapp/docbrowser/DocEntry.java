package com.nixapp.docbrowser;

public class DocEntry {
    public final String title;
    public final String subtitle;
    public final String onlineUrl;
    public final String offlineIndex;
    public final String downloadUrl;

    public DocEntry(String title, String subtitle, String onlineUrl,
                    String offlineIndex, String downloadUrl) {
        this.title = title;
        this.subtitle = subtitle;
        this.onlineUrl = onlineUrl;
        this.offlineIndex = offlineIndex;
        this.downloadUrl = downloadUrl;
    }
}
