package rs.readahead.washington.mobile.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import rs.readahead.washington.mobile.R;


public class LockScreenActivity extends AppCompatActivity implements ICacheWordSubscriber {
    @BindView(R.id.button_create)
    Button mBtnCreate;

    private CacheWordHandler mCacheWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCacheWord = new CacheWordHandler(this);
    }

    @Override
    public void onCacheWordUninitialized() {
        mBtnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LockScreenActivity.this, NewPatternActivity.class));
                finish();
            }
        });
    }

    @Override
    public void onCacheWordLocked() {
        startActivity(new Intent(this, SecurityActivity.class));
        finish();
    }

    @Override
    public void onCacheWordOpened() {
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
