package rs.readahead.washington.mobile.views.fragment;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;


public class EraseSensitiveDataFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.erase_everything)
    SwitchCompat eraseEverything;
    @BindView(R.id.erase_gallery)
    SwitchCompat mEraseGallery;
    @BindView(R.id.erase_contacts)
    SwitchCompat mEraseContacts;
    @BindView(R.id.erase_media_recipients)
    SwitchCompat mEraseMediaRecipients;
    @BindView(R.id.erase_materials)
    SwitchCompat mEraseTrainingMaterials;
    @BindView(R.id.erase_reports)
    SwitchCompat eraseReports;
    @BindView(R.id.erase_forms)
    SwitchCompat eraseForms;

    private Unbinder unbinder;


    public EraseSensitiveDataFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_erase_sensitiv_data, container, false);
        unbinder = ButterKnife.bind(this, view);

        setViews();

        eraseEverything.setOnCheckedChangeListener(this);
        mEraseGallery.setOnCheckedChangeListener(this);
        mEraseContacts.setOnCheckedChangeListener(this);
        mEraseMediaRecipients.setOnCheckedChangeListener(this);
        mEraseTrainingMaterials.setOnCheckedChangeListener(this);
        eraseReports.setOnCheckedChangeListener(this);
        eraseForms.setOnCheckedChangeListener(this);

        return view;
    }

    private void setViews() {
        eraseEverything.setChecked(Preferences.isEraseEverything());
        updateOthers(Preferences.isEraseEverything());
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        SharedPrefs sp = SharedPrefs.getInstance();

        switch (v.getId()) {
            case R.id.erase_everything:
                eraseEverything.setChecked(isChecked);
                Preferences.setEraseEverything(isChecked);
                updateOthers(isChecked);
                break;
            case R.id.erase_gallery:
                mEraseGallery.setChecked(isChecked);
                sp.setEraseGalleryActive(isChecked);
                break;
            case R.id.erase_contacts:
                mEraseContacts.setChecked(isChecked);
                sp.setEraseContactsActive(isChecked);
                break;
            case R.id.erase_media_recipients:
                mEraseMediaRecipients.setChecked(isChecked);
                sp.setEraseMediaRecipientsActive(isChecked);
                break;
            case R.id.erase_materials:
                mEraseTrainingMaterials.setChecked(isChecked);
                sp.setEraseMaterialsActive(isChecked);
                break;
            case R.id.erase_reports:
                eraseReports.setChecked(isChecked);
                Preferences.setEraseReports(isChecked);
                break;
            case R.id.erase_forms:
                eraseForms.setChecked(isChecked);
                Preferences.setEraseForms(isChecked);
                break;
        }
    }

    private void updateOthers(boolean eraseEverything) {
        SharedPrefs sp = SharedPrefs.getInstance();

        mEraseGallery.setChecked(eraseEverything || sp.isEraseGalleryActive());
        mEraseGallery.setEnabled(!eraseEverything);
        sp.setEraseGalleryActive(eraseEverything || sp.isEraseGalleryActive());

        mEraseContacts.setChecked(eraseEverything || sp.isEraseContactsActive());
        mEraseContacts.setEnabled(!eraseEverything);
        sp.setEraseContactsActive(eraseEverything || sp.isEraseContactsActive());

        mEraseMediaRecipients.setChecked(eraseEverything || sp.isEraseMediaRecipientsActive());
        mEraseMediaRecipients.setEnabled(!eraseEverything);
        sp.setEraseMediaRecipientsActive(eraseEverything || sp.isEraseMediaRecipientsActive());

        mEraseTrainingMaterials.setChecked(eraseEverything || sp.isEraseMaterialsActive());
        mEraseTrainingMaterials.setEnabled(!eraseEverything);
        sp.setEraseMaterialsActive(eraseEverything || sp.isEraseMaterialsActive());

        eraseReports.setChecked(eraseEverything || Preferences.isEraseReports());
        eraseReports.setEnabled(!eraseEverything);
        Preferences.setEraseReports(eraseEverything || Preferences.isEraseReports());

        eraseForms.setChecked(eraseEverything || Preferences.isEraseForms());
        eraseForms.setEnabled(!eraseEverything);
        Preferences.setEraseForms(eraseEverything || Preferences.isEraseForms());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
