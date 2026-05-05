package com.nixapp.docbrowser;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private TextView bootText;
    private ScrollView scrollView;
    private TextView cursor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean skipped = false;
    private int lineIndex = 0;

    private static final String[] BOOT_LINES = {
        "",
        "NixDoc UEFI Firmware v2.6.0  Copyright (C) 2024",
        "CPU: ARM Cortex-A (8 cores)   RAM: Check OK",
        "Storage: Internal flash detected",
        "",
        "Booting NixDoc OS...",
        "",
        "[    0.000000] NixDoc kernel 1.0.0-stable (Android) #1 SMP",
        "[    0.000000] Command line: root=/dev/docs ro quiet splash",
        "[    0.042731] ACPI: documentation subsystem initialized",
        "[    0.087452] clocksource: tsc-early: mask: 0xffffffffffffffff",
        "[    0.134108] Calibrating delay loop... 3200.00 BogoMIPS",
        "[    0.179834] Memory: 14745624K available",
        "[    0.224517] PCI: doc-bus initialized",
        "[    0.269203] io scheduler bfq registered (default)",
        "[    0.313889] input: NixDoc touchscreen as /dev/input/event0",
        "",
        "[    0.401245] nixdoc: loading documentation modules...",
        "[    0.448001] nixos:          module loaded            [ OK ]",
        "[    0.492734] nixpkgs:        module loaded            [ OK ]",
        "[    0.537412] guix:           module loaded            [ OK ]",
        "[    0.582099] guix-cookbook:  module loaded            [ OK ]",
        "[    0.626781] gentoo:         module loaded            [ OK ]",
        "[    0.671468] archlinux:      module loaded            [ OK ]",
        "[    0.716150] lfs:            module loaded            [ OK ]",
        "",
        "[    0.760837] Starting documentation daemon...         [ OK ]",
        "[    0.805519] Starting offline storage service...      [ OK ]",
        "[    0.850206] Starting WebView renderer...             [ OK ]",
        "[    0.894888] Starting dark mode engine...             [ OK ]",
        "",
        "[    0.939575] nixdoc: 7 documentation sources registered",
        "[    0.984257] nixdoc: offline storage at /data/docs",
        "[    1.028944] nixdoc: all systems nominal",
        "",
        "Welcome to NixDoc Browser 1.0",
        "Kernel 1.0.0-stable on Android ARM",
        "",
    };

    // Delay between lines in ms — shorter for kernel lines, longer for key events
    private static final int[] LINE_DELAYS = new int[BOOT_LINES.length];

    static {
        for (int i = 0; i < BOOT_LINES.length; i++) {
            String line = BOOT_LINES[i];
            if (line.isEmpty()) {
                LINE_DELAYS[i] = 60;
            } else if (line.startsWith("[") && line.contains("module loaded")) {
                LINE_DELAYS[i] = 120;
            } else if (line.startsWith("[")) {
                LINE_DELAYS[i] = 55;
            } else if (line.startsWith("NixDoc UEFI") || line.startsWith("Welcome")) {
                LINE_DELAYS[i] = 200;
            } else {
                LINE_DELAYS[i] = 80;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive full-screen black
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_splash);

        bootText = findViewById(R.id.boot_text);
        scrollView = findViewById(R.id.scroll_view);
        cursor = findViewById(R.id.cursor);
        TextView skipHint = findViewById(R.id.skip_hint);

        // Blink the cursor
        ObjectAnimator blink = ObjectAnimator.ofFloat(cursor, "alpha", 1f, 0f);
        blink.setDuration(500);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.start();

        // Fade in skip hint after 500ms
        handler.postDelayed(() ->
                skipHint.animate().alpha(1f).setDuration(400).start(), 500);

        // Tap anywhere to skip
        findViewById(R.id.scroll_view).setOnClickListener(v -> skip());
        skipHint.setOnClickListener(v -> skip());
        bootText.setOnClickListener(v -> skip());
        findViewById(android.R.id.content).setOnClickListener(v -> skip());

        scheduleNextLine(300);
    }

    private void scheduleNextLine(long delay) {
        handler.postDelayed(this::printNextLine, delay);
    }

    private void printNextLine() {
        if (skipped || lineIndex >= BOOT_LINES.length) {
            if (!skipped) launch();
            return;
        }

        String line = BOOT_LINES[lineIndex];

        // Colour certain lines differently
        String colored = colorize(line);
        bootText.append(colored.isEmpty() ? "\n" : colored + "\n");

        // Auto-scroll to bottom
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

        int delay = LINE_DELAYS[lineIndex];
        lineIndex++;
        scheduleNextLine(delay);
    }

    private String colorize(String line) {
        // Keep raw string — TextView doesn't handle ANSI; we use HTML via Spanned
        return line;
    }

    private void skip() {
        if (skipped) return;
        skipped = true;
        handler.removeCallbacksAndMessages(null);
        launch();
    }

    private void launch() {
        // Brief green flash then transition
        bootText.append("\nLaunching NixDoc Browser...\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 350);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
