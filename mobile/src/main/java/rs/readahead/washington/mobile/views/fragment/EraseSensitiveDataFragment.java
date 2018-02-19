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
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;


public class EraseSensitiveDataFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.erase_database)
    SwitchCompat mEraseDatabase;
    @BindView(R.id.erase_gallery)
    SwitchCompat mEraseGallery;
    @BindView(R.id.erase_contacts)
    SwitchCompat mEraseContacts;
    @BindView(R.id.erase_media_recipients)
    SwitchCompat mEraseMediaRecipients;
    @BindView(R.id.erase_materials)
    SwitchCompat mEraseTrainingMaterials;

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

        mEraseDatabase.setOnCheckedChangeListener(this);
        mEraseGallery.setOnCheckedChangeListener(this);
        mEraseContacts.setOnCheckedChangeListener(this);
        mEraseMediaRecipients.setOnCheckedChangeListener(this);
        mEraseTrainingMaterials.setOnCheckedChangeListener(this);

        return view;
    }

    private void setViews() {
        SharedPrefs sp = SharedPrefs.getInstance();

        mEraseDatabase.setChecked(sp.isEraseDatabaseActive());
        mEraseGallery.setChecked(sp.isEraseGalleryActive());
        mEraseContacts.setChecked(sp.isEraseContactsActive());
        mEraseMediaRecipients.setChecked(sp.isEraseMediaRecipientsActive());
        mEraseTrainingMaterials.setChecked(sp.isEraseMaterialsActive());
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        SharedPrefs sp = SharedPrefs.getInstance();

        switch (v.getId()) {
            case R.id.erase_database:
                mEraseDatabase.setChecked(isChecked);
                sp.setEraseDatabaseActive(isChecked);
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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
