package com.accesschecker;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_REQUEST_CODE = 1001;

    // Root card
    private View       rootPulse, rootDot;
    private TextView   rootStatusTv, rootDetailsTv, rootExpandTv;
    private LinearLayout rootDetailsLayout;

    // Bootloader card
    private View       bootPulse, bootDot;
    private TextView   bootStatusTv, bootDetailsTv, bootExpandTv;
    private LinearLayout bootDetailsLayout;

    // Shizuku card
    private View       shizukuPulse, shizukuDot;
    private TextView   shizukuStatusTv, shizukuDetailsTv, shizukuExpandTv;
    private LinearLayout shizukuDetailsLayout;

    // Score
    private TextView   scoreText;
    private ProgressBar scoreBar;

    // Results (written on background thread, read on main)
    private volatile RootChecker.Result     rootResult;
    private volatile BootloaderChecker.Result bootResult;
    private volatile ShizukuChecker.Result  shizukuResult;

    // Pulse animations
    private AnimatorSet rootAnim, bootAnim, shizukuAnim;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Shizuku lifecycle listeners ───────────────────────────────────────

    private final Shizuku.OnBinderReceivedListener onBinderReceived = () ->
            mainHandler.post(() -> runShizukuCheck(true));

    private final Shizuku.OnBinderDeadListener onBinderDead = () ->
            mainHandler.post(() -> runShizukuCheck(true));

    private final Shizuku.OnRequestPermissionResultListener onPermResult =
            (code, result) -> {
                if (code == SHIZUKU_REQUEST_CODE)
                    mainHandler.post(() -> runShizukuCheck(true));
            };

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupCardClicks();

        try {
            Shizuku.addBinderReceivedListenerSticky(onBinderReceived);
            Shizuku.addBinderDeadListener(onBinderDead);
            Shizuku.addRequestPermissionResultListener(onPermResult);
        } catch (Throwable ignored) {}

        shizukuDot.setOnClickListener(v -> requestShizukuPermission());

        findViewById(R.id.btn_recheck).setOnClickListener(v -> recheckAll());

        recheckAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeBinderReceivedListener(onBinderReceived);
            Shizuku.removeBinderDeadListener(onBinderDead);
            Shizuku.removeRequestPermissionResultListener(onPermResult);
        } catch (Throwable ignored) {}
        executor.shutdown();
        cancelAnim(rootAnim);
        cancelAnim(bootAnim);
        cancelAnim(shizukuAnim);
    }

    // ── Check orchestration ───────────────────────────────────────────────

    private void recheckAll() {
        rootResult    = null;
        bootResult    = null;
        shizukuResult = null;

        setLoading(rootPulse, rootDot, rootStatusTv);
        setLoading(bootPulse, bootDot, bootStatusTv);
        setLoading(shizukuPulse, shizukuDot, shizukuStatusTv);
        scoreText.setText("--/100");
        scoreText.setTextColor(getColor(R.color.green_dim));
        scoreBar.setProgress(0);

        executor.execute(() -> {
            RootChecker.Result root = RootChecker.check(this);
            mainHandler.post(() -> {
                applyRootResult(root);
                maybeUpdateScore();
            });

            BootloaderChecker.Result boot = BootloaderChecker.check();
            mainHandler.post(() -> {
                applyBootResult(boot);
                maybeUpdateScore();
            });
        });

        // Shizuku API must be called on main thread
        runShizukuCheck(false);
    }

    private void runShizukuCheck(boolean updateScore) {
        ShizukuChecker.Result result = ShizukuChecker.check(this);
        // Cross-reference run mode with root result
        if (result.running) {
            result.runMode = (rootResult != null
                    && rootResult.status == RootChecker.Status.GRANTED) ? "root" : "adb";
        }
        applyShizukuResult(result);
        if (updateScore) maybeUpdateScore();
    }

    // ── Display methods ───────────────────────────────────────────────────

    private void applyRootResult(RootChecker.Result r) {
        rootResult = r;
        int color;
        String label;
        switch (r.status) {
            case GRANTED:
                color = Color.parseColor("#FF3B3B");
                label = "ROOTED";
                break;
            case DENIED:
                color = Color.parseColor("#00E676");
                label = "NOT ROOTED";
                break;
            default:
                color = Color.parseColor("#FFD600");
                label = "UNKNOWN";
                break;
        }
        applyDotColor(rootPulse, rootDot, color);
        rootStatusTv.setText(label);
        rootStatusTv.setTextColor(color);
        startSonarPulse(rootPulse, color, 1);

        StringBuilder sb = new StringBuilder();
        for (String line : r.lines) sb.append("  ").append(line).append("\n");
        rootDetailsTv.setText(sb.toString().trim());
    }

    private void applyBootResult(BootloaderChecker.Result r) {
        bootResult = r;
        int color;
        String label;
        switch (r.status) {
            case LOCKED:
                color = Color.parseColor("#00E676");
                label = "LOCKED";
                break;
            case UNLOCKED:
                color = Color.parseColor("#FF3B3B");
                label = "UNLOCKED";
                break;
            default:
                color = Color.parseColor("#FFD600");
                label = "UNKNOWN";
                break;
        }
        // If boot state is unknown but verified boot tells us something, use it
        if (r.status == BootloaderChecker.Status.UNKNOWN) {
            if (r.verifiedBoot == BootloaderChecker.VerifiedBootState.GREEN) {
                color = Color.parseColor("#00E676");
                label = "LIKELY LOCKED";
            } else if (r.verifiedBoot == BootloaderChecker.VerifiedBootState.ORANGE) {
                color = Color.parseColor("#FF3B3B");
                label = "LIKELY UNLOCKED";
            }
        }
        applyDotColor(bootPulse, bootDot, color);
        bootStatusTv.setText(label);
        bootStatusTv.setTextColor(color);
        startSonarPulse(bootPulse, color, 2);

        StringBuilder sb = new StringBuilder();
        for (String line : r.lines) sb.append("  ").append(line).append("\n");
        bootDetailsTv.setText(sb.toString().trim());
    }

    private void applyShizukuResult(ShizukuChecker.Result r) {
        shizukuResult = r;
        int color;
        String label;
        switch (r.status) {
            case AVAILABLE_PERMITTED:
                color = Color.parseColor("#FF3B3B");
                label = "RUNNING / PERMITTED";
                break;
            case AVAILABLE_DENIED:
                color = Color.parseColor("#FFD600");
                label = "RUNNING / NO PERM";
                break;
            case INSTALLED_STOPPED:
                color = Color.parseColor("#FFD600");
                label = "INSTALLED / STOPPED";
                break;
            case NOT_INSTALLED:
                color = Color.parseColor("#00E676");
                label = "NOT INSTALLED";
                break;
            default:
                color = Color.parseColor("#FFD600");
                label = "UNKNOWN";
                break;
        }
        applyDotColor(shizukuPulse, shizukuDot, color);
        shizukuStatusTv.setText(label);
        shizukuStatusTv.setTextColor(color);
        startSonarPulse(shizukuPulse, color, 3);

        StringBuilder sb = new StringBuilder();
        for (String line : r.lines) sb.append("  ").append(line).append("\n");
        if (r.runMode != null)
            sb.append("  run mode  : ").append(r.runMode).append("\n");
        if (r.status == ShizukuChecker.Status.AVAILABLE_DENIED)
            sb.append("\n  [ TAP STATUS DOT TO REQUEST PERMISSION ]");
        shizukuDetailsTv.setText(sb.toString().trim());
    }

    private void maybeUpdateScore() {
        if (rootResult == null || bootResult == null || shizukuResult == null) return;

        int score = 100;

        // Root
        if (rootResult.status == RootChecker.Status.GRANTED)  score -= 30;
        else if (rootResult.status == RootChecker.Status.UNKNOWN) score -= 5;

        // Bootloader
        if (bootResult.status == BootloaderChecker.Status.UNLOCKED) score -= 35;
        else if (bootResult.status == BootloaderChecker.Status.UNKNOWN) score -= 5;

        // Verified boot
        switch (bootResult.verifiedBoot) {
            case YELLOW:  score -=  5; break;
            case ORANGE:  score -= 10; break;
            case RED:     score -= 15; break;
            case UNKNOWN: score -=  5; break;
            default: break;
        }

        // dm-verity
        if (!bootResult.dmVerityEnabled) score -= 5;

        // Shizuku
        if (shizukuResult.status == ShizukuChecker.Status.AVAILABLE_PERMITTED) score -= 10;

        score = Math.max(0, score);

        int scoreColor = score >= 80 ? Color.parseColor("#00E676")
                       : score >= 50 ? Color.parseColor("#FFD600")
                       :               Color.parseColor("#FF3B3B");

        scoreText.setText(score + "/100");
        scoreText.setTextColor(scoreColor);

        ObjectAnimator barAnim = ObjectAnimator.ofInt(scoreBar, "progress",
                scoreBar.getProgress(), score);
        barAnim.setDuration(900);
        barAnim.start();

        scoreBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(scoreColor));
    }

    // ── Animation ─────────────────────────────────────────────────────────

    private void applyDotColor(View pulse, View dot, int color) {
        int pulseAlpha = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
        setCircleColor(pulse, pulseAlpha);
        setCircleColor(dot, color);
    }

    private void setCircleColor(View v, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        v.setBackground(d);
    }

    private void setLoading(View pulse, View dot, TextView statusTv) {
        int gray = Color.parseColor("#444444");
        applyDotColor(pulse, dot, gray);
        statusTv.setText("CHECKING...");
        statusTv.setTextColor(Color.parseColor("#888888"));
        cancelAnim(currentAnim(pulse));
        startSonarPulse(pulse, gray, 0);
    }

    private AnimatorSet currentAnim(View pulse) {
        if (pulse == rootPulse) return rootAnim;
        if (pulse == bootPulse) return bootAnim;
        return shizukuAnim;
    }

    private void startSonarPulse(View pulse, int color, int slot) {
        AnimatorSet old = slot == 1 ? rootAnim : slot == 2 ? bootAnim : shizukuAnim;
        cancelAnim(old);

        pulse.setScaleX(1f);
        pulse.setScaleY(1f);
        pulse.setAlpha(0.75f);

        // Pulse color update
        int pulseAlpha = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
        setCircleColor(pulse, pulseAlpha);

        ObjectAnimator sx    = ObjectAnimator.ofFloat(pulse, "scaleX", 1f, 3f);
        ObjectAnimator sy    = ObjectAnimator.ofFloat(pulse, "scaleY", 1f, 3f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(pulse, "alpha",  0.75f, 0f);

        for (ObjectAnimator a : new ObjectAnimator[]{ sx, sy, alpha }) {
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.RESTART);
            a.setDuration(2200);
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, alpha);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();

        if (slot == 1) rootAnim    = set;
        else if (slot == 2) bootAnim = set;
        else shizukuAnim = set;
    }

    private static void cancelAnim(AnimatorSet a) {
        if (a != null && a.isRunning()) a.cancel();
    }

    // ── UI setup ──────────────────────────────────────────────────────────

    private void bindViews() {
        rootPulse          = findViewById(R.id.root_pulse);
        rootDot            = findViewById(R.id.root_dot);
        rootStatusTv       = findViewById(R.id.root_status);
        rootDetailsTv      = findViewById(R.id.root_details_text);
        rootExpandTv       = findViewById(R.id.root_expand);
        rootDetailsLayout  = findViewById(R.id.root_details);

        bootPulse          = findViewById(R.id.boot_pulse);
        bootDot            = findViewById(R.id.boot_dot);
        bootStatusTv       = findViewById(R.id.boot_status);
        bootDetailsTv      = findViewById(R.id.boot_details_text);
        bootExpandTv       = findViewById(R.id.boot_expand);
        bootDetailsLayout  = findViewById(R.id.boot_details);

        shizukuPulse       = findViewById(R.id.shizuku_pulse);
        shizukuDot         = findViewById(R.id.shizuku_dot);
        shizukuStatusTv    = findViewById(R.id.shizuku_status);
        shizukuDetailsTv   = findViewById(R.id.shizuku_details_text);
        shizukuExpandTv    = findViewById(R.id.shizuku_expand);
        shizukuDetailsLayout = findViewById(R.id.shizuku_details);

        scoreText = findViewById(R.id.score_text);
        scoreBar  = findViewById(R.id.score_bar);
    }

    private void setupCardClicks() {
        wireExpand(R.id.root_header,    rootDetailsLayout,    rootExpandTv);
        wireExpand(R.id.boot_header,    bootDetailsLayout,    bootExpandTv);
        wireExpand(R.id.shizuku_header, shizukuDetailsLayout, shizukuExpandTv);
    }

    private void wireExpand(int headerId, LinearLayout details, TextView arrow) {
        findViewById(headerId).setOnClickListener(v -> {
            boolean visible = details.getVisibility() == View.VISIBLE;
            details.setVisibility(visible ? View.GONE : View.VISIBLE);
            arrow.setText(visible ? "▼" : "▲");
        });
    }

    private void requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Shizuku service not running", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Shizuku.isPreV11()) {
                Toast.makeText(this, "Shizuku v11+ required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Shizuku error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
