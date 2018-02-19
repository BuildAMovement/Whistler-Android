package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.util.List;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import me.zhanghai.android.patternlock.PatternUtils;
import me.zhanghai.android.patternlock.PatternView;
import me.zhanghai.android.patternlock.SetPatternActivity;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.DialogsUtil;


public class NewPatternActivity extends SetPatternActivity implements ICacheWordSubscriber {

    private CacheWordHandler mCacheWord;
    private Context context = NewPatternActivity.this;
    private ProgressDialog dialog;
    private String mNewPassphrase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCacheWord = new CacheWordHandler(this);
    }

    @Override
    protected void onCanceled() {
        super.onCanceled();
        finish();
    }

    @Override
    protected int getMinPatternSize() {
        return 6;
    }

    @Override
    protected void onSetPattern(List<PatternView.Cell> pattern) {
        mNewPassphrase = PatternUtils.patternToSha1String(pattern);
    }

    @Override
    protected void onConfirmed() {
        dialog = DialogsUtil.showProgressDialog(context, getString(R.string.setting_data));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mCacheWord.setPassphrase(mNewPassphrase.toCharArray());
                } catch (GeneralSecurityException e) {
                    Log.e("", "CacheWord pass initialization failed: " + e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void onCacheWordUninitialized() {

    }

    @Override
    public void onCacheWordLocked() {

    }

    @Override
    public void onCacheWordOpened() {
        if (dialog != null) {
            dialog.dismiss();
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
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

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }
}
