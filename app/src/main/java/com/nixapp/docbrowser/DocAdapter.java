package com.nixapp.docbrowser;

import android.content.Context;
import android.content.Intent;
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
        holder.docTag.setText(doc.tag);

        // Apply per-doc banner color
        holder.cardBanner.setBackgroundColor(ContextCompat.getColor(context, doc.bannerColorRes));

        // Apply accent to online button
        holder.btnReadOnline.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, doc.accentColorRes)));

        // Offline status badge
        boolean downloaded = OfflineStorage.isDownloaded(context, doc.title);
        if (downloaded) {
            holder.offlineStatusRow.setVisibility(View.VISIBLE);
            holder.btnDownload.setText(context.getString(R.string.read_offline));
        } else {
            holder.offlineStatusRow.setVisibility(View.GONE);
            holder.btnDownload.setText(context.getString(R.string.download_offline));
        }

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

    @Override
    public int getItemCount() {
        return docs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout cardBanner;
        TextView docIcon, docTag, title, subtitle;
        MaterialButton btnReadOnline, btnDownload;
        View offlineStatusRow;

        ViewHolder(View itemView) {
            super(itemView);
            cardBanner = itemView.findViewById(R.id.card_banner);
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
