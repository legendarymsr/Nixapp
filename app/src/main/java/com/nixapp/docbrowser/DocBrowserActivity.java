package com.nixapp.docbrowser;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
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

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_OFFLINE = "offline";

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout findBar;
    private EditText findInput;
    private TextView findCount;
    private boolean isOffline;

    private static final String DARK_MODE_JS =
            "javascript:(function(){" +
            "var s=document.createElement('style');" +
            "s.innerHTML='*{background-color:#1a1a1a!important;color:#e0e0e0!important}" +
            "a{color:#7cb3ff!important}" +
            "pre,code,tt{background:#252525!important;color:#c5f0a4!important;border-color:#444!important}" +
            "table{border-color:#444!important}" +
            "th{background:#2a2a2a!important}" +
            "img{opacity:0.85}" +
            "input,select,textarea{background:#2a2a2a!important;color:#e0e0e0!important;border-color:#555!important}';" +
            "document.head.appendChild(s);" +
            "})()";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_browser);

        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        isOffline = getIntent().getBooleanExtra(EXTRA_OFFLINE, false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "Documentation");
        }

        progressBar = findViewById(R.id.progress_bar);
        webView = findViewById(R.id.web_view);
        findBar = findViewById(R.id.find_bar);
        findInput = findViewById(R.id.find_input);
        findCount = findViewById(R.id.find_count);
        ImageButton findPrev = findViewById(R.id.btn_find_prev);
        ImageButton findNext = findViewById(R.id.btn_find_next);
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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFind(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        findPrev.setOnClickListener(v -> webView.findNext(false));
        findNext.setOnClickListener(v -> webView.findNext(true));
        findClose.setOnClickListener(v -> closeFindBar());

        if (url != null) {
            webView.loadUrl(url);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setTextZoom(100);

        if (isOffline) {
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.setForceDarkAllowed(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.setAlgorithmicDarkeningAllowed(true);
        }

        webView.setBackgroundColor(Color.parseColor("#1a1a1a"));

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(title);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isOffline && !url.startsWith("file://")) {
                    Toast.makeText(DocBrowserActivity.this,
                            "Offline mode: cannot open external link", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject dark CSS for sites that don't support dark mode natively
                view.evaluateJavascript(DARK_MODE_JS.replace("javascript:", ""), null);
            }
        });

        webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (isDoneCounting) {
                findCount.setText(numberOfMatches == 0 ? "No results" :
                        (activeMatchOrdinal + 1) + " / " + numberOfMatches);
            }
        });
    }

    private void performFind(String query) {
        if (query.isEmpty()) {
            webView.clearMatches();
            findCount.setText("");
        } else {
            webView.findAllAsync(query);
        }
    }

    private void closeFindBar() {
        findBar.setVisibility(View.GONE);
        webView.clearMatches();
        findInput.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_find) {
            findBar.setVisibility(findBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (findBar.getVisibility() == View.VISIBLE) {
                findInput.requestFocus();
            }
            return true;
        } else if (id == R.id.action_refresh) {
            webView.reload();
            return true;
        } else if (id == R.id.action_back) {
            if (webView.canGoBack()) webView.goBack();
            return true;
        } else if (id == R.id.action_forward) {
            if (webView.canGoForward()) webView.goForward();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (findBar.getVisibility() == View.VISIBLE) {
            closeFindBar();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
