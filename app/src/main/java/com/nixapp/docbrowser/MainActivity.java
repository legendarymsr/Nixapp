package com.nixapp.docbrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
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
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView appTitle = findViewById(R.id.app_title);
        appTitle.setText(R.string.app_name);

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
                "The official NixOS operating system documentation",
                "https://nixos.org/manual/nixos/stable/",
                "index.html",
                "https://nixos.org/manual/nixos/stable/"
        ));

        list.add(new DocEntry(
                "Nixpkgs Manual",
                "The Nix packages collection reference manual",
                "https://nixos.org/manual/nixpkgs/stable/",
                "index.html",
                "https://nixos.org/manual/nixpkgs/stable/"
        ));

        list.add(new DocEntry(
                "GNU Guix Manual",
                "The official GNU Guix system documentation",
                "https://guix.gnu.org/manual/en/html_node/",
                "index.html",
                "https://guix.gnu.org/manual/en/html_node/"
        ));

        list.add(new DocEntry(
                "Guix Cookbook",
                "Tutorials and examples for GNU Guix",
                "https://guix.gnu.org/cookbook/en/html_node/",
                "index.html",
                "https://guix.gnu.org/cookbook/en/html_node/"
        ));

        return list;
    }
}
