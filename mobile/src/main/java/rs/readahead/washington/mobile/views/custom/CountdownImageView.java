package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import rs.readahead.washington.mobile.R;


public class CountdownImageView extends ImageView {
    private int currentNumber = -1;
    private TypedArray drawables;


    public CountdownImageView(Context context) {
        super(context);
        init();
    }

    public CountdownImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CountdownImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setCountdownNumber(int number) {
        if (number == currentNumber) {
            return;
        }
        
        setImageDrawable(drawables.getDrawable(currentNumber = number));
    }

    protected void init() {
        if (isInEditMode()) return;

        drawables = getResources().obtainTypedArray(R.array.countdown_array);
    }

}
