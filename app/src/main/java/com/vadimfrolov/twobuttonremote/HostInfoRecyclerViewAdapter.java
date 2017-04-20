package com.vadimfrolov.twobuttonremote;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vadimfrolov.twobuttonremote.HostSearchFragment.OnListFragmentInteractionListener;
import com.vadimfrolov.twobuttonremote.Network.HostBean;
import com.vadimfrolov.twobuttonremote.Network.NetInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link HostBean} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class HostInfoRecyclerViewAdapter extends RecyclerView.Adapter<HostInfoRecyclerViewAdapter.ViewHolder> {

    private List<HostBean> mValues;
    private final OnListFragmentInteractionListener mListener;
    private Context mContext;

    public HostInfoRecyclerViewAdapter(List<HostBean> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    public HostInfoRecyclerViewAdapter(Context context, OnListFragmentInteractionListener listener) {
        mListener = listener;
        mValues = new ArrayList<>();
        mContext = context;
    }

    public void clear() {
        mValues.clear();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_hostinfo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).name());
        holder.mContentView.setText(mValues.get(position).hardwareAddress.toUpperCase());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void addItem(HostBean bean) {
        if (bean != null && !bean.hardwareAddress.equals(NetInfo.NOMAC)) {
            mValues.add(bean);
            Collections.sort(mValues, new Comparator<HostBean>() {
                @Override
                public int compare(HostBean lhs, HostBean rhs) {
                    long leftIp = NetInfo.getUnsignedLongFromIp(lhs.ipAddress);
                    long rightIp = NetInfo.getUnsignedLongFromIp(rhs.ipAddress);
                    if (leftIp > rightIp)
                        return 1;
                    if (leftIp == rightIp)
                        return 0;
                    return -1;
                }
            });
            // we have to notify of the whole dataset change since we do the sorting
            notifyDataSetChanged();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public HostBean mItem;

        public ViewHolder(View view) {
            super(view);
            mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
