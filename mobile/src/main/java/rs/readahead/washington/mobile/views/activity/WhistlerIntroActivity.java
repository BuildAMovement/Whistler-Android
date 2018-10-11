package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.ISlidePolicy;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.PermissionUtil;


@RuntimePermissions
public class WhistlerIntroActivity extends AppIntro implements ICacheWordSubscriber {
    public static final String FROM_ABOUT = "from_about";

    private CacheWordHandler cacheWordHandler;
    private boolean policyRespected = false;
    private AlertDialog alertDialog;
    private boolean fromAbout;
    private boolean permissionSlideEnabled;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(base));
    }

        @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cacheWordHandler = new CacheWordHandler(this);

        fromAbout = getIntent().getBooleanExtra(FROM_ABOUT, false);
        //permissionSlideEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        //        (!fromAbout || hasMissingPermission());
        // we have disabled above as there is no way to know if user selected
        // "do not ask again", in which case permission request will fail
        permissionSlideEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !fromAbout;

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.app_name,
                R.drawable.whistler_logo,
                R.string.ra_intro_title1,
                R.string.ra_intro_text1,
                R.string.ra_intro_link1,
                AboutHelpActivity.class
        )));

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.camouflage,
                R.drawable.ic_lock_white_24dp,
                R.string.ra_intro_title2,
                R.string.ra_intro_text2,
                R.string.ra_intro_link2,
                CamouflageSettingsActivity.class
        )));

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.ra_report,
                R.drawable.ic_report_white,
                R.string.ra_intro_title3,
                R.string.ra_intro_text3,
                R.string.ra_intro_link3,
                ReportSettingsActivity.class
        )));

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.ra_collect,
                R.drawable.main_collect,
                R.string.ra_intro_title4,
                R.string.ra_intro_text4,
                R.string.ra_intro_link4,
                CollectManageServersActivity.class
        )));

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.ra_panic,
                R.drawable.ic_report_problem_white_24dp,
                R.string.ra_intro_title5,
                R.string.ra_intro_text5,
                R.string.ra_intro_link5,
                PanicModeSettingsActivity.class
        )));

        if (permissionSlideEnabled) {
            addSlide(PermissionFragment.newInstance());
        }

        showStatusBar(true);
        showSkipButton(true);
        setSkipText(getString(R.string.skip));
        setSeparatorColor(getResources().getColor(R.color.wa_gray));

        if (!fromAbout) {
            setDoneText(getString(R.string.ra_start));
        }
    }

    @Override
    public void onSkipPressed(@Nullable Fragment fragment) {
        if (permissionSlideEnabled) {
            pager.setCurrentItem(getSlides().size() - 1);
        } else {
            closeWhistlerIntro();
        }
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
    }

    @Override
    public void onDonePressed(@Nullable Fragment fragment) {
        askPermissions(); // BAM wants permissions to be asked until all are accepted
    }

    @Override
    public void onBackPressed() {
        boolean closeApp = pager.isFirstSlide(fragments.size());

        super.onBackPressed();

        if (!fromAbout && closeApp) {
            finish();
            cacheWordHandler.lock();
        }
    }

    public void askPermissions() {
        WhistlerIntroActivityPermissionsDispatcher.askPermissionsImplWithCheck(this);
    }

    @NeedsPermission({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    public void askPermissionsImpl() {
        closeWhistlerIntro();
    }

    @OnShowRationale({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    void showPermissionsRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(
                this, request, getString(R.string.ra_intro_permission_rationale));
    }

    @OnPermissionDenied({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    void onPermissionsDenied() {
        // policyRespected = true;
    }

    @OnNeverAskAgain({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    void onPermissionsNeverAskAgain() {
        closeWhistlerIntro();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        WhistlerIntroActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cacheWordHandler.connectToService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cacheWordHandler.disconnectFromService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    @Override
    public void onCacheWordUninitialized() {
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        MyApplication.startLockScreenActivity(this);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
    }

    private void closeWhistlerIntro() {
        if (!fromAbout) {
            Preferences.setFirstStart(false);
            MyApplication.startMainActivity(this);
        }

        finish();
    }

    /*private boolean hasMissingPermission() {
        String permissions[] = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
        };

        for (String permission: permissions) {
            if (!PermissionUtil.checkPermission(this, permission)) {
                return true;
            }
        }

        return false;
    }*/

    public static class IntroFragment extends Fragment {
        private static final String ARG_HEADER_RES_ID = "headerResId";
        private static final String ARG_IMAGE_RES_ID = "imageResId";
        private static final String ARG_TITLE_RES_ID = "titleResId";
        private static final String ARG_TEXT_RES_ID = "textResId";
        private static final String ARG_LINK_RES_ID = "linkResId";
        private static final String ARG_LINK_ACTIVITY_CLASS = "linkActivityClass";

        private int imageResId;
        private int headerResId, titleResId, textResId, linkResId;
        private Class linkActivityClass;

        public static IntroFragment newInstance(IntroPage introPage) {
            IntroFragment slide = new IntroFragment();

            Bundle args = new Bundle();
            args.putInt(ARG_HEADER_RES_ID, introPage.headerResId);
            args.putInt(ARG_IMAGE_RES_ID, introPage.imageResId);
            args.putInt(ARG_TITLE_RES_ID, introPage.titleResId);
            args.putInt(ARG_TEXT_RES_ID, introPage.textResId);
            args.putInt(ARG_LINK_RES_ID, introPage.linkResId);
            args.putSerializable(ARG_LINK_ACTIVITY_CLASS, introPage.linkActivityClass);
            slide.setArguments(args);

            return slide;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            if (getArguments() != null && getArguments().size() != 0) {
                imageResId = getArguments().getInt(ARG_IMAGE_RES_ID);
                headerResId = getArguments().getInt(ARG_HEADER_RES_ID);
                titleResId = getArguments().getInt(ARG_TITLE_RES_ID);
                textResId = getArguments().getInt(ARG_TEXT_RES_ID);
                linkResId = getArguments().getInt(ARG_LINK_RES_ID);
                linkActivityClass = (Class) getArguments().getSerializable(ARG_LINK_ACTIVITY_CLASS);
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if (savedInstanceState != null) {
                imageResId = savedInstanceState.getInt(ARG_IMAGE_RES_ID);
                headerResId = savedInstanceState.getInt(ARG_HEADER_RES_ID);
                titleResId = savedInstanceState.getInt(ARG_TITLE_RES_ID);
                textResId = savedInstanceState.getInt(ARG_TEXT_RES_ID);
                linkResId = savedInstanceState.getInt(ARG_LINK_RES_ID);
                linkActivityClass = (Class) savedInstanceState.getSerializable(ARG_LINK_ACTIVITY_CLASS);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_whistler_intro, container, false);

            TextView he = view.findViewById(R.id.header);
            TextView ti = view.findViewById(R.id.title);
            TextView te = view.findViewById(R.id.text);
            TextView li = view.findViewById(R.id.link);
            ImageView im = view.findViewById(R.id.image);

            he.setText(getString(headerResId));
            ti.setText(getString(titleResId));
            te.setText(getString(textResId));

            SpannableString content = new SpannableString(getString(linkResId));
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            li.setText(content);
            li.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), linkActivityClass);
                    startActivity(intent);
                }
            });

            im.setImageResource(imageResId);

            return view;
        }
    }

    public static class PermissionFragment extends Fragment implements ISlidePolicy {
        public static PermissionFragment newInstance() {
            return new PermissionFragment();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_whistler_intro_permissions, container, false);

            TextView li = view.findViewById(R.id.link);

            SpannableString content =  new SpannableString(getString(R.string.ra_grant_permissions));
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            li.setText(content);
            li.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    askPermissions();
                }
            });

            return view;
        }

        @Override
        public boolean isPolicyRespected() {
            WhistlerIntroActivity activity = (WhistlerIntroActivity) getActivity();
            return activity == null || activity.policyRespected;
        }

        @Override
        public void onUserIllegallyRequestedNextPage() {
            askPermissions();
        }

        private void askPermissions() {
            WhistlerIntroActivity activity = (WhistlerIntroActivity) getActivity();
            if (activity != null) {
                activity.askPermissions();
            }
        }
    }

    private static class IntroPage {
        @StringRes
        int headerResId;
        @DrawableRes
        int imageResId;
        @StringRes
        int titleResId;
        @StringRes
        int textResId;
        @StringRes
        int linkResId;
        Class linkActivityClass;

        IntroPage(int headerResId, int imageResId, int titleResId, int textResId, int linkResId, Class linkActivityClass) {
            this.headerResId = headerResId;
            this.imageResId = imageResId;
            this.titleResId = titleResId;
            this.textResId = textResId;
            this.linkResId = linkResId;
            this.linkActivityClass = linkActivityClass;
        }
    }
}
