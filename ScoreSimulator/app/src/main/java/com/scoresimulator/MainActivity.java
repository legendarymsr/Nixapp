package com.scoresimulator;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Score card
    private TextView  scoreText;
    private ProgressBar scoreBar;
    private TextView  breakdownText;

    // Spinners
    private Spinner spinRoot;
    private Spinner spinBootloader;
    private Spinner spinVerifiedBoot;
    private Spinner spinShizuku;

    // Badges (per-card deduction labels)
    private TextView rootBadge;
    private TextView blBadge;
    private TextView vbBadge;
    private TextView shBadge;

    // Flags
    private Switch  swDmVerity;
    private TextView verityHint;

    // Spinner indices — Root
    private static final int ROOT_DENIED  = 0;
    private static final int ROOT_UNKNOWN = 1;
    private static final int ROOT_GRANTED = 2;

    // Spinner indices — Bootloader
    private static final int BL_LOCKED   = 0;
    private static final int BL_UNKNOWN  = 1;
    private static final int BL_UNLOCKED = 2;

    // Spinner indices — Verified Boot
    private static final int VB_GREEN   = 0;
    private static final int VB_YELLOW  = 1;
    private static final int VB_ORANGE  = 2;
    private static final int VB_RED     = 3;
    private static final int VB_UNKNOWN = 4;

    // Spinner indices — Shizuku
    private static final int SH_NONE      = 0;
    private static final int SH_DENIED    = 1;
    private static final int SH_PERMITTED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scoreText     = findViewById(R.id.score_text);
        scoreBar      = findViewById(R.id.score_bar);
        breakdownText = findViewById(R.id.breakdown_text);
        spinRoot          = findViewById(R.id.spin_root);
        spinBootloader    = findViewById(R.id.spin_bootloader);
        spinVerifiedBoot  = findViewById(R.id.spin_verified_boot);
        spinShizuku       = findViewById(R.id.spin_shizuku);
        rootBadge = findViewById(R.id.root_badge);
        blBadge   = findViewById(R.id.bl_badge);
        vbBadge   = findViewById(R.id.vb_badge);
        shBadge   = findViewById(R.id.sh_badge);
        swDmVerity  = findViewById(R.id.sw_dm_verity);
        verityHint  = findViewById(R.id.verity_hint);

        setupSpinner(spinRoot, new String[]{
            "DENIED   (no root detected)    ±0",
            "UNKNOWN  (weak signals only)   −5",
            "GRANTED  (rooted device)      −30"
        });
        setupSpinner(spinBootloader, new String[]{
            "LOCKED   (secure)              ±0",
            "UNKNOWN                        −5",
            "UNLOCKED                      −35"
        });
        setupSpinner(spinVerifiedBoot, new String[]{
            "GREEN    (verified)            ±0",
            "YELLOW   (custom key)          −5",
            "ORANGE   (unlocked BL)        −10",
            "RED      (verification failed)−15",
            "UNKNOWN                        −5"
        });
        setupSpinner(spinShizuku, new String[]{
            "not installed                  ±0",
            "installed, not permitted       ±0",
            "installed, PERMITTED          −10"
        });

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                recalculate();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spinRoot.setOnItemSelectedListener(listener);
        spinBootloader.setOnItemSelectedListener(listener);
        spinVerifiedBoot.setOnItemSelectedListener(listener);
        spinShizuku.setOnItemSelectedListener(listener);

        swDmVerity.setOnCheckedChangeListener((btn, checked) -> recalculate());

        Button btnReset = findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(v -> reset());

        recalculate();
    }

    private void reset() {
        spinRoot.setSelection(ROOT_DENIED);
        spinBootloader.setSelection(BL_LOCKED);
        spinVerifiedBoot.setSelection(VB_GREEN);
        spinShizuku.setSelection(SH_NONE);
        swDmVerity.setChecked(true);
    }

    private void recalculate() {
        int score = 100;
        StringBuilder sb = new StringBuilder();

        // Root
        int rootIdx = spinRoot.getSelectedItemPosition();
        int rootDeduct = 0;
        if (rootIdx == ROOT_GRANTED) { rootDeduct = 30; score -= 30; }
        else if (rootIdx == ROOT_UNKNOWN) { rootDeduct = 5; score -= 5; }
        sb.append(deductLine("root", rootDeduct));
        setBadge(rootBadge, rootDeduct);

        // Bootloader
        int blIdx = spinBootloader.getSelectedItemPosition();
        int blDeduct = 0;
        if (blIdx == BL_UNLOCKED) { blDeduct = 35; score -= 35; }
        else if (blIdx == BL_UNKNOWN) { blDeduct = 5; score -= 5; }
        sb.append(deductLine("bootloader", blDeduct));
        setBadge(blBadge, blDeduct);

        // Verified boot
        int vbIdx = spinVerifiedBoot.getSelectedItemPosition();
        int vbDeduct = 0;
        switch (vbIdx) {
            case VB_YELLOW:  vbDeduct =  5; break;
            case VB_ORANGE:  vbDeduct = 10; break;
            case VB_RED:     vbDeduct = 15; break;
            case VB_UNKNOWN: vbDeduct =  5; break;
        }
        score -= vbDeduct;
        sb.append(deductLine("verified boot", vbDeduct));
        setBadge(vbBadge, vbDeduct);

        // dm-verity
        boolean verityOk = swDmVerity.isChecked();
        int verityDeduct = verityOk ? 0 : 5;
        score -= verityDeduct;
        if (verityDeduct > 0) sb.append(deductLine("dm-verity disabled", verityDeduct));
        verityHint.setText(verityOk ? "enabled  ±0" : "disabled −5");
        verityHint.setTextColor(verityOk
                ? Color.parseColor("#3D6B3D")
                : Color.parseColor("#FF3B3B"));

        // Shizuku
        int shIdx = spinShizuku.getSelectedItemPosition();
        int shDeduct = (shIdx == SH_PERMITTED) ? 10 : 0;
        score -= shDeduct;
        sb.append(deductLine("shizuku", shDeduct));
        setBadge(shBadge, shDeduct);

        score = Math.max(0, score);

        // Display
        int color = scoreColor(score);
        scoreText.setText(score + "/100");
        scoreText.setTextColor(color);

        ObjectAnimator anim = ObjectAnimator.ofInt(scoreBar, "progress", scoreBar.getProgress(), score);
        anim.setDuration(400);
        anim.start();
        scoreBar.setProgressTintList(ColorStateList.valueOf(color));

        // Trim trailing newline
        String breakdown = sb.toString();
        if (breakdown.endsWith("\n")) breakdown = breakdown.substring(0, breakdown.length() - 1);
        breakdownText.setText(breakdown);
    }

    private String deductLine(String label, int deduct) {
        String padded = String.format("%-22s", label);
        return padded + (deduct == 0 ? "  ±0" : String.format(" −%d", deduct)) + "\n";
    }

    private void setBadge(TextView badge, int deduct) {
        if (deduct == 0) {
            badge.setText("±0");
            badge.setTextColor(Color.parseColor("#00E676"));
        } else {
            badge.setText("−" + deduct);
            badge.setTextColor(deduct >= 20
                    ? Color.parseColor("#FF3B3B")
                    : Color.parseColor("#FFD600"));
        }
    }

    private int scoreColor(int score) {
        if (score >= 80) return Color.parseColor("#00E676");
        if (score >= 50) return Color.parseColor("#FFD600");
        return Color.parseColor("#FF3B3B");
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {

            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, convertView, parent);
                styleSpinnerItem(tv);
                return tv;
            }

            @Override
            public View getDropDownView(int pos, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(pos, convertView, parent);
                styleSpinnerItem(tv);
                tv.setBackgroundColor(Color.parseColor("#0F160F"));
                tv.setPadding(32, 20, 32, 20);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void styleSpinnerItem(TextView tv) {
        tv.setTextColor(Color.parseColor("#00FF41"));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(12f);
    }
}
