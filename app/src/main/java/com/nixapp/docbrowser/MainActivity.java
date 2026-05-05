package com.nixapp.docbrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DocAdapter adapter;
    private List<DocEntry> docs;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        docs = buildDocList();
        adapter = new DocAdapter(this, docs);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(downloadReceiver);
    }

    private List<DocEntry> buildDocList() {
        List<DocEntry> list = new ArrayList<>();

        list.add(new DocEntry(
                "NixOS Manual",
                "The official NixOS operating system documentation — configuration, installation, and modules.",
                "https://nixos.org/manual/nixos/stable/",
                "index.html",
                "https://nixos.org/manual/nixos/stable/",
                "NX", "System",
                R.color.nixos_blue_dim, R.color.nixos_blue
        ));

        list.add(new DocEntry(
                "Nixpkgs Manual",
                "The Nix packages collection reference — functions, overlays, and packaging guidelines.",
                "https://nixos.org/manual/nixpkgs/stable/",
                "index.html",
                "https://nixos.org/manual/nixpkgs/stable/",
                "Np", "Packages",
                R.color.nixpkgs_cyan_dim, R.color.nixpkgs_cyan
        ));

        list.add(new DocEntry(
                "GNU Guix Manual",
                "The official GNU Guix system documentation — package management and system configuration.",
                "https://guix.gnu.org/manual/en/html_node/",
                "index.html",
                "https://guix.gnu.org/manual/en/html_node/",
                "Gx", "System",
                R.color.guix_orange_dim, R.color.guix_orange
        ));

        list.add(new DocEntry(
                "Guix Cookbook",
                "Tutorials, how-tos, and worked examples for GNU Guix users and contributors.",
                "https://guix.gnu.org/cookbook/en/html_node/",
                "index.html",
                "https://guix.gnu.org/cookbook/en/html_node/",
                "GC", "Cookbook",
                R.color.cookbook_green_dim, R.color.cookbook_green
        ));

        list.add(new DocEntry(
                "Gentoo Handbook",
                "The official Gentoo Linux installation and configuration handbook.",
                "https://wiki.gentoo.org/wiki/Handbook:AMD64",
                "index.html",
                "https://wiki.gentoo.org/wiki/Handbook:AMD64",
                "Ge", "Handbook",
                R.color.gentoo_purple_dim, R.color.gentoo_purple
        ));

        list.add(new DocEntry(
                "Arch Linux Wiki",
                "The comprehensive Arch Linux wiki — installation, configuration, and troubleshooting.",
                "https://wiki.archlinux.org/",
                "index.html",
                "https://wiki.archlinux.org/",
                "Ar", "Wiki",
                R.color.arch_blue_dim, R.color.arch_blue
        ));

        list.add(new DocEntry(
                "Linux From Scratch",
                "Step-by-step instructions for building your own custom Linux system from source.",
                "https://www.linuxfromscratch.org/lfs/view/stable/",
                "index.html",
                "https://www.linuxfromscratch.org/lfs/view/stable/",
                "LFS", "Book",
                R.color.lfs_red_dim, R.color.lfs_red
        ));

        return list;
    }
}
