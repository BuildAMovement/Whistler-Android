package rs.readahead.washington.mobile.views.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.sharedpref.SharedPrefs;


public class EraseSensitiveDataFragment extends Fragment {

    @BindView(R.id.erase_database) CheckedTextView mEraseDatabase;
    @BindView(R.id.erase_contacts) CheckedTextView mEraseContacts;
    @BindView(R.id.erase_media_recipients) CheckedTextView mEraseMediaRecipients;
    @BindView(R.id.erase_materials) CheckedTextView mEraseTrainingMaterials;
    @BindView(R.id.erase_evidence_videos) CheckedTextView mEraseVideos;
    @BindView(R.id.erase_evidence_audio) CheckedTextView mEraseAudios;
    @BindView(R.id.erase_evidence_image) CheckedTextView mEraseImages;

    private Unbinder unbinder;

    public EraseSensitiveDataFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_erase_sensitiv_data, container, false);
        unbinder = ButterKnife.bind(this, view);
        setViews();
        return view;
    }

    private void setViews() {
        mEraseDatabase.setChecked(SharedPrefs.getInstance().isEraseDatabaseActive());
        mEraseContacts.setChecked(SharedPrefs.getInstance().isEraseContactsActive());
        mEraseMediaRecipients.setChecked(SharedPrefs.getInstance().isEraseMediaActive());
        mEraseTrainingMaterials.setChecked(SharedPrefs.getInstance().isEraseMaterialsActive());
        mEraseVideos.setChecked(SharedPrefs.getInstance().isEraseVideosActive());
        mEraseAudios.setChecked(SharedPrefs.getInstance().isEraseAudiosActive());
        mEraseImages.setChecked(SharedPrefs.getInstance().isErasePhotosActive());
    }

    @OnClick({R.id.erase_database, R.id.erase_contacts, R.id.erase_media_recipients, R.id.erase_materials,
            R.id.erase_evidence_videos, R.id.erase_evidence_audio, R.id.erase_evidence_image})
    public void manage(View view) {

        CheckedTextView checkedTextView = (CheckedTextView) view;
        boolean isChecked = checkedTextView.isChecked();

        switch (view.getId()) {
            case R.id.erase_database:
                mEraseDatabase.setChecked(!isChecked);
                SharedPrefs.getInstance().setEraseDatabaseActive(!isChecked);
                break;
            case R.id.erase_contacts:
                mEraseContacts.setChecked(!isChecked);
                SharedPrefs.getInstance().setEraseContactsActive(!isChecked);
                break;
            case R.id.erase_media_recipients:
                mEraseMediaRecipients.setChecked(!isChecked);
                SharedPrefs.getInstance().setEraseMediaActive(!isChecked);
                break;
            case R.id.erase_materials:
                mEraseTrainingMaterials.setChecked(!isChecked);
                SharedPrefs.getInstance().setEraseMaterialsActive(!isChecked);
                break;
            case R.id.erase_evidence_videos:
                mEraseVideos.setChecked(!isChecked);
                SharedPrefs.getInstance().setEraseVideosActive(!isChecked);
                break;
            case R.id.erase_evidence_audio:
                mEraseAudios.setChecked(!isChecked);
                SharedPrefs.getInstance().setEraseAudiosActive(!isChecked);
                break;
            case R.id.erase_evidence_image:
                mEraseImages.setChecked(!isChecked);
                SharedPrefs.getInstance().setErasePhotosActive(!isChecked);
                break;
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

}
