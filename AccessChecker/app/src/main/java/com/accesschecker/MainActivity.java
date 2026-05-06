package com.accesschecker;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_REQUEST_CODE = 1001;

    // Root card
    private View         rootPulse, rootDot;
    private TextView     rootStatusTv, rootDetailsTv, rootExpandTv, rootConfidenceTv;
    private LinearLayout rootDetailsLayout;

    // Bootloader card
    private View         bootPulse, bootDot;
    private TextView     bootStatusTv, bootDetailsTv, bootExpandTv, bootConfidenceTv;
    private LinearLayout bootDetailsLayout;

    // Zygisk card
    private View         zygiskPulse, zygiskDot;
    private TextView     zygiskStatusTv, zygiskDetailsTv, zygiskExpandTv, zygiskConfidenceTv;
    private LinearLayout zygiskDetailsLayout;

    // Shizuku card
    private View         shizukuPulse, shizukuDot;
    private TextView     shizukuStatusTv, shizukuDetailsTv, shizukuExpandTv;
    private LinearLayout shizukuDetailsLayout;

    // Score
    private TextView    scoreText;
    private ProgressBar scoreBar;

    // Results (written on background thread, read on main)
    private volatile RootChecker.Result       rootResult;
    private volatile BootloaderChecker.Result bootResult;
    private volatile ZygiskChecker.Result     zygiskResult;
    private volatile ShizukuChecker.Result    shizukuResult;

    // Pulse animations
    private AnimatorSet rootAnim, bootAnim, zygiskAnim, shizukuAnim;

    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    // ── Shizuku lifecycle listeners ───────────────────────────────────────

    private final Shizuku.OnBinderReceivedListener onBinderReceived =
            () -> mainHandler.post(() -> runShizukuCheck(true));

    private final Shizuku.OnBinderDeadListener onBinderDead =
            () -> mainHandler.post(() -> runShizukuCheck(true));

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
        findViewById(R.id.btn_export).setOnClickListener(v -> exportJson());

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
        cancelAnim(zygiskAnim);
        cancelAnim(shizukuAnim);
    }

    // ── Check orchestration ───────────────────────────────────────────────

    private void recheckAll() {
        rootResult    = null;
        bootResult    = null;
        zygiskResult  = null;
        shizukuResult = null;

        setLoading(rootPulse,    rootDot,    rootStatusTv,    1);
        setLoading(bootPulse,    bootDot,    bootStatusTv,    2);
        setLoading(zygiskPulse,  zygiskDot,  zygiskStatusTv,  3);
        setLoading(shizukuPulse, shizukuDot, shizukuStatusTv, 4);
        scoreText.setText("--/100");
        scoreText.setTextColor(getColor(R.color.green_dim));
        scoreBar.setProgress(0);
        rootConfidenceTv.setText("");
        bootConfidenceTv.setText("");
        zygiskConfidenceTv.setText("");

        executor.execute(() -> {
            RootChecker.Result root = RootChecker.check(this);
            mainHandler.post(() -> { applyRootResult(root); maybeUpdateScore(); });

            BootloaderChecker.Result boot = BootloaderChecker.check(root);
            mainHandler.post(() -> { applyBootResult(boot); maybeUpdateScore(); });

            ZygiskChecker.Result zy = ZygiskChecker.check();
            mainHandler.post(() -> { applyZygiskResult(zy); maybeUpdateScore(); });
        });

        runShizukuCheck(false);
    }

    private void runShizukuCheck(boolean updateScore) {
        ShizukuChecker.Result result = ShizukuChecker.check(this);
        applyShizukuResult(result);
        if (updateScore) maybeUpdateScore();
    }

    // ── Display methods ───────────────────────────────────────────────────

    private void applyRootResult(RootChecker.Result r) {
        rootResult = r;
        int color;  String label;
        switch (r.status) {
            case GRANTED: color = Color.parseColor("#FF3B3B"); label = "ROOTED";     break;
            case DENIED:  color = Color.parseColor("#00E676"); label = "NOT ROOTED"; break;
            default:      color = Color.parseColor("#FFD600"); label = "UNKNOWN";    break;
        }
        applyDotColor(rootPulse, rootDot, color);
        rootStatusTv.setText(label);
        rootStatusTv.setTextColor(color);
        startSonarPulse(rootPulse, color, 1);
        rootConfidenceTv.setText("confidence: " + r.confidence + "%");
        rootDetailsTv.setText(buildLines(r.lines));
    }

    private void applyBootResult(BootloaderChecker.Result r) {
        bootResult = r;
        int color;  String label;
        switch (r.status) {
            case LOCKED:   color = Color.parseColor("#00E676"); label = "LOCKED";   break;
            case UNLOCKED: color = Color.parseColor("#FF3B3B"); label = "UNLOCKED"; break;
            default:       color = Color.parseColor("#FFD600"); label = "UNKNOWN";  break;
        }
        if (r.status == BootloaderChecker.Status.UNKNOWN) {
            if (r.verifiedBoot == BootloaderChecker.VerifiedBootState.GREEN) {
                color = Color.parseColor("#00E676"); label = "LIKELY LOCKED";
            } else if (r.verifiedBoot == BootloaderChecker.VerifiedBootState.ORANGE) {
                color = Color.parseColor("#FF3B3B"); label = "LIKELY UNLOCKED";
            }
        }
        applyDotColor(bootPulse, bootDot, color);
        bootStatusTv.setText(label);
        bootStatusTv.setTextColor(color);
        startSonarPulse(bootPulse, color, 2);
        bootConfidenceTv.setText("hw-attest: " + r.hwAttestation
                + "  vb: " + r.verifiedBoot.name().toLowerCase());
        bootDetailsTv.setText(buildLines(r.lines));
    }

    private void applyZygiskResult(ZygiskChecker.Result r) {
        zygiskResult = r;
        int color;  String label;
        switch (r.status) {
            case DETECTED:  color = Color.parseColor("#FF3B3B"); label = "HOOKING DETECTED";  break;
            case SUSPECTED: color = Color.parseColor("#FFD600"); label = "SUSPECTED HOOKING"; break;
            case CLEAN:     color = Color.parseColor("#00E676"); label = "CLEAN";             break;
            default:        color = Color.parseColor("#FFD600"); label = "UNKNOWN";           break;
        }
        applyDotColor(zygiskPulse, zygiskDot, color);
        zygiskStatusTv.setText(label);
        zygiskStatusTv.setTextColor(color);
        startSonarPulse(zygiskPulse, color, 3);
        zygiskConfidenceTv.setText("confidence: " + r.confidence + "%");
        zygiskDetailsTv.setText(buildLines(r.lines));
    }

    private void applyShizukuResult(ShizukuChecker.Result r) {
        shizukuResult = r;
        int color;  String label;
        switch (r.status) {
            case AVAILABLE_PERMITTED: color = Color.parseColor("#FF3B3B"); label = "RUNNING / PERMITTED";  break;
            case AVAILABLE_DENIED:    color = Color.parseColor("#FFD600"); label = "RUNNING / NO PERM";    break;
            case INSTALLED_STOPPED:   color = Color.parseColor("#FFD600"); label = "INSTALLED / STOPPED";  break;
            case NOT_INSTALLED:       color = Color.parseColor("#00E676"); label = "NOT INSTALLED";        break;
            default:                  color = Color.parseColor("#FFD600"); label = "UNKNOWN";              break;
        }
        applyDotColor(shizukuPulse, shizukuDot, color);
        shizukuStatusTv.setText(label);
        shizukuStatusTv.setTextColor(color);
        startSonarPulse(shizukuPulse, color, 4);

        StringBuilder sb = new StringBuilder();
        for (String line : r.lines) sb.append("  ").append(line).append("\n");
        if (r.runMode != null) sb.append("  run mode  : ").append(r.runMode).append("\n");
        if (r.status == ShizukuChecker.Status.AVAILABLE_DENIED)
            sb.append("\n  [ TAP STATUS DOT TO REQUEST PERMISSION ]");
        shizukuDetailsTv.setText(sb.toString().trim());
    }

    private void maybeUpdateScore() {
        if (rootResult == null || bootResult == null
                || zygiskResult == null || shizukuResult == null) return;

        int score = 100;

        // Root (max −30)
        if (rootResult.status == RootChecker.Status.GRANTED)       score -= 30;
        else if (rootResult.status == RootChecker.Status.UNKNOWN)  score -= 5;

        // Bootloader (max −30)
        if (bootResult.status == BootloaderChecker.Status.UNLOCKED)       score -= 30;
        else if (bootResult.status == BootloaderChecker.Status.UNKNOWN)   score -= 5;

        // Verified boot (max −15)
        switch (bootResult.verifiedBoot) {
            case YELLOW:  score -=  5; break;
            case ORANGE:  score -= 10; break;
            case RED:     score -= 15; break;
            case UNKNOWN: score -=  5; break;
            default: break;
        }

        // dm-verity (max −5)
        if (!bootResult.dmVerityEnabled) score -= 5;

        // Zygisk / hooking (max −10)
        if      (zygiskResult.status == ZygiskChecker.Status.DETECTED)  score -= 10;
        else if (zygiskResult.status == ZygiskChecker.Status.SUSPECTED) score -= 5;

        // Suspicious mounts (max −5)
        if (rootResult.suspiciousMounts) score -= 5;

        // Shizuku (max −5)
        if (shizukuResult.status == ShizukuChecker.Status.AVAILABLE_PERMITTED) score -= 5;

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

    // ── JSON export ───────────────────────────────────────────────────────

    private void exportJson() {
        if (rootResult == null || bootResult == null
                || zygiskResult == null || shizukuResult == null) {
            Toast.makeText(this, "Run a scan first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject report = new JSONObject();
            report.put("generated",   new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.US).format(new Date()));
            report.put("app_version", "1.5");

            JSONObject root = new JSONObject();
            root.put("status",            rootResult.status.name());
            root.put("confidence_pct",    rootResult.confidence);
            root.put("su_path",           rootResult.suPath != null ? rootResult.suPath : "");
            root.put("root_manager",      rootResult.rootManager != null ? rootResult.rootManager : "");
            root.put("exec_java",         rootResult.execTestPassed);
            root.put("exec_native",       rootResult.nativeSuPassed);
            root.put("magisk_socket",     rootResult.magiskSocketFound);
            root.put("kernelsu_vfs",      rootResult.kernelSuVfs);
            root.put("apatch_vfs",        rootResult.apatchVfs);
            root.put("suspicious_mounts", rootResult.suspiciousMounts);
            root.put("fuse_fs",           rootResult.fuseMounts);
            report.put("root", root);

            JSONObject boot = new JSONObject();
            boot.put("status",            bootResult.status.name());
            boot.put("confidence_pct",    bootResult.confidence);
            boot.put("verified_boot",     bootResult.verifiedBoot.name());
            boot.put("dm_verity",         bootResult.dmVerityEnabled);
            boot.put("encryption",        bootResult.encryptionState);
            boot.put("debuggable",        bootResult.debuggable);
            boot.put("test_keys",         bootResult.testKeys);
            boot.put("hw_attestation",    bootResult.hwAttestation);
            boot.put("props_masked",      bootResult.propertiesMasked);
            report.put("bootloader", boot);

            JSONObject zy = new JSONObject();
            zy.put("status",             zygiskResult.status.name());
            zy.put("confidence_pct",     zygiskResult.confidence);
            zy.put("zygisk_in_maps",     zygiskResult.zygiskInMaps);
            zy.put("riru_in_maps",       zygiskResult.riruInMaps);
            zy.put("shamiko_in_maps",    zygiskResult.shamikoInMaps);
            zy.put("lsposed_process",    zygiskResult.lsposedProcess);
            zy.put("magisk_socket",      zygiskResult.magiskSocket);
            zy.put("magisk_mounts",      zygiskResult.magiskMounts);
            zy.put("proc1_maps",         zygiskResult.proc1Maps);
            zy.put("suspicious_fd",      zygiskResult.suspiciousFd);
            zy.put("tracing_active",     zygiskResult.tracingActive);
            zy.put("tracer_pid",         zygiskResult.tracerPid);
            report.put("zygisk_hooking", zy);

            JSONObject sh = new JSONObject();
            sh.put("status",    shizukuResult.status.name());
            sh.put("installed", shizukuResult.installed);
            sh.put("running",   shizukuResult.running);
            sh.put("permitted", shizukuResult.hasPermission);
            sh.put("version",   shizukuResult.version);
            sh.put("run_mode",  shizukuResult.runMode != null ? shizukuResult.runMode : "");
            report.put("shizuku", sh);

            JSONArray rawChecks = new JSONArray();
            for (String l : rootResult.lines)    rawChecks.put("root: "    + l);
            for (String l : bootResult.lines)    rawChecks.put("boot: "    + l);
            for (String l : zygiskResult.lines)  rawChecks.put("zygisk: "  + l);
            for (String l : shizukuResult.lines) rawChecks.put("shizuku: " + l);
            report.put("raw_checks", rawChecks);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "AccessChecker Report");
            intent.putExtra(Intent.EXTRA_TEXT, report.toString(2));
            startActivity(Intent.createChooser(intent, "Share Report"));

        } catch (JSONException e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Animation ─────────────────────────────────────────────────────────

    private void applyDotColor(View pulse, View dot, int color) {
        int alpha = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
        setCircleColor(pulse, alpha);
        setCircleColor(dot, color);
    }

    private void setCircleColor(View v, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        v.setBackground(d);
    }

    private void setLoading(View pulse, View dot, TextView statusTv, int slot) {
        int gray = Color.parseColor("#444444");
        applyDotColor(pulse, dot, gray);
        statusTv.setText("CHECKING...");
        statusTv.setTextColor(Color.parseColor("#888888"));
        cancelAnim(animForSlot(slot));
        startSonarPulse(pulse, gray, slot);
    }

    private AnimatorSet animForSlot(int slot) {
        switch (slot) {
            case 1: return rootAnim;
            case 2: return bootAnim;
            case 3: return zygiskAnim;
            default: return shizukuAnim;
        }
    }

    private void startSonarPulse(View pulse, int color, int slot) {
        cancelAnim(animForSlot(slot));
        pulse.setScaleX(1f);
        pulse.setScaleY(1f);
        pulse.setAlpha(0.75f);
        setCircleColor(pulse, Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)));

        ObjectAnimator sx = ObjectAnimator.ofFloat(pulse, "scaleX", 1f, 3f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(pulse, "scaleY", 1f, 3f);
        ObjectAnimator al = ObjectAnimator.ofFloat(pulse, "alpha",  0.75f, 0f);
        for (ObjectAnimator a : new ObjectAnimator[]{ sx, sy, al }) {
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.RESTART);
            a.setDuration(2200);
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, al);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();

        switch (slot) {
            case 1: rootAnim    = set; break;
            case 2: bootAnim    = set; break;
            case 3: zygiskAnim  = set; break;
            default: shizukuAnim = set; break;
        }
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
        rootConfidenceTv   = findViewById(R.id.root_confidence);

        bootPulse          = findViewById(R.id.boot_pulse);
        bootDot            = findViewById(R.id.boot_dot);
        bootStatusTv       = findViewById(R.id.boot_status);
        bootDetailsTv      = findViewById(R.id.boot_details_text);
        bootExpandTv       = findViewById(R.id.boot_expand);
        bootDetailsLayout  = findViewById(R.id.boot_details);
        bootConfidenceTv   = findViewById(R.id.boot_confidence);

        zygiskPulse         = findViewById(R.id.zygisk_pulse);
        zygiskDot           = findViewById(R.id.zygisk_dot);
        zygiskStatusTv      = findViewById(R.id.zygisk_status);
        zygiskDetailsTv     = findViewById(R.id.zygisk_details_text);
        zygiskExpandTv      = findViewById(R.id.zygisk_expand);
        zygiskDetailsLayout = findViewById(R.id.zygisk_details);
        zygiskConfidenceTv  = findViewById(R.id.zygisk_confidence);

        shizukuPulse        = findViewById(R.id.shizuku_pulse);
        shizukuDot          = findViewById(R.id.shizuku_dot);
        shizukuStatusTv     = findViewById(R.id.shizuku_status);
        shizukuDetailsTv    = findViewById(R.id.shizuku_details_text);
        shizukuExpandTv     = findViewById(R.id.shizuku_expand);
        shizukuDetailsLayout = findViewById(R.id.shizuku_details);

        scoreText = findViewById(R.id.score_text);
        scoreBar  = findViewById(R.id.score_bar);
    }

    private void setupCardClicks() {
        wireExpand(R.id.root_header,    rootDetailsLayout,    rootExpandTv);
        wireExpand(R.id.boot_header,    bootDetailsLayout,    bootExpandTv);
        wireExpand(R.id.zygisk_header,  zygiskDetailsLayout,  zygiskExpandTv);
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

    private static String buildLines(java.util.List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append("\n");
        return sb.toString().trim();
    }
}
