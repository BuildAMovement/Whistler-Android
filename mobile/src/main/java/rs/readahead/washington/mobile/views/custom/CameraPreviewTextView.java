package rs.readahead.washington.mobile.views.custom;

import android.Manifest;
import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.PermissionUtil;


public class CameraPreviewTextView extends AppCompatTextView {
    public CameraPreviewTextView(Context context) {
        this(context, null);
    }

    public CameraPreviewTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (PermissionUtil.checkPermission(getContext(), Manifest.permission.CAMERA)) {
            setText(R.string.ra_capture_verifiable_photo_video);
        } else {
            setText(R.string.ra_enable_camera_preview);
        }
    }
}
