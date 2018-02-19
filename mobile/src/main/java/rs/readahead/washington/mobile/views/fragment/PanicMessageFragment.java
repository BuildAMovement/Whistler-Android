package rs.readahead.washington.mobile.views.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.LocationProvider;
import rs.readahead.washington.mobile.util.PermissionUtil;


public class PanicMessageFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.panic_message)
    EditText mPanicMessageView;
    @BindView(R.id.geolocation)
    SwitchCompat mGeolocationView;
    @BindView(R.id.panic_message_layout)
    TextInputLayout mPanicMessageLayout;

    private Unbinder unbinder;

    public PanicMessageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_panic_message, container, false);
        unbinder = ButterKnife.bind(this, view);
        setViews();
        mGeolocationView.setOnCheckedChangeListener(this);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.panic_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            return redirectBack();
        }

        if (id == R.id.action_select) {
            mPanicMessageView.clearFocus();
            SharedPrefs.getInstance().setPanicMessage(mPanicMessageView.getText().toString());
            Toast.makeText(getActivity(), R.string.panic_message_saved, Toast.LENGTH_SHORT).show();
            return redirectBack();
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean redirectBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
            return true;
        }

        return false;
    }

    private void setViews() {
        mPanicMessageView.setText(SharedPrefs.getInstance().getPanicMessage());
        mGeolocationView.setChecked(SharedPrefs.getInstance().isPanicGeolocationActive());
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        if (v.getId() == R.id.geolocation) {
            if (isChecked) {
                if (PermissionUtil.checkPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION, getString(R.string.permission_location))) {
                    if (!LocationProvider.isLocationEnabled(getContext())) {
                        LocationProvider.openSettings(getContext());
                        return;
                    }
                    SharedPrefs.getInstance().setPanicGeolocationActive(isChecked);
                    mGeolocationView.setChecked(isChecked);
                }
            } else {
                SharedPrefs.getInstance().setPanicGeolocationActive(isChecked);
                mGeolocationView.setChecked(isChecked);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
