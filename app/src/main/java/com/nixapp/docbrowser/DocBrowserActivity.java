package com.nixapp.docbrowser;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DocBrowserActivity extends AppCompatActivity {

    public static final String EXTRA_URL     = "url";
    public static final String EXTRA_TITLE   = "title";
    public static final String EXTRA_OFFLINE = "offline";

    private WebView      webView;
    private ProgressBar  progressBar;
    private LinearLayout findBar;
    private EditText     findInput;
    private TextView     findCount;
    private boolean      isOffline;

    // Applied via JS after page load for online pages (offline pages already have CSS injected)
    private static final String DARK_JS =
        "(function(){" +
        "if(document.getElementById('nixdoc-dark'))return;" + // already injected offline
        "var s=document.createElement('style');s.id='nixdoc-dark';" +
        "s.textContent='" +
            "html,body,*{background:#111!important;color:#ddd!important;border-color:#333!important}" +
            "a{color:#7cb3ff!important}a:visited{color:#b088ff!important}" +
            "code,pre,tt,.verbatim,.programlisting,.screen{background:#1e1e1e!important;" +
            "color:#c5f0a4!important;border:1px solid #333!important}" +
            "table{border-collapse:collapse}th{background:#222!important}" +
            "img{opacity:.85;filter:brightness(.9)}" +
            "input,select,textarea{background:#1e1e1e!important;color:#ddd!important}" +
            "nav,header,.sidebar,.toc{background:#161616!important}" +
            ".note,.warning,.tip,.caution{background:#1a1a0a!important;" +
            "border-left:4px solid #666!important}" +
        "';" +
        "document.head.appendChild(s);" +
        "})()";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_browser);

        String url   = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        isOffline    = getIntent().getBooleanExtra(EXTRA_OFFLINE, false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "Documentation");
        }

        progressBar = findViewById(R.id.progress_bar);
        webView     = findViewById(R.id.web_view);
        findBar     = findViewById(R.id.find_bar);
        findInput   = findViewById(R.id.find_input);
        findCount   = findViewById(R.id.find_count);

        ImageButton findPrev  = findViewById(R.id.btn_find_prev);
        ImageButton findNext  = findViewById(R.id.btn_find_next);
        ImageButton findClose = findViewById(R.id.btn_find_close);

        setupWebView();

        findInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performFind(findInput.getText().toString());
                return true;
            }
            return false;
        });
        findInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                performFind(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        findPrev.setOnClickListener(v  -> webView.findNext(false));
        findNext.setOnClickListener(v  -> webView.findNext(true));
        findClose.setOnClickListener(v -> closeFindBar());

        if (url != null) webView.loadUrl(url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setTextZoom(100);

        // Offline file access
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            s.setAllowUniversalAccessFromFileURLs(true);
            s.setAllowFileAccessFromFileURLs(true);
        }

        // System dark mode forcing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.setForceDarkAllowed(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            s.setAlgorithmicDarkeningAllowed(true);
        }

        webView.setBackgroundColor(Color.parseColor("#111111"));

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                progressBar.setProgress(p);
                progressBar.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onReceivedTitle(WebView view, String t) {
                if (getSupportActionBar() != null) getSupportActionBar().setSubtitle(t);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (isOffline && !url.startsWith("file://")) {
                    Toast.makeText(DocBrowserActivity.this,
                            "Offline — external links blocked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject dark CSS via JS for online pages; offline pages already have it embedded
                view.evaluateJavascript(DARK_JS, null);
            }
        });

        webView.setFindListener((ordinal, total, done) -> {
            if (done) {
                findCount.setText(total == 0 ? "No results"
                        : (ordinal + 1) + " / " + total);
            }
        });
    }

    private void performFind(String q) {
        if (q.isEmpty()) { webView.clearMatches(); findCount.setText(""); }
        else webView.findAllAsync(q);
    }

    private void closeFindBar() {
        findBar.setVisibility(View.GONE);
        webView.clearMatches();
        findInput.setText("");
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if      (id == android.R.id.home)   { onBackPressed(); return true; }
        else if (id == R.id.action_find)    {
            boolean show = findBar.getVisibility() != View.VISIBLE;
            findBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) findInput.requestFocus();
            return true;
        }
        else if (id == R.id.action_refresh) { webView.reload();             return true; }
        else if (id == R.id.action_back)    { if (webView.canGoBack())  webView.goBack();    return true; }
        else if (id == R.id.action_forward) { if (webView.canGoForward()) webView.goForward(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        if (findBar.getVisibility() == View.VISIBLE)  closeFindBar();
        else if (webView.canGoBack())                 webView.goBack();
        else                                          super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
