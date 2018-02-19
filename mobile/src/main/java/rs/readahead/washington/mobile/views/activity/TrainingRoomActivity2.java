package rs.readahead.washington.mobile.views.activity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.TrainModuleHandler;


public class TrainingRoomActivity2 extends CacheWordSubscriberBaseActivity {
    public static String TRAIN_MODULE_ID = "tmi";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.training_web_view)
    WebView mWebView;
    @BindView(R.id.target_view)
    FrameLayout mTargetView;
    @BindView(R.id.main_content)
    FrameLayout mContentView;

    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private View mCustomView;
    private MyChromeClient mClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_room_activity);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initSetup();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSetup() {
        if (!getIntent().hasExtra(TRAIN_MODULE_ID)) {
            return;
        }

        //noinspection ConstantConditions
        long id = getIntent().getExtras().getLong(TRAIN_MODULE_ID, 0);
        File file = new File(TrainModuleHandler.getModuleDir(this, id), "index.html");

        if (file.exists()) {
            String mUrl = "file://" + file.getAbsolutePath();

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportZoom(false);
            webSettings.setAllowFileAccess(true);
            webSettings.setLoadsImagesAutomatically(true);
            webSettings.setSaveFormData(false);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);

            mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            mWebView.setWebViewClient(new MyWebViewClient());
            mClient = new MyChromeClient();
            mWebView.setWebChromeClient(mClient);
            mWebView.setVerticalScrollBarEnabled(false);
            mWebView.setHorizontalScrollBarEnabled(false);
            mWebView.canGoBack();
            mWebView.loadUrl(mUrl);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // we want everything to be opened in WebView
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }
    }

    private class MyChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            mCustomViewCallback = callback;
            mTargetView.addView(view);
            mCustomView = view;
            mContentView.setVisibility(View.GONE);
            mTargetView.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.GONE);
            mTargetView.bringToFront();
        }

        @Override
        public void onHideCustomView() {
            if (mCustomView == null) {
                return;
            }

            mCustomView.setVisibility(View.GONE);
            mTargetView.removeView(mCustomView);
            mCustomView = null;
            mTargetView.setVisibility(View.GONE);
            mCustomViewCallback.onCustomViewHidden();
            mContentView.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            super.onBackPressed();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            mClient.onHideCustomView();
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        mWebView.clearCache(true);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mWebView.destroy();
        super.onDestroy();
    }
}
