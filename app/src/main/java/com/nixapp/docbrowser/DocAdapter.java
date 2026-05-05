package com.nixapp.docbrowser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
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
        View v = LayoutInflater.from(context).inflate(R.layout.item_doc, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        DocEntry doc = docs.get(position);

        h.title.setText(doc.title);
        h.subtitle.setText(doc.subtitle);
        h.docIcon.setText(doc.iconLetter);
        h.docTag.setText(doc.tag.toUpperCase());

        int accent    = ContextCompat.getColor(context, doc.accentColorRes);
        int accentDim = ContextCompat.getColor(context, doc.bannerColorRes);
        int cardBg    = ContextCompat.getColor(context, R.color.surface_card);

        // Gradient banner
        GradientDrawable grad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ accentDim, cardBg });
        h.bannerGradient.setBackground(grad);
        h.accentLine.setBackgroundColor(accentDim);

        // Icon box
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(dpToPx(10));
        iconBg.setColor(Color.argb(200,
                Color.red(accentDim), Color.green(accentDim), Color.blue(accentDim)));
        h.iconCircle.setBackground(iconBg);
        h.docIcon.setTextColor(accent);

        // Online button accent
        h.btnReadOnline.setBackgroundTintList(ColorStateList.valueOf(accent));
        h.btnReadOnline.setIconTint(ColorStateList.valueOf(Color.WHITE));

        // ── State logic ────────────────────────────────────────────────────
        boolean downloading = DownloadState.isActive(doc.title);
        boolean downloaded  = !downloading && OfflineStorage.isDownloaded(context, doc.title);
        DownloadState.Progress prog = DownloadState.get(doc.title);

        // Progress container
        if (downloading && prog != null) {
            h.progressContainer.setVisibility(View.VISIBLE);
            h.offlineStatusRow.setVisibility(View.GONE);
            h.progressPhase.setText(prog.phase);
            if (prog.percent >= 0) {
                h.progressBar.setIndeterminate(false);
                h.progressBar.setProgress(prog.percent);
                h.progressLabel.setText(prog.percent + "%  " + prog.bytesLabel());
            } else {
                h.progressBar.setIndeterminate(true);
                h.progressLabel.setText(prog.bytesLabel());
            }
        } else {
            h.progressContainer.setVisibility(View.GONE);
        }

        // Offline badge
        if (downloaded) {
            h.offlineStatusRow.setVisibility(View.VISIBLE);
            String size = OfflineStorage.sizeMb(context, doc.title);
            h.offlineSizeText.setText("Available offline" + (size.isEmpty() ? "" : " · " + size));
        } else if (!downloading) {
            h.offlineStatusRow.setVisibility(View.GONE);
        }

        // Download button label
        if (downloading) {
            h.btnDownload.setText("Downloading…");
            h.btnDownload.setIcon(null);
            h.btnDownload.setEnabled(false);
        } else if (downloaded) {
            h.btnDownload.setText(context.getString(R.string.read_offline));
            h.btnDownload.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_offline_read));
            h.btnDownload.setEnabled(true);
        } else {
            h.btnDownload.setText(context.getString(R.string.download_offline));
            h.btnDownload.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_download));
            h.btnDownload.setEnabled(true);
        }
        h.btnDownload.setIconTint(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.text_secondary)));
        h.btnDownload.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        h.btnDownload.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.divider)));

        // ── Click handlers ─────────────────────────────────────────────────

        h.btnReadOnline.setOnClickListener(v -> {
            Intent i = new Intent(context, DocBrowserActivity.class);
            i.putExtra(DocBrowserActivity.EXTRA_URL, doc.onlineUrl);
            i.putExtra(DocBrowserActivity.EXTRA_TITLE, doc.title);
            i.putExtra(DocBrowserActivity.EXTRA_OFFLINE, false);
            context.startActivity(i);
        });

        h.btnDownload.setOnClickListener(v -> {
            if (OfflineStorage.isDownloaded(context, doc.title)) {
                // Open offline
                File index = new File(OfflineStorage.getDocDir(context, doc.title), doc.offlineIndex);
                String fileUri = Uri.fromFile(index).toString();
                Intent i = new Intent(context, DocBrowserActivity.class);
                i.putExtra(DocBrowserActivity.EXTRA_URL, fileUri);
                i.putExtra(DocBrowserActivity.EXTRA_TITLE, doc.title + " (Offline)");
                i.putExtra(DocBrowserActivity.EXTRA_OFFLINE, true);
                context.startActivity(i);
            } else if (!DownloadState.isActive(doc.title)) {
                // Start download
                Toast.makeText(context, "Downloading " + doc.title + "…",
                        Toast.LENGTH_SHORT).show();
                Intent svc = new Intent(context, DownloadService.class);
                svc.putExtra(DownloadService.EXTRA_DOC_TITLE, doc.title);
                svc.putExtra(DownloadService.EXTRA_DOC_URL, doc.downloadUrl);
                context.startService(svc);
            }
        });

        // Long-press to delete offline copy
        h.itemView.setOnLongClickListener(v -> {
            if (OfflineStorage.isDownloaded(context, doc.title)) {
                String size = OfflineStorage.sizeMb(context, doc.title);
                String msg  = "Delete the offline copy of "" + doc.title + ""?"
                        + (size.isEmpty() ? "" : "\n\nThis will free " + size + ".");
                new AlertDialog.Builder(context)
                        .setTitle("Delete offline copy")
                        .setMessage(msg)
                        .setPositiveButton("Delete", (d, w) -> {
                            OfflineStorage.deleteDoc(context, doc.title);
                            int pos = h.getAdapterPosition();
                            if (pos != RecyclerView.NO_ID) notifyItemChanged(pos);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
            return false;
        });
    }

    /** Called from MainActivity's progress broadcast receiver. */
    public void onProgress(String docTitle) {
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).title.equals(docTitle)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() { return docs.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout iconCircle;
        View bannerGradient, accentLine, offlineStatusRow, progressContainer;
        TextView docIcon, docTag, title, subtitle, offlineSizeText;
        TextView progressPhase, progressLabel;
        ProgressBar progressBar;
        MaterialButton btnReadOnline, btnDownload;

        ViewHolder(View v) {
            super(v);
            bannerGradient   = v.findViewById(R.id.banner_gradient);
            accentLine       = v.findViewById(R.id.accent_line);
            iconCircle       = v.findViewById(R.id.icon_circle);
            docIcon          = v.findViewById(R.id.doc_icon);
            docTag           = v.findViewById(R.id.doc_tag);
            title            = v.findViewById(R.id.doc_title);
            subtitle         = v.findViewById(R.id.doc_subtitle);
            offlineStatusRow = v.findViewById(R.id.offline_status_row);
            offlineSizeText  = v.findViewById(R.id.offline_size_text);
            progressContainer = v.findViewById(R.id.progress_container);
            progressPhase    = v.findViewById(R.id.progress_phase);
            progressLabel    = v.findViewById(R.id.progress_label);
            progressBar      = v.findViewById(R.id.progress_bar);
            btnReadOnline    = v.findViewById(R.id.btn_read_online);
            btnDownload      = v.findViewById(R.id.btn_download);
        }
    }
}
