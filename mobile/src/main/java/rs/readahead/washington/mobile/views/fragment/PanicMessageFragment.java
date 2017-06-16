package rs.readahead.washington.mobile.views.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.LocationProvider;
import rs.readahead.washington.mobile.util.PermissionHandler;

public class PanicMessageFragment extends Fragment {

    @BindView(R.id.panic_message) EditText mPanicMessageView;
    @BindView(R.id.geolocation) CheckedTextView mGeolocationView;
    @BindView(R.id.panic_message_layout) TextInputLayout mPanicMessageLayout;

    private Unbinder unbinder;

    public PanicMessageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_panic_message, container, false);
        unbinder = ButterKnife.bind(this, view);
        setViews();
        return view;
    }

    private void setViews() {
        mPanicMessageView.setText(SharedPrefs.getInstance().getPanicMessage());
        mGeolocationView.setChecked(SharedPrefs.getInstance().isPanicGeolocationActive());
    }

    @OnClick(R.id.panic_ok)
    public void onOkClicked() {
        mPanicMessageView.clearFocus();
        SharedPrefs.getInstance().setPanicMessage(mPanicMessageView.getText().toString());
        Snackbar.make(mGeolocationView, R.string.panic_message_saved, Snackbar.LENGTH_SHORT).show();
    }

    @OnClick(R.id.panic_cancel)
    public void onCancelClicked() {
        getActivity().onBackPressed();
    }


    @OnClick(R.id.geolocation)
    public void handleClick(View view) {

        CheckedTextView checkedTextView = (CheckedTextView) view;
        if (checkedTextView.isChecked()) {
            SharedPrefs.getInstance().setPanicGeolocationActive(false);
            mGeolocationView.setChecked(false);
        } else {
            if (PermissionHandler.checkPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION, getString(R.string.permission_location))) {
                if (!LocationProvider.isLocationEnabled(getContext())) {
                    LocationProvider.openSettings(getContext());
                    return;
                }

                SharedPrefs.getInstance().setPanicGeolocationActive(true);
                mGeolocationView.setChecked(true);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case C.REQUEST_CODE_ASK_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        mGeolocationView.performClick();
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }


}
