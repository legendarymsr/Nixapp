package com.nixapp.docbrowser;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private TextView bootText;
    private TextView skipHint;
    private ScrollView scrollView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean bootComplete = false;
    private int lineIndex = 0;
    private long bootStartTime;

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

    private static final int[] LINE_DELAYS = new int[BOOT_LINES.length];
    static {
        for (int i = 0; i < BOOT_LINES.length; i++) {
            String l = BOOT_LINES[i];
            if (l.isEmpty())                              LINE_DELAYS[i] = 55;
            else if (l.contains("module loaded"))         LINE_DELAYS[i] = 115;
            else if (l.startsWith("["))                   LINE_DELAYS[i] = 50;
            else if (l.startsWith("NixDoc UEFI") ||
                     l.startsWith("Welcome"))             LINE_DELAYS[i] = 190;
            else                                          LINE_DELAYS[i] = 75;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_splash);

        bootText  = findViewById(R.id.boot_text);
        scrollView = findViewById(R.id.scroll_view);
        skipHint  = findViewById(R.id.skip_hint);
        TextView cursor = findViewById(R.id.cursor);

        bootStartTime = System.currentTimeMillis();

        // Blinking cursor
        ObjectAnimator blink = ObjectAnimator.ofFloat(cursor, "alpha", 1f, 0f);
        blink.setDuration(500);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.start();

        // Fade in skip hint after 600ms
        handler.postDelayed(() ->
                skipHint.animate().alpha(1f).setDuration(400).start(), 600);

        // Single tap handler: skip boot OR continue after fastfetch
        View.OnClickListener tapListener = v -> onTap();
        scrollView.setOnClickListener(tapListener);
        bootText.setOnClickListener(tapListener);
        skipHint.setOnClickListener(tapListener);
        findViewById(android.R.id.content).setOnClickListener(tapListener);

        scheduleNextLine(300);
    }

    private void onTap() {
        if (bootComplete) {
            launch();
        } else {
            // Skip animation: flush remaining lines instantly then show fastfetch
            handler.removeCallbacksAndMessages(null);
            StringBuilder sb = new StringBuilder();
            while (lineIndex < BOOT_LINES.length) {
                sb.append(BOOT_LINES[lineIndex]).append("\n");
                lineIndex++;
            }
            bootText.append(sb.toString());
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            showFastfetch();
        }
    }

    private void scheduleNextLine(long delay) {
        handler.postDelayed(this::printNextLine, delay);
    }

    private void printNextLine() {
        if (lineIndex >= BOOT_LINES.length) {
            showFastfetch();
            return;
        }
        bootText.append(BOOT_LINES[lineIndex] + "\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        int delay = LINE_DELAYS[lineIndex];
        lineIndex++;
        scheduleNextLine(delay);
    }

    // ── Fastfetch ──────────────────────────────────────────────────────────

    private void showFastfetch() {
        bootText.append("\n" + buildFastfetch());
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

        bootComplete = true;
        skipHint.setText("── tap anywhere to continue ──");
        skipHint.setTextColor(0xFF44FF44);
        skipHint.animate().alpha(1f).setDuration(300).start();
    }

    @SuppressWarnings("deprecation")
    private String buildFastfetch() {
        // ── Real hardware info ────────────────────────────────────────────
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mem);
        long totalMib = mem.totalMem / (1024 * 1024);
        long usedMib  = (mem.totalMem - mem.availMem) / (1024 * 1024);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        float density = dm.density;
        String dpi = dm.densityDpi + " dpi";

        StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
        long storTotal = sf.getTotalBytes() / (1024 * 1024 * 1024);
        long storFree  = sf.getFreeBytes()  / (1024 * 1024 * 1024);

        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int battery = bm != null
                ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
        String battStr = battery >= 0 ? battery + "%" : "unknown";
        String battBar = batteryBar(battery);

        int    cores     = Runtime.getRuntime().availableProcessors();
        long   uptimeSec = (System.currentTimeMillis() - bootStartTime) / 1000;
        String model     = Build.MANUFACTURER + " " + Build.MODEL;
        String board     = Build.HARDWARE;
        String abi       = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "arm64";
        String androidVer = "Android " + Build.VERSION.RELEASE
                            + "  (API " + Build.VERSION.SDK_INT + ")";

        // ── Fun computed fields ───────────────────────────────────────────
        String nerdScore = nerdScore();
        String upStr     = formatUptime(uptimeSec);

        // ── ASCII logo (20 chars wide, 8 lines) ───────────────────────────
        String[] logo = {
            "       .  *  .  *   ",
            "      * \\  |  / *   ",
            "     *  --[Nd]--  * ",
            "      * /  |  \\ *   ",
            "       .  *  .  *   ",
            "        NixDoc OS   ",
            "                    ",
            "                    ",
        };

        // ── Info rows ─────────────────────────────────────────────────────
        String user = "user@nixdoc";
        String sep  = repeat("─", 28);
        String[] info = {
            user,
            sep,
            "OS       NixDoc Browser 1.0",
            "Kernel   1.0.0-stable-arm64",
            "Host     " + model,
            "Board    " + board + "  (" + abi + ")",
            "Uptime   " + upStr,
            "Android  " + androidVer,
            "Shell    nixdoc-sh 1.0",
            "DE       nixdoc-wm (dark terminal)",
            "Font     Monospace (crisp)",
            "Docs     7 loaded  (NixOS Nixpkgs Guix Arch Gentoo LFS)",
            "Nerd Lvl " + nerdScore,
            sep,
            "Res      " + dm.widthPixels + "x" + dm.heightPixels + "  @" + dpi,
            "CPU      " + cores + "-core ARM  (" + abi + ")",
            "Memory   " + usedMib + " MiB / " + totalMib + " MiB",
            "Storage  " + (storTotal - storFree) + " GiB / " + storTotal + " GiB",
            "Battery  " + battStr + "  " + battBar,
            sep,
            "  █ █ █ █ █ █ █ █  (terminal colors)",
        };

        int lines = Math.max(logo.length, info.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            String l = i < logo.length ? logo[i] : "                    ";
            String r = i < info.length ? info[i] : "";
            sb.append(l).append("  ").append(r).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String batteryBar(int pct) {
        if (pct < 0) return "[----------]";
        int filled = pct / 10;
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < 10; i++) b.append(i < filled ? "█" : "░");
        b.append("]");
        return b.toString();
    }

    private String formatUptime(long secs) {
        if (secs < 60) return secs + "s";
        long mins = secs / 60; long s = secs % 60;
        if (mins < 60) return mins + "m " + s + "s";
        return (mins / 60) + "h " + (mins % 60) + "m";
    }

    private String nerdScore() {
        // Fun score: NixOS + Guix = functional purist, + Gentoo/Arch/LFS = absolute ricer
        String[] badges = { "★★★★★  MAXIMUM RICE", "[ confirmed distro hopper ]",
                            "rice lord  (seek help)", "kernel.org/superfan" };
        int idx = (int)(System.currentTimeMillis() % badges.length);
        return badges[idx];
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    // ── Launch ─────────────────────────────────────────────────────────────

    private void launch() {
        bootText.append("Launching NixDoc Browser...\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        handler.postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 300);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
