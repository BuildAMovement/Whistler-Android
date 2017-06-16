package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import rs.readahead.washington.mobile.R;


public class SecureMessagingChatFragment extends Fragment {

    public SecureMessagingChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_secure_messaging_chat, container, false);
    }
}
