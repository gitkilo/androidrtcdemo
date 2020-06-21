package com.kilo.rtcdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class PeerListAdapter extends BaseAdapter {
    private List<Peer> peerList;
    private Context mContext;
    private OnClickListener clickListener;
    public PeerListAdapter(@NonNull Context context, @NonNull OnClickListener listener, @NonNull List<Peer> data)
    {
        clickListener = listener;
        mContext = context;
        peerList = data;
    }
    @Override
    public int getCount() {
        return peerList.size();
    }

    @Override
    public Peer getItem(int position) {
        return peerList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null)
        {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
            viewHolder.tvId = convertView.findViewById(R.id.tv_id);
            viewHolder.tvName = convertView.findViewById(R.id.tv_name);
            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != clickListener)
                {
                    clickListener.onClick(getItem(position));
                }
            }
        });
        viewHolder.tvName.setText(getItem(position).getName());
        viewHolder.tvId.setText(getItem(position).getId() + "");
        return convertView;
    }

    interface OnClickListener
    {
        void onClick(Peer peer);
    }
    class ViewHolder
    {
        TextView tvName;
        TextView tvId;
    }
}
