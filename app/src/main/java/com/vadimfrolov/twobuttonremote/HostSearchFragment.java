/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.twobuttonremote;

import android.content.Context;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.vadimfrolov.twobuttonremote.Network.HostBean;
import com.vadimfrolov.twobuttonremote.Network.NetInfo;

/**
 * A fragment representing a list of hosts.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class HostSearchFragment extends FragmentNet implements DiscoveryListener {

    private OnListFragmentInteractionListener mListener;
    private AbstractDiscovery mDiscoveryTask = null;
    private HostInfoRecyclerViewAdapter mAdapter;
    private ProgressBar mProgressBar = null;
    private ConstraintLayout mNotificationView;
    private View mListView;
    private int mCurrentNetwork = 0; // Hash (ID) of a currently connected network
    // Reference to MenuItem that is used to add host manually. Used to control its visibility.
    private MenuItem mAddHostMenuItem = null;
    private boolean mIsTablet = false;

    //protected NetInfo mNetInfo = null;
    protected long mNetworkIp;
    protected long mNetworkStart;
    protected long mNetworkEnd;

    @Override
    public void onNewHost(HostBean host) {
        if (mAdapter != null) {
            mAdapter.addItem(host);
        }
    }

    @Override
    public int getTimeout() {
        return 250;
    }

    @Override
    public String getGatewayIp() {
        if (mNetInfo == null) {
            return NetInfo.NOIP;
        }

        return mNetInfo.gatewayIp;
    }

    @Override
    public void onStartDiscovering() {
        if (mProgressBar == null) {
            AppCompatActivity act = (AppCompatActivity)getActivity();
            mProgressBar = (ProgressBar) act.findViewById(R.id.pbHostsDiscovery);
        }

        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            setDiscoverProgress(0);
        }
    }

    @Override
    public void onStopDiscovering() {
        mDiscoveryTask = null;
        if (getActivity() != null) {
            Toast.makeText((Context) getActivity(),
                    R.string.discovery_stopped, Toast.LENGTH_SHORT).show();
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void setDiscoverProgress(int progress) {
        if (mProgressBar != null) {
            mProgressBar.setProgress(progress);
        }
        //getActivity().setProgress(progress);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(HostBean item);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HostSearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = (Context)getActivity();

        setHasOptionsMenu(true);
    }

    @Override
    protected void updateNetworkStatus() {
        if (mNotificationView == null || mListView == null) {
            return;
        }

        // Update UI
        if (mIsConnected) {
            mListView.setVisibility(View.VISIBLE);
            mNotificationView.setVisibility(View.GONE);

            // Get current network details
            mNetworkIp = NetInfo.getUnsignedLongFromIp(mNetInfo.ip);
            // Detected IP
            int shift = (32 - mNetInfo.cidr);
            if (mNetInfo.cidr < 31) {
                mNetworkStart = (mNetworkIp >> shift << shift) + 1;
                mNetworkEnd = (mNetworkStart | ((1 << shift) - 1)) - 1;
                // DEBUG:
                //mNetworkEnd = mNetworkStart + 50;
            } else {
                mNetworkStart = (mNetworkIp >> shift << shift);
                mNetworkEnd = (mNetworkStart | ((1 << shift) - 1));
            }

            if (mCurrentNetwork != mNetInfo.hashCode()) {
                mCurrentNetwork = mNetInfo.hashCode();
                // start/restart discovery
                mAdapter.clear();
                startDiscover();
            } else {
                if (mAdapter.getItemCount() == 0) {
                    startDiscover();
                }
            }
        } else {
            if (mDiscoveryTask != null) {
                mDiscoveryTask.cancel(true);
            }
            mListView.setVisibility(View.GONE);
            mNotificationView.setVisibility(View.VISIBLE);
        }
        // Only show it as a menu action when we are disconnected
        // and we are on a phone layout. There will be an
        // entry in RecyclerView otherwise.
        if (mAddHostMenuItem != null) {
            if (mIsTablet) {
                mAddHostMenuItem.setVisible(false);
            } else {
                mAddHostMenuItem.setVisible(!mIsConnected);
            }
        }
    }

    public void setTablet(boolean isTablet) {
        mIsTablet = isTablet;
        updateNetworkStatus();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_hostinfo_list, container, false);
        mListView = view.findViewById(R.id.list);
        mNotificationView = (ConstraintLayout) view.findViewById(R.id.notification_banner);

        // Set the adapter
        if (mListView instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) mListView;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            mAdapter = new HostInfoRecyclerViewAdapter(context, mListener);
            recyclerView.setAdapter(mAdapter);
        }

        updateNetworkStatus();

        return view;
    }

    public void resetAppBar() {
        AppCompatActivity act = (AppCompatActivity)getActivity();
        Toolbar toolbar = (Toolbar) act.findViewById(R.id.main_toolbar);
        act.setSupportActionBar(toolbar);
        if (toolbar != null) {
            act.getSupportActionBar().setTitle(getResources().getString(R.string.add_host));

            // Add Up Navigation, part 1
            act.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            updateNetworkStatus();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsConnected) {
            updateNetworkStatus();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDiscoveryTask != null) {
            mDiscoveryTask.cancel(true);
            mDiscoveryTask = null;
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.discovery_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mAddHostMenuItem = menu.findItem(R.id.action_add_manually);
        if (mAddHostMenuItem != null && mIsTablet) {
            mAddHostMenuItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Add Up Navigation, part 2 (final)
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;

            case R.id.action_add_manually:
                HostBean manual = new HostBean();
                manual.hostname = mContext.getResources().getString(R.string.hosts_manual);
                manual.hardwareAddress = NetInfo.NOMAC;
                manual.ipAddress = NetInfo.NOIP;
                manual.broadcastIp = NetInfo.NOIP;
                if (mListener != null) {
                    mListener.onListFragmentInteraction(manual);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Discover hosts
     */
    private void startDiscover() {
        if (mDiscoveryTask != null) {
            mDiscoveryTask.cancel(true);
        }

        mDiscoveryTask = new DnsDiscovery(this);
        mDiscoveryTask.setNetwork(mNetworkIp, mNetworkStart, mNetworkEnd);
        mDiscoveryTask.setBroadcastIp(NetInfo.getUnsignedLongFromIp(mNetInfo.broadcastIp));
        mDiscoveryTask.execute();
    }
}
