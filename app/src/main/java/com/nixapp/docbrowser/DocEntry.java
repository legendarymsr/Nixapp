package com.nixapp.docbrowser;

public class DocEntry {
    public final String title;
    public final String subtitle;
    public final String onlineUrl;
    public final String offlineIndex;
    public final String downloadUrl;
    public final String iconLetter;
    public final String tag;
    public final int bannerColorRes;
    public final int accentColorRes;

    public DocEntry(String title, String subtitle, String onlineUrl,
                    String offlineIndex, String downloadUrl,
                    String iconLetter, String tag,
                    int bannerColorRes, int accentColorRes) {
        this.title = title;
        this.subtitle = subtitle;
        this.onlineUrl = onlineUrl;
        this.offlineIndex = offlineIndex;
        this.downloadUrl = downloadUrl;
        this.iconLetter = iconLetter;
        this.tag = tag;
        this.bannerColorRes = bannerColorRes;
        this.accentColorRes = accentColorRes;
    }
}
