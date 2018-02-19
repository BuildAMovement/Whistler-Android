package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;


public class CameraPreviewAnonymousButton extends AppCompatImageButton {
    private CompositeDisposable disposable;

    public CameraPreviewAnonymousButton(Context context) {
        this(context, null);
    }

    public CameraPreviewAnonymousButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewAnonymousButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        disposable = new CompositeDisposable();

        disposable.add(Single.fromCallable(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return SharedPrefs.getInstance().isAnonymousMode();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        displayDrawable(aBoolean);
                    }
                })
        );

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                disposable.add(Single.fromCallable(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                boolean next = !SharedPrefs.getInstance().isAnonymousMode();
                                SharedPrefs.getInstance().setAnonymousMode(next);
                                return next;
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                displayDrawable(aBoolean);
                            }
                        })
                );
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void displayDrawable(boolean isAnonymous) {
        if (isAnonymous) {
            displayDisable();
        } else {
            displayEnable();
        }
    }

    private void displayEnable() {
        setImageResource(R.drawable.ic_location_searching_white);
    }

    private void displayDisable() {
        setImageResource(R.drawable.ic_location_disabled_white);
    }
}
