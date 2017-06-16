package rs.readahead.washington.mobile.views.fragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.views.activity.ISecureMessagingView;


public class SecureMessagingContactListFragment extends Fragment {

    @BindView(R.id.secure_messaging_contact_list_mock)
    ImageView image;

    private Unbinder unbinder;
    private ISecureMessagingView parent;


    public SecureMessagingContactListFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        parent = (ISecureMessagingView) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_secure_messaging_contact_list, container, false);
        unbinder = ButterKnife.bind(this, view);

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showChat();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
