package rs.readahead.washington.mobile.views.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.util.OnClearFromRecentService;

public class SplashActivity extends Activity implements ICacheWordSubscriber {

    private Context context = SplashActivity.this;
    private  Handler handler;
    private CacheWordHandler mCacheWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        handler = new Handler();
        mCacheWord = new CacheWordHandler(context);
        startService(new Intent(getBaseContext(), OnClearFromRecentService.class));

    }

    @Override
    public void onCacheWordUninitialized() {
        MyApplication.showLockScreen(context);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.showLockScreen(context);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(context, MainActivity.class));
                finish();
            }
        }, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCacheWord.connectToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCacheWord.disconnectFromService();
    }
}
