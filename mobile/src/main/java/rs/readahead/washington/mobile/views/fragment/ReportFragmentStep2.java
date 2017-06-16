package rs.readahead.washington.mobile.views.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.models.Report;
import rs.readahead.washington.mobile.util.DateUtil;
import rs.readahead.washington.mobile.views.activity.NewReportActivity;


public class ReportFragmentStep2 extends Fragment implements View.OnTouchListener {
    @BindView(R.id.additional_information_edit) EditText mAdditionalInformation;
    @BindView(R.id.metadata_report) CheckedTextView mMetadataReport;
    @BindView(R.id.pick_date) RadioButton mDateSelect;
    @BindView(R.id.today) RadioButton mToday;
    @BindView(R.id.yesterday) RadioButton mYesterday;
    @BindView(R.id.title_edit) EditText mTitle;
    @BindView(R.id.location_edit) EditText mLocation;
    @BindView(R.id.dates_selector) RadioGroup mDatesGroup;

    private Unbinder unbinder;
    private Report mReport;
    private Calendar mCalendar;
    private TitleTextWatcher mTitleTextWatcher;
    private ContentTextWatcher mContentTextWatcher;
    private LocationTextWatcher mLocationTextWatcher;


    public ReportFragmentStep2() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_step2, container, false);
        unbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mReport = ((NewReportActivity) getActivity()).getReport();
        setViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        addTextWatchers();
    }

    @Override
    public void onStop() {
        removeTextWatchers();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    private void setViews() {
        mAdditionalInformation.setText(mReport.getContent());
        mMetadataReport.setChecked(mReport.isMetadataSelected());
        mLocation.setText(mReport.getLocation());
        mTitle.setText(mReport.getTitle());
        setDate();
    }

    private void setDate() {
        final Date dateOfReport = mReport.getDate();

        if (dateOfReport != null) {
            if (DateUtils.isToday(dateOfReport.getTime())) {
                mToday.post(new Runnable() {
                    @Override
                    public void run() {
                        mDatesGroup.check(R.id.today);
                    }
                });
            } else {
                if (DateUtil.isYesterday(dateOfReport)) {
                    mYesterday.post(new Runnable() {
                        @Override
                        public void run() {
                            mDatesGroup.check(R.id.yesterday);
                        }
                    });
                } else {
                    mDateSelect.post(new Runnable() {
                        @Override
                        public void run() {
                            mDatesGroup.check(R.id.pick_date);
                            mDateSelect.setText(DateUtil.getStringFromDate(dateOfReport));
                        }
                    });

                }
            }
        } else {
            mReport.setDate(DateUtil.getCurrentDate());
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.additional_information_edit) {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
        }
        return false;
    }

    private void addTextWatchers() {
        mTitle.addTextChangedListener(mTitleTextWatcher = new TitleTextWatcher());
        mAdditionalInformation.addTextChangedListener(mContentTextWatcher = new ContentTextWatcher());
        mLocation.addTextChangedListener(mLocationTextWatcher = new LocationTextWatcher());
    }

    private void removeTextWatchers() {
        mTitle.removeTextChangedListener(mTitleTextWatcher);
        mAdditionalInformation.removeTextChangedListener(mContentTextWatcher);
        mLocation.removeTextChangedListener(mLocationTextWatcher);

        mTitleTextWatcher = null;
        mContentTextWatcher = null;
        mLocationTextWatcher = null;
    }

    private class TitleTextWatcher extends OnTextChangedWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mReport.setTitle(s.toString());
        }
    }

    private class ContentTextWatcher extends OnTextChangedWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mReport.setContent(s.toString());
        }
    }

    private class LocationTextWatcher extends OnTextChangedWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mReport.setLocation(s.toString());
        }
    }

    @OnClick({R.id.pick_date, R.id.today, R.id.yesterday})
    public void startActivity(View view) {
        switch (view.getId()) {
            case R.id.pick_date:
                showCalendarDialog();
                break;

            case R.id.today:
                mReport.setDate(DateUtil.getCurrentDate());
                break;

            case R.id.yesterday:
                mReport.setDate(DateUtil.getYesterdaysDate());
                break;
        }
    }

    private void showCalendarDialog() {
        mCalendar = Calendar.getInstance();
        DatePickerDialog mDatePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar = Calendar.getInstance();
                mCalendar.set(year, monthOfYear, dayOfMonth);
                mDateSelect.setText(DateUtil.getStringFromDate(mCalendar.getTime()));
                mReport.setDate(mCalendar.getTime());
            }
        }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        mDatePickerDialog.getDatePicker().setMaxDate(mCalendar.getTimeInMillis());
        mDatePickerDialog.show();
    }

    @OnClick({R.id.metadata_report})
    public void handleClick(View view) {
        switch (view.getId()) {
            case R.id.metadata_report:
                CheckedTextView checkedTextView = (CheckedTextView) view;
                if (checkedTextView.isChecked()) {
                    mReport.setMetadataSelected(false);
                    mMetadataReport.setChecked(false);
                } else {
                    mReport.setMetadataSelected(true);
                    mMetadataReport.setChecked(true);
                }
                break;
        }
    }

    private abstract static class OnTextChangedWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
