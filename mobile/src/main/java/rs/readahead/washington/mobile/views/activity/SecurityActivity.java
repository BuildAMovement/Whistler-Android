package rs.readahead.washington.mobile.views.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.security.GeneralSecurityException;
import java.util.List;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import me.zhanghai.android.patternlock.ConfirmPatternActivity;
import me.zhanghai.android.patternlock.PatternUtils;
import me.zhanghai.android.patternlock.PatternView;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.DialogsUtil;
import timber.log.Timber;


public class SecurityActivity extends ConfirmPatternActivity implements ICacheWordSubscriber {

    private CacheWordHandler mCacheWord;
    private Context context = SecurityActivity.this;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCacheWord = new CacheWordHandler(this);
        Button button = (Button) findViewById(R.id.pl_right_button);
        button.setVisibility(View.INVISIBLE);

    }

    @Override
    protected boolean isPatternCorrect(List<PatternView.Cell> pattern) {
        dialog = DialogsUtil.showProgressDialog(context);

        try {
            mCacheWord.setPassphrase(PatternUtils.patternToSha1String(pattern).toCharArray());
            return true;
        } catch (final GeneralSecurityException e) {
            Timber.d(e, getClass().getName()); // todo: exc once got here on app start..
            dismissDialog();
            return false;
        }
    }



    @Override
    public void onCacheWordUninitialized() {

    }

    @Override
    public void onCacheWordLocked() {

    }

    @Override
    public void onCacheWordOpened() {
        dismissDialog();
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
        dismissDialog();
        mCacheWord.disconnectFromService();
    }

    private void dismissDialog(){
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
