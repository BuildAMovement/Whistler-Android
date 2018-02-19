package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;


public class CollectServersAdapter extends ArrayAdapter<CollectServer> {
    private ICollectServersAdapterHandler handler;

    public interface ICollectServersAdapterHandler {
        void onCollectServersAdapterEdit(CollectServer server);
        void onCollectServersAdapterRemove(CollectServer server);
    }

    public CollectServersAdapter(
            @NonNull Context context,
            ICollectServersAdapterHandler handler,
            List<CollectServer> items) {
        super(context, 0, items);
        this.handler = handler;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final CollectServer collectServer = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.collect_server_row_for_list, parent, false);
        }

        TextView titleView = ButterKnife.findById(convertView, R.id.server_title);
        ImageView edit = ButterKnife.findById(convertView, R.id.edit);
        ImageView remove = ButterKnife.findById(convertView, R.id.delete);

        if (collectServer != null) {
            titleView.setText(collectServer.getName());

            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handler.onCollectServersAdapterRemove(collectServer);
                }
            });

            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handler.onCollectServersAdapterEdit(collectServer);
                }
            });
        }

        return convertView;
    }
}
