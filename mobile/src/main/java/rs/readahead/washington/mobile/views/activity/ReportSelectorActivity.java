package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.PermissionHandler;


@RuntimePermissions
public class ReportSelectorActivity extends MetadataActivity {
    private AlertDialog rationaleDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report_selector);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        if (rationaleDialog != null && rationaleDialog.isShowing()) {
            rationaleDialog.dismiss();
        }

        super.onStop();
    }

    @OnClick({R.id.draft_reports, R.id.new_report, R.id.archived_reports})
    public void startActivity(View view) {
        switch (view.getId()) {
            case R.id.new_report:
                ReportSelectorActivityPermissionsDispatcher.startNewReportActivityWithCheck(this);
                break;

            case R.id.draft_reports:
                startActivity(new Intent(ReportSelectorActivity.this, DraftReportsActivity.class));
                break;

            case R.id.archived_reports:
                startActivity(new Intent(ReportSelectorActivity.this, ArchiveReportsActivity.class));
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ReportSelectorActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startNewReportActivity() {
        startActivity(new Intent(ReportSelectorActivity.this, NewReportActivity.class));
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        rationaleDialog = PermissionHandler.showRationale(this, request, getString(R.string.permission_location_metadata));
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
        startNewReportActivity();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        startNewReportActivity();
    }
}
