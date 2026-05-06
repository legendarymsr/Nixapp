package com.accesschecker;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rikka.shizuku.Shizuku;

/* loaded from: classes3.dex */
public class MainActivity extends AppCompatActivity {
    private static final int SHIZUKU_REQUEST_CODE = 1001;
    private AnimatorSet bootAnim;
    private LinearLayout bootDetailsLayout;
    private TextView bootDetailsTv;
    private View bootDot;
    private TextView bootExpandTv;
    private View bootPulse;
    private volatile BootloaderChecker.Result bootResult;
    private TextView bootStatusTv;
    private AnimatorSet rootAnim;
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Shizuku.OnBinderReceivedListener onBinderReceived = new Shizuku.OnBinderReceivedListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda9
        @Override // rikka.shizuku.Shizuku.OnBinderReceivedListener
        public final void onBinderReceived() {
            MainActivity.this.m47lambda$new$1$comaccesscheckerMainActivity();
        }
    };
    private final Shizuku.OnBinderDeadListener onBinderDead = new Shizuku.OnBinderDeadListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda10
        @Override // rikka.shizuku.Shizuku.OnBinderDeadListener
        public final void onBinderDead() {
            MainActivity.this.m49lambda$new$3$comaccesscheckerMainActivity();
        }
    };
    private final Shizuku.OnRequestPermissionResultListener onPermResult = new Shizuku.OnRequestPermissionResultListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda11
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
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda8
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
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m48lambda$new$2$comaccesscheckerMainActivity();
            }
        });
    }

    /* renamed from: lambda$new$5$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m51lambda$new$5$comaccesscheckerMainActivity(int code, int result) {
        if (code == 1001) {
            this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda4
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
        findViewById(R.id.btn_recheck).setOnClickListener(new View.OnClickListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m53lambda$onCreate$7$comaccesscheckerMainActivity(view);
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
        cancelAnim(this.shizukuAnim);
    }

    private void recheckAll() {
        this.rootResult = null;
        this.bootResult = null;
        this.shizukuResult = null;
        setLoading(this.rootPulse, this.rootDot, this.rootStatusTv);
        setLoading(this.bootPulse, this.bootDot, this.bootStatusTv);
        setLoading(this.shizukuPulse, this.shizukuDot, this.shizukuStatusTv);
        this.scoreText.setText("--/100");
        this.scoreText.setTextColor(getColor(R.color.green_dim));
        this.scoreBar.setProgress(0);
        this.executor.execute(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m54lambda$recheckAll$10$comaccesscheckerMainActivity();
            }
        });
        runShizukuCheck(false);
    }

    /* renamed from: lambda$recheckAll$10$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m54lambda$recheckAll$10$comaccesscheckerMainActivity() {
        final RootChecker.Result root = RootChecker.check(this);
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m55lambda$recheckAll$8$comaccesscheckerMainActivity(root);
            }
        });
        final BootloaderChecker.Result boot = BootloaderChecker.check(root);
        this.mainHandler.post(new Runnable() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m56lambda$recheckAll$9$comaccesscheckerMainActivity(boot);
            }
        });
    }

    /* renamed from: lambda$recheckAll$8$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m55lambda$recheckAll$8$comaccesscheckerMainActivity(RootChecker.Result root) {
        applyRootResult(root);
        maybeUpdateScore();
    }

    /* renamed from: lambda$recheckAll$9$com-accesschecker-MainActivity, reason: not valid java name */
    /* synthetic */ void m56lambda$recheckAll$9$comaccesscheckerMainActivity(BootloaderChecker.Result boot) {
        applyBootResult(boot);
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
        StringBuilder sb = new StringBuilder();
        for (String line : r.lines) {
            sb.append("  ").append(line).append("\n");
        }
        this.rootDetailsTv.setText(sb.toString().trim());
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
        StringBuilder sb = new StringBuilder();
        for (String line : r.lines) {
            sb.append("  ").append(line).append("\n");
        }
        this.bootDetailsTv.setText(sb.toString().trim());
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
        startSonarPulse(this.shizukuPulse, color, 3);
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
        if (this.rootResult == null || this.bootResult == null || this.shizukuResult == null) {
            return;
        }
        int score = 100;
        if (this.rootResult.status == RootChecker.Status.GRANTED) {
            score = 100 - 35;
        } else if (this.rootResult.status == RootChecker.Status.UNKNOWN) {
            score = 100 - 5;
        }
        if (this.bootResult.status == BootloaderChecker.Status.UNLOCKED) {
            score -= 35;
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
        if (this.shizukuResult.status == ShizukuChecker.Status.AVAILABLE_PERMITTED) {
            score -= 10;
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

    private void applyDotColor(View pulse, View dot, int color) {
        int pulseAlpha = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
        setCircleColor(pulse, pulseAlpha);
        setCircleColor(dot, color);
    }

    private void setCircleColor(View v, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(1);
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
        return pulse == this.rootPulse ? this.rootAnim : pulse == this.bootPulse ? this.bootAnim : this.shizukuAnim;
    }

    private void startSonarPulse(View pulse, int color, int slot) {
        int i = 1;
        AnimatorSet old = slot == 1 ? this.rootAnim : slot == 2 ? this.bootAnim : this.shizukuAnim;
        cancelAnim(old);
        pulse.setScaleX(1.0f);
        pulse.setScaleY(1.0f);
        pulse.setAlpha(0.75f);
        int pulseAlpha = Color.argb(70, Color.red(color), Color.green(color), Color.blue(color));
        setCircleColor(pulse, pulseAlpha);
        ObjectAnimator sx = ObjectAnimator.ofFloat(pulse, "scaleX", 1.0f, 3.0f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(pulse, "scaleY", 1.0f, 3.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(pulse, "alpha", 0.75f, 0.0f);
        ObjectAnimator[] objectAnimatorArr = {sx, sy, alpha};
        int i2 = 0;
        while (i2 < 3) {
            ObjectAnimator a = objectAnimatorArr[i2];
            a.setRepeatCount(-1);
            a.setRepeatMode(i);
            a.setDuration(2200L);
            i2++;
            i = 1;
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, alpha);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();
        if (slot == 1) {
            this.rootAnim = set;
        } else if (slot == 2) {
            this.bootAnim = set;
        } else {
            this.shizukuAnim = set;
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
        this.bootPulse = findViewById(R.id.boot_pulse);
        this.bootDot = findViewById(R.id.boot_dot);
        this.bootStatusTv = (TextView) findViewById(R.id.boot_status);
        this.bootDetailsTv = (TextView) findViewById(R.id.boot_details_text);
        this.bootExpandTv = (TextView) findViewById(R.id.boot_expand);
        this.bootDetailsLayout = (LinearLayout) findViewById(R.id.boot_details);
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
        wireExpand(R.id.shizuku_header, this.shizukuDetailsLayout, this.shizukuExpandTv);
    }

    private void wireExpand(int headerId, final LinearLayout details, final TextView arrow) {
        findViewById(headerId).setOnClickListener(new View.OnClickListener() { // from class: com.accesschecker.MainActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.lambda$wireExpand$11(details, arrow, view);
            }
        });
    }

    static /* synthetic */ void lambda$wireExpand$11(LinearLayout details, TextView arrow, View v) {
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
}
