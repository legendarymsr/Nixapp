package com.nixapp.docbrowser;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.util.List;

public class DocAdapter extends RecyclerView.Adapter<DocAdapter.ViewHolder> {

    private final List<DocEntry> docs;
    private final Context context;

    public DocAdapter(Context context, List<DocEntry> docs) {
        this.context = context;
        this.docs = docs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocEntry doc = docs.get(position);

        holder.title.setText(doc.title);
        holder.subtitle.setText(doc.subtitle);
        holder.docIcon.setText(doc.iconLetter);
        holder.docTag.setText(doc.tag.toUpperCase());

        int accent = ContextCompat.getColor(context, doc.accentColorRes);
        int accentDim = ContextCompat.getColor(context, doc.bannerColorRes);
        int cardBg = ContextCompat.getColor(context, R.color.surface_card);

        // Gradient banner: accent dim top → card background bottom
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ accentDim, cardBg });
        holder.bannerGradient.setBackground(gradient);

        // Thin accent separator line
        holder.accentLine.setBackgroundColor(accentDim);

        // Icon box: semi-transparent accent tint with rounded corners
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(dpToPx(10));
        iconBg.setColor(Color.argb(200,
                Color.red(accentDim), Color.green(accentDim), Color.blue(accentDim)));
        holder.iconCircle.setBackground(iconBg);
        holder.docIcon.setTextColor(accent);

        // Online button: accent color fill
        holder.btnReadOnline.setBackgroundTintList(ColorStateList.valueOf(accent));
        holder.btnReadOnline.setIconTint(ColorStateList.valueOf(Color.WHITE));

        // Offline/download button state
        boolean downloaded = OfflineStorage.isDownloaded(context, doc.title);
        holder.offlineStatusRow.setVisibility(downloaded ? View.VISIBLE : View.GONE);

        if (downloaded) {
            holder.btnDownload.setText(context.getString(R.string.read_offline));
            holder.btnDownload.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_offline_read));
        } else {
            holder.btnDownload.setText(context.getString(R.string.download_offline));
            holder.btnDownload.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_download));
        }
        holder.btnDownload.setIconTint(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.text_secondary)));
        holder.btnDownload.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        holder.btnDownload.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.divider)));

        holder.btnReadOnline.setOnClickListener(v -> {
            Intent intent = new Intent(context, DocBrowserActivity.class);
            intent.putExtra(DocBrowserActivity.EXTRA_URL, doc.onlineUrl);
            intent.putExtra(DocBrowserActivity.EXTRA_TITLE, doc.title);
            intent.putExtra(DocBrowserActivity.EXTRA_OFFLINE, false);
            context.startActivity(intent);
        });

        holder.btnDownload.setOnClickListener(v -> {
            if (OfflineStorage.isDownloaded(context, doc.title)) {
                File offlineDir = OfflineStorage.getDocDir(context, doc.title);
                Intent intent = new Intent(context, DocBrowserActivity.class);
                intent.putExtra(DocBrowserActivity.EXTRA_URL,
                        "file://" + new File(offlineDir, doc.offlineIndex).getAbsolutePath());
                intent.putExtra(DocBrowserActivity.EXTRA_TITLE, doc.title + " (Offline)");
                intent.putExtra(DocBrowserActivity.EXTRA_OFFLINE, true);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Downloading " + doc.title + "…", Toast.LENGTH_SHORT).show();
                Intent serviceIntent = new Intent(context, DownloadService.class);
                serviceIntent.putExtra(DownloadService.EXTRA_DOC_TITLE, doc.title);
                serviceIntent.putExtra(DownloadService.EXTRA_DOC_URL, doc.downloadUrl);
                context.startService(serviceIntent);
            }
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout iconCircle;
        View bannerGradient, accentLine, offlineStatusRow;
        TextView docIcon, docTag, title, subtitle;
        MaterialButton btnReadOnline, btnDownload;

        ViewHolder(View itemView) {
            super(itemView);
            bannerGradient = itemView.findViewById(R.id.banner_gradient);
            accentLine = itemView.findViewById(R.id.accent_line);
            iconCircle = itemView.findViewById(R.id.icon_circle);
            docIcon = itemView.findViewById(R.id.doc_icon);
            docTag = itemView.findViewById(R.id.doc_tag);
            title = itemView.findViewById(R.id.doc_title);
            subtitle = itemView.findViewById(R.id.doc_subtitle);
            btnReadOnline = itemView.findViewById(R.id.btn_read_online);
            btnDownload = itemView.findViewById(R.id.btn_download);
            offlineStatusRow = itemView.findViewById(R.id.offline_status_row);
        }
    }
}
