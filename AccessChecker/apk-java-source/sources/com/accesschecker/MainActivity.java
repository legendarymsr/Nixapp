package com.accesschecker;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.accesschecker.BootloaderChecker;
import com.accesschecker.RootChecker;
import com.accesschecker.ShizukuChecker;
import com.accesschecker.ZygiskChecker;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rikka.shizuku.Shizuku;

/* loaded from: classes3.dex */
public class MainActivity extends AppCompatActivity {
    private static final int SHIZUKU_REQUEST_CODE = 1001;
    private AnimatorSet bootAnim;
    private TextView bootConfidenceTv;
    private LinearLayout bootDetailsLayout;
    private TextView bootDetailsTv;
    private View bootDot;
    private TextView bootExpandTv;
    private View bootPulse;
    private volatile BootloaderChecker.Result bootResult;
    private TextView bootStatusTv;
    private AnimatorSet rootAnim;
    private TextView rootConfidenceTv;
    private LinearLayout rootDetailsLayout;
    private TextView rootDetailsTv;
    private View rootDot;
    private TextView rootExpandTv;
    private View rootPulse;
    private volatile RootChecker.Result rootResult;
    private TextView rootStatusTv;
    private ProgressBar scoreBar;
    private TextView scoreText;
    private AnimatorSet shizukuAnim;
    private LinearLayout shizukuDetailsLayout;
    private TextView shizukuDetailsTv;
    private View shizukuDot;
    private TextView shizukuExpandTv;
    private View shizukuPulse;
    private volatile ShizukuChecker.Result shizukuResult;
    private TextView shizukuStatusTv;
    private AnimatorSet zygiskAnim;
    private TextView zygiskConfidenceTv;
    private LinearLayout zygiskDetailsLayout;
    private TextView zygiskDetailsTv;
    private View zygiskDot;
    private TextView zygiskExpandTv;
    private View zygiskPulse;
    private volatile ZygiskChecker.Result zygiskResult;
    private TextView zygiskStatusTv;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Shizuku.OnBinderReceivedListener onBinderReceived = new Shizuku.OnBinderReceivedListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda10
        @Override // rikka.shizuku.Shizuku.OnBinderReceivedListener
        public final void onBinderReceived() {
            MainActivity.this.m47lambda$new$1$comaccesscheckerMainActivity();
        }
    };
    private final Shizuku.OnBinderDeadListener onBinderDead = new Shizuku.OnBinderDeadListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda11
        @Override // rikka.shizuku.Shizuku.OnBinderDeadListener
        public final void onBinderDead() {
            MainActivity.this.m49lambda$new$3$comaccesscheckerMainActivity();
        }
    };
    private final Shizuku.OnRequestPermissionResultListener onPermResult = new Shizuku.OnRequestPermissionResultListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda12
        @Override // rikka.shizuku.Shizuku.OnRequestPermissionResultListener
        public final void onRequestPermissionResult(int i, int i2) {
            MainActivity.this.m51lambda$new$5$comaccesscheckerMainActivity(i, i2);
        }
    };

    /* renamed from: lambda$new$0$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m46lambda$new$0$comaccesscheckerMainActivity() {
        runShizukuCheck(true);
    }

    /* renamed from: lambda$new$1$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m47lambda$new$1$comaccesscheckerMainActivity() {
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m46lambda$new$0$comaccesscheckerMainActivity();
            }
        });
    }

    /* renamed from: lambda$new$2$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m48lambda$new$2$comaccesscheckerMainActivity() {
        runShizukuCheck(true);
    }

    /* renamed from: lambda$new$3$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m49lambda$new$3$comaccesscheckerMainActivity() {
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m48lambda$new$2$comaccesscheckerMainActivity();
            }
        });
    }

    /* renamed from: lambda$new$5$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m51lambda$new$5$comaccesscheckerMainActivity(int code, int result) {
        if (code == 1001) {
            this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.this.m50lambda$new$4$comaccesscheckerMainActivity();
                }
            });
        }
    }

    /* renamed from: lambda$new$4$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m50lambda$new$4$comaccesscheckerMainActivity() {
        runShizukuCheck(true);
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupCardClicks();
        try {
            Shizuku.addBinderReceivedListenerSticky(this.onBinderReceived);
            Shizuku.addBinderDeadListener(this.onBinderDead);
            Shizuku.addRequestPermissionResultListener(this.onPermResult);
        } catch (Throwable th) {
        }
        this.shizukuDot.setOnClickListener(new View.OnClickListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m52lambda$onCreate$6$comaccesscheckerMainActivity(view);
            }
        });
        findViewById(R.id.btn_recheck).setOnClickListener(new View.OnClickListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m53lambda$onCreate$7$comaccesscheckerMainActivity(view);
            }
        });
        findViewById(R.id.btn_export).setOnClickListener(new View.OnClickListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m54lambda$onCreate$8$comaccesscheckerMainActivity(view);
            }
        });
        recheckAll();
    }

    /* renamed from: lambda$onCreate$6$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m52lambda$onCreate$6$comaccesscheckerMainActivity(View v) {
        requestShizukuPermission();
    }

    /* renamed from: lambda$onCreate$7$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m53lambda$onCreate$7$comaccesscheckerMainActivity(View v) {
        recheckAll();
    }

    /* renamed from: lambda$onCreate$8$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m54lambda$onCreate$8$comaccesscheckerMainActivity(View v) {
        exportJson();
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeBinderReceivedListener(this.onBinderReceived);
            Shizuku.removeBinderDeadListener(this.onBinderDead);
            Shizuku.removeRequestPermissionResultListener(this.onPermResult);
        } catch (Throwable th) {
        }
        this.executor.shutdown();
        cancelAnim(this.rootAnim);
        cancelAnim(this.bootAnim);
        cancelAnim(this.zygiskAnim);
        cancelAnim(this.shizukuAnim);
    }

    private void recheckAll() {
        this.rootResult = null;
        this.bootResult = null;
        this.zygiskResult = null;
        this.shizukuResult = null;
        setLoading(this.rootPulse, this.rootDot, this.rootStatusTv, 1);
        setLoading(this.bootPulse, this.bootDot, this.bootStatusTv, 2);
        setLoading(this.zygiskPulse, this.zygiskDot, this.zygiskStatusTv, 3);
        setLoading(this.shizukuPulse, this.shizukuDot, this.shizukuStatusTv, 4);
        this.scoreText.setText("--/100");
        this.scoreText.setTextColor(getColor(R.color.green_dim));
        this.scoreBar.setProgress(0);
        this.rootConfidenceTv.setText("");
        this.bootConfidenceTv.setText("");
        this.zygiskConfidenceTv.setText("");
        this.executor.execute(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m57lambda$recheckAll$12$comaccesscheckerMainActivity();
            }
        });
        runShizukuCheck(false);
    }

    /* renamed from: lambda$recheckAll$12$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m57lambda$recheckAll$12$comaccesscheckerMainActivity() {
        final RootChecker.Result root = RootChecker.check(this);
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m58lambda$recheckAll$9$comaccesscheckerMainActivity(root);
            }
        });
        final BootloaderChecker.Result boot = BootloaderChecker.check(root);
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m55lambda$recheckAll$10$comaccesscheckerMainActivity(boot);
            }
        });
        final ZygiskChecker.Result zy = ZygiskChecker.check();
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m56lambda$recheckAll$11$comaccesscheckerMainActivity(zy);
            }
        });
    }

    /* renamed from: lambda$recheckAll$9$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m58lambda$recheckAll$9$comaccesscheckerMainActivity(RootChecker.Result root) {
        applyRootResult(root);
        maybeUpdateScore();
    }

    /* renamed from: lambda$recheckAll$10$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m55lambda$recheckAll$10$comaccesscheckerMainActivity(BootloaderChecker.Result boot) {
        applyBootResult(boot);
        maybeUpdateScore();
    }

    /* renamed from: lambda$recheckAll$11$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m56lambda$recheckAll$11$comaccesscheckerMainActivity(ZygiskChecker.Result zy) {
        applyZygiskResult(zy);
        maybeUpdateScore();
    }

    private void runShizukuCheck(boolean updateScore) {
        ShizukuChecker.Result result = ShizukuChecker.check(this);
        applyShizukuResult(result);
        if (updateScore) {
            maybeUpdateScore();
        }
    }

    private void applyRootResult(RootChecker.Result r) {
        int color;
        String label;
        this.rootResult = r;
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
        applyDotColor(this.rootPulse, this.rootDot, color);
        this.rootStatusTv.setText(label);
        this.rootStatusTv.setTextColor(color);
        startSonarPulse(this.rootPulse, color, 1);
        this.rootConfidenceTv.setText("confidence: " + r.confidence + "%");
        this.rootDetailsTv.setText(buildLines(r.lines));
    }

    private void applyBootResult(BootloaderChecker.Result r) {
        int color;
        String label;
        this.bootResult = r;
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
        if (r.status == BootloaderChecker.Status.UNKNOWN) {
            if (r.verifiedBoot == BootloaderChecker.VerifiedBootState.GREEN) {
                color = Color.parseColor("#00E676");
                label = "LIKELY LOCKED";
            } else if (r.verifiedBoot == BootloaderChecker.VerifiedBootState.ORANGE) {
                color = Color.parseColor("#FF3B3B");
                label = "LIKELY UNLOCKED";
            }
        }
        applyDotColor(this.bootPulse, this.bootDot, color);
        this.bootStatusTv.setText(label);
        this.bootStatusTv.setTextColor(color);
        startSonarPulse(this.bootPulse, color, 2);
        this.bootConfidenceTv.setText("hw-attest: " + r.hwAttestation + "  vb: " + r.verifiedBoot.name().toLowerCase());
        this.bootDetailsTv.setText(buildLines(r.lines));
    }

    private void applyZygiskResult(ZygiskChecker.Result r) {
        int color;
        String label;
        this.zygiskResult = r;
        switch (r.status) {
            case DETECTED:
                color = Color.parseColor("#FF3B3B");
                label = "HOOKING DETECTED";
                break;
            case SUSPECTED:
                color = Color.parseColor("#FFD600");
                label = "SUSPECTED HOOKING";
                break;
            case CLEAN:
                color = Color.parseColor("#00E676");
                label = "CLEAN";
                break;
            default:
                color = Color.parseColor("#FFD600");
                label = "UNKNOWN";
                break;
        }
        applyDotColor(this.zygiskPulse, this.zygiskDot, color);
        this.zygiskStatusTv.setText(label);
        this.zygiskStatusTv.setTextColor(color);
        startSonarPulse(this.zygiskPulse, color, 3);
        this.zygiskConfidenceTv.setText("confidence: " + r.confidence + "%");
        this.zygiskDetailsTv.setText(buildLines(r.lines));
    }

    private void applyShizukuResult(ShizukuChecker.Result r) {
        int color;
        String label;
        this.shizukuResult = r;
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
        applyDotColor(this.shizukuPulse, this.shizukuDot, color);
        this.shizukuStatusTv.setText(label);
        this.shizukuStatusTv.setTextColor(color);
        startSonarPulse(this.shizukuPulse, color, 4);
        StringBuilder sb = new StringBuilder();
        for (String line : r.lines) {
            sb.append("  ").append(line).append("\n");
        }
        if (r.runMode != null) {
            sb.append("  run mode  : ").append(r.runMode).append("\n");
        }
        if (r.status == ShizukuChecker.Status.AVAILABLE_DENIED) {
            sb.append("\n  [ TAP STATUS DOT TO REQUEST PERMISSION ]");
        }
        this.shizukuDetailsTv.setText(sb.toString().trim());
    }

    private void maybeUpdateScore() {
        int scoreColor;
        if (this.rootResult == null || this.bootResult == null || this.zygiskResult == null || this.shizukuResult == null) {
            return;
        }
        int score = 100;
        if (this.rootResult.status == RootChecker.Status.GRANTED) {
            score = 100 - 30;
        } else if (this.rootResult.status == RootChecker.Status.UNKNOWN) {
            score = 100 - 5;
        }
        if (this.bootResult.status == BootloaderChecker.Status.UNLOCKED) {
            score -= 30;
        } else if (this.bootResult.status == BootloaderChecker.Status.UNKNOWN) {
            score -= 5;
        }
        switch (this.bootResult.verifiedBoot) {
            case YELLOW:
                score -= 5;
                break;
            case ORANGE:
                score -= 10;
                break;
            case RED:
                score -= 15;
                break;
            case UNKNOWN:
                score -= 5;
                break;
        }
        if (!this.bootResult.dmVerityEnabled) {
            score -= 5;
        }
        if (this.zygiskResult.status == ZygiskChecker.Status.DETECTED) {
            score -= 10;
        } else if (this.zygiskResult.status == ZygiskChecker.Status.SUSPECTED) {
            score -= 5;
        }
        if (this.rootResult.suspiciousMounts) {
            score -= 5;
        }
        if (this.shizukuResult.status == ShizukuChecker.Status.AVAILABLE_PERMITTED) {
            score -= 5;
        }
        int score2 = Math.max(0, score);
        if (score2 >= 80) {
            scoreColor = Color.parseColor("#00E676");
        } else {
            scoreColor = score2 >= 50 ? Color.parseColor("#FFD600") : Color.parseColor("#FF3B3B");
        }
        this.scoreText.setText(score2 + "/100");
        this.scoreText.setTextColor(scoreColor);
        ObjectAnimator barAnim = ObjectAnimator.ofInt(this.scoreBar, NotificationCompat.CATEGORY_PROGRESS, this.scoreBar.getProgress(), score2);
        barAnim.setDuration(900L);
        barAnim.start();
        this.scoreBar.setProgressTintList(ColorStateList.valueOf(scoreColor));
    }

    private void exportJson() {
        if (this.rootResult == null || this.bootResult == null || this.zygiskResult == null || this.shizukuResult == null) {
            Toast.makeText(this, "Run a scan first", 0).show();
            return;
        }
        try {
            JSONObject report = new JSONObject();
            report.put("generated", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));
            report.put("app_version", "1.5");
            JSONObject root = new JSONObject();
            root.put(NotificationCompat.CATEGORY_STATUS, this.rootResult.status.name());
            root.put("confidence_pct", this.rootResult.confidence);
            root.put("su_path", this.rootResult.suPath != null ? this.rootResult.suPath : "");
            root.put("root_manager", this.rootResult.rootManager != null ? this.rootResult.rootManager : "");
            root.put("exec_java", this.rootResult.execTestPassed);
            root.put("exec_native", this.rootResult.nativeSuPassed);
            root.put("magisk_socket", this.rootResult.magiskSocketFound);
            root.put("kernelsu_vfs", this.rootResult.kernelSuVfs);
            root.put("apatch_vfs", this.rootResult.apatchVfs);
            root.put("suspicious_mounts", this.rootResult.suspiciousMounts);
            root.put("fuse_fs", this.rootResult.fuseMounts);
            report.put("root", root);
            JSONObject boot = new JSONObject();
            boot.put(NotificationCompat.CATEGORY_STATUS, this.bootResult.status.name());
            boot.put("confidence_pct", this.bootResult.confidence);
            boot.put("verified_boot", this.bootResult.verifiedBoot.name());
            boot.put("dm_verity", this.bootResult.dmVerityEnabled);
            boot.put("encryption", this.bootResult.encryptionState);
            boot.put("debuggable", this.bootResult.debuggable);
            boot.put("test_keys", this.bootResult.testKeys);
            boot.put("hw_attestation", this.bootResult.hwAttestation);
            boot.put("props_masked", this.bootResult.propertiesMasked);
            report.put("bootloader", boot);
            JSONObject zy = new JSONObject();
            zy.put(NotificationCompat.CATEGORY_STATUS, this.zygiskResult.status.name());
            zy.put("confidence_pct", this.zygiskResult.confidence);
            zy.put("zygisk_in_maps", this.zygiskResult.zygiskInMaps);
            zy.put("riru_in_maps", this.zygiskResult.riruInMaps);
            zy.put("shamiko_in_maps", this.zygiskResult.shamikoInMaps);
            zy.put("lsposed_process", this.zygiskResult.lsposedProcess);
            zy.put("magisk_socket", this.zygiskResult.magiskSocket);
            zy.put("magisk_mounts", this.zygiskResult.magiskMounts);
            zy.put("proc1_maps", this.zygiskResult.proc1Maps);
            zy.put("suspicious_fd", this.zygiskResult.suspiciousFd);
            zy.put("tracing_active", this.zygiskResult.tracingActive);
            zy.put("tracer_pid", this.zygiskResult.tracerPid);
            report.put("zygisk_hooking", zy);
            JSONObject sh = new JSONObject();
            sh.put(NotificationCompat.CATEGORY_STATUS, this.shizukuResult.status.name());
            sh.put("installed", this.shizukuResult.installed);
            sh.put("running", this.shizukuResult.running);
            sh.put("permitted", this.shizukuResult.hasPermission);
            sh.put("version", this.shizukuResult.version);
            sh.put("run_mode", this.shizukuResult.runMode != null ? this.shizukuResult.runMode : "");
            report.put("shizuku", sh);
            JSONArray rawChecks = new JSONArray();
            for (String l : this.rootResult.lines) {
                rawChecks.put("root: " + l);
            }
            for (String l2 : this.bootResult.lines) {
                rawChecks.put("boot: " + l2);
            }
            for (String l3 : this.zygiskResult.lines) {
                rawChecks.put("zygisk: " + l3);
            }
            for (String l4 : this.shizukuResult.lines) {
                rawChecks.put("shizuku: " + l4);
            }
            report.put("raw_checks", rawChecks);
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.SUBJECT", "AccessChecker Report");
            intent.putExtra("android.intent.extra.TEXT", report.toString(2));
            startActivity(Intent.createChooser(intent, "Share Report"));
        } catch (JSONException e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), 1).show();
        }
    }

    private void applyDotColor(View pulse, View dot, int color) {
        int alpha = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
        setCircleColor(pulse, alpha);
        setCircleColor(dot, color);
    }

    private void setCircleColor(View v, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(1);
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
            case 1:
                return this.rootAnim;
            case 2:
                return this.bootAnim;
            case 3:
                return this.zygiskAnim;
            default:
                return this.shizukuAnim;
        }
    }

    private void startSonarPulse(View pulse, int color, int slot) {
        cancelAnim(animForSlot(slot));
        pulse.setScaleX(1.0f);
        pulse.setScaleY(1.0f);
        pulse.setAlpha(0.75f);
        setCircleColor(pulse, Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)));
        ObjectAnimator sx = ObjectAnimator.ofFloat(pulse, "scaleX", 1.0f, 3.0f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(pulse, "scaleY", 1.0f, 3.0f);
        ObjectAnimator al = ObjectAnimator.ofFloat(pulse, "alpha", 0.75f, 0.0f);
        ObjectAnimator[] objectAnimatorArr = {sx, sy, al};
        for (int i = 0; i < 3; i++) {
            ObjectAnimator a = objectAnimatorArr[i];
            a.setRepeatCount(-1);
            a.setRepeatMode(1);
            a.setDuration(2200L);
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, al);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();
        switch (slot) {
            case 1:
                this.rootAnim = set;
                break;
            case 2:
                this.bootAnim = set;
                break;
            case 3:
                this.zygiskAnim = set;
                break;
            default:
                this.shizukuAnim = set;
                break;
        }
    }

    private static void cancelAnim(AnimatorSet a) {
        if (a == null || !a.isRunning()) {
            return;
        }
        a.cancel();
    }

    private void bindViews() {
        this.rootPulse = findViewById(R.id.root_pulse);
        this.rootDot = findViewById(R.id.root_dot);
        this.rootStatusTv = (TextView) findViewById(R.id.root_status);
        this.rootDetailsTv = (TextView) findViewById(R.id.root_details_text);
        this.rootExpandTv = (TextView) findViewById(R.id.root_expand);
        this.rootDetailsLayout = (LinearLayout) findViewById(R.id.root_details);
        this.rootConfidenceTv = (TextView) findViewById(R.id.root_confidence);
        this.bootPulse = findViewById(R.id.boot_pulse);
        this.bootDot = findViewById(R.id.boot_dot);
        this.bootStatusTv = (TextView) findViewById(R.id.boot_status);
        this.bootDetailsTv = (TextView) findViewById(R.id.boot_details_text);
        this.bootExpandTv = (TextView) findViewById(R.id.boot_expand);
        this.bootDetailsLayout = (LinearLayout) findViewById(R.id.boot_details);
        this.bootConfidenceTv = (TextView) findViewById(R.id.boot_confidence);
        this.zygiskPulse = findViewById(R.id.zygisk_pulse);
        this.zygiskDot = findViewById(R.id.zygisk_dot);
        this.zygiskStatusTv = (TextView) findViewById(R.id.zygisk_status);
        this.zygiskDetailsTv = (TextView) findViewById(R.id.zygisk_details_text);
        this.zygiskExpandTv = (TextView) findViewById(R.id.zygisk_expand);
        this.zygiskDetailsLayout = (LinearLayout) findViewById(R.id.zygisk_details);
        this.zygiskConfidenceTv = (TextView) findViewById(R.id.zygisk_confidence);
        this.shizukuPulse = findViewById(R.id.shizuku_pulse);
        this.shizukuDot = findViewById(R.id.shizuku_dot);
        this.shizukuStatusTv = (TextView) findViewById(R.id.shizuku_status);
        this.shizukuDetailsTv = (TextView) findViewById(R.id.shizuku_details_text);
        this.shizukuExpandTv = (TextView) findViewById(R.id.shizuku_expand);
        this.shizukuDetailsLayout = (LinearLayout) findViewById(R.id.shizuku_details);
        this.scoreText = (TextView) findViewById(R.id.score_text);
        this.scoreBar = (ProgressBar) findViewById(R.id.score_bar);
    }

    private void setupCardClicks() {
        wireExpand(R.id.root_header, this.rootDetailsLayout, this.rootExpandTv);
        wireExpand(R.id.boot_header, this.bootDetailsLayout, this.bootExpandTv);
        wireExpand(R.id.zygisk_header, this.zygiskDetailsLayout, this.zygiskExpandTv);
        wireExpand(R.id.shizuku_header, this.shizukuDetailsLayout, this.shizukuExpandTv);
    }

    private void wireExpand(int headerId, final LinearLayout details, final TextView arrow) {
        findViewById(headerId).setOnClickListener(new View.OnClickListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.lambda$wireExpand$13(details, arrow, view);
            }
        });
    }

    static /* synthetic */ void lambda$wireExpand$13(LinearLayout details, TextView arrow, View v) {
        boolean visible = details.getVisibility() == 0;
        details.setVisibility(visible ? 8 : 0);
        arrow.setText(visible ? "▼" : "▲");
    }

    private void requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Shizuku service not running", 0).show();
                return;
            }
            if (Shizuku.isPreV11()) {
                Toast.makeText(this, "Shizuku v11+ required", 0).show();
            } else if (Shizuku.checkSelfPermission() == 0) {
                Toast.makeText(this, "Permission already granted", 0).show();
            } else {
                Shizuku.requestPermission(1001);
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Shizuku error: " + t.getMessage(), 0).show();
        }
    }

    private static String buildLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l).append("\n");
        }
        return sb.toString().trim();
    }
}
