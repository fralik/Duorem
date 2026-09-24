/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;

public class HostSearchFragment extends Fragment implements DiscoveryListener {
    private static final String DISCOVERY_PREFERENCES = "discovery_preferences";
    private static final String USE_ROOT_LOOKUP = "use_root_lookup";
    private OnListFragmentInteractionListener listener;
    private DnsDiscovery discovery;
    private HostInfoRecyclerViewAdapter adapter;
    private ProgressBar progress;
    private View notice;
    private String networkIdentity;

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(HostBean item);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.fragment_hostinfo_list, container, false);
        RecyclerView list = view.findViewById(R.id.list);
        adapter = new HostInfoRecyclerViewAdapter(requireContext(), listener);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        notice = view.findViewById(R.id.network_notice);
        notice.setOnClickListener(v -> ((ActivityNet) requireActivity()).requestNetworkPermission());
        return view;
    }

    public void resetAppBar() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar((Toolbar) activity.findViewById(R.id.main_toolbar));
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(R.string.add_host);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void updateNetworkStatus() {
        if (adapter == null || !isResumed()) return;
        ActivityNet activity = (ActivityNet) requireActivity();
        NetInfo network = activity.mNetInfo;
        notice.setVisibility(activity.hasNetworkPermission() ? View.GONE : View.VISIBLE);
        if (!activity.mIsConnected || NetInfo.NOIP.equals(network.ip)) {
            stopDiscovery();
            networkIdentity = null;
        } else if (!network.identity().equals(networkIdentity)) {
            stopDiscovery();
            networkIdentity = network.identity();
            adapter.clear();
            boolean useRootLookup = requireContext().getSharedPreferences(
                    DISCOVERY_PREFERENCES, Context.MODE_PRIVATE)
                    .getBoolean(USE_ROOT_LOOKUP, false);
            discovery = new DnsDiscovery(this, network, useRootLookup);
            discovery.start();
        }
        requireActivity().invalidateOptionsMenu();
    }

    @Override public void onResume() { super.onResume(); updateNetworkStatus(); }

    @Override
    public void onPause() {
        stopDiscovery();
        networkIdentity = null;
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopDiscovery();
        adapter = null;
        notice = null;
        progress = null;
        super.onDestroyView();
    }

    private void stopDiscovery() {
        if (discovery != null) {
            discovery.close();
            discovery = null;
        }
        if (progress != null) progress.setVisibility(View.GONE);
    }

    @Override public int getTimeout() { return 500; }
    @Override public void onNewHost(HostBean host) { if (adapter != null) adapter.addItem(host); }

    @Override
    public void onStartDiscovering() {
        progress = requireActivity().findViewById(R.id.pbHostsDiscovery);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopDiscovering() {
        stopDiscovery();
    }

    @Override
    public void setDiscoverProgress(int value) {
        if (progress != null) progress.setProgress(value);
    }

    @Override
    public void onDiscoveryError() {
        stopDiscovery();
        Toast.makeText(requireContext(), R.string.discovery_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRootLookupUnavailable() {
        requireContext().getSharedPreferences(DISCOVERY_PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean(USE_ROOT_LOOKUP, false).apply();
        requireActivity().invalidateOptionsMenu();
        Toast.makeText(requireContext(), R.string.root_lookup_unavailable,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (OnListFragmentInteractionListener) context;
    }

    @Override public void onDetach() { listener = null; super.onDetach(); }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.discovery_menu, menu);
        menu.findItem(R.id.action_root_lookup).setChecked(requireContext().getSharedPreferences(
                DISCOVERY_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(USE_ROOT_LOOKUP, false));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(requireActivity());
            return true;
        }
        if (id == R.id.action_add_manually) {
            HostBean host = new HostBean();
            host.resetForView();
            listener.onListFragmentInteraction(host);
            return true;
        }
        if (id == R.id.action_root_lookup) {
            boolean enabled = !item.isChecked();
            requireContext().getSharedPreferences(DISCOVERY_PREFERENCES, Context.MODE_PRIVATE)
                    .edit().putBoolean(USE_ROOT_LOOKUP, enabled).apply();
            item.setChecked(enabled);
            Toast.makeText(requireContext(), enabled
                    ? R.string.root_lookup_enabled : R.string.root_lookup_disabled,
                    Toast.LENGTH_LONG).show();
            stopDiscovery();
            networkIdentity = null;
            if (adapter != null) adapter.clear();
            updateNetworkStatus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
