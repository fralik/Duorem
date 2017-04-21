/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 2, see README
 */

package com.vadimfrolov.twobuttonremote;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.vadimfrolov.twobuttonremote.Network.HostBean;
import com.vadimfrolov.twobuttonremote.Network.NetInfo;

public class SearchConfigureActivity extends AppCompatActivity
    implements HostSearchFragment.OnListFragmentInteractionListener {

    ProgressBar progressBarFooter;
    ViewGroup mHostDetailsView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_configure);

        HostSearchFragment searchFragment = (HostSearchFragment)
                getSupportFragmentManager().findFragmentById(R.id.host_list_fragment);
        if (searchFragment != null) {
            searchFragment.resetAppBar();
        }

        if (savedInstanceState != null) {
            // The fragment manager will handle restoring them if we are being restored from a save state
        } else {
            mHostDetailsView = (ViewGroup) findViewById(R.id.host_details_fragement);
            if (mHostDetailsView != null) {
                HostBean manual = new HostBean();
                manual.resetForView();
                manual.hostname = getApplicationContext().getResources().getString(R.string.hosts_manual);
                manual.hardwareAddress = NetInfo.NOMAC;
                manual.ipAddress = NetInfo.NOIP;
                manual.broadcastIp = NetInfo.NOIP;

                TargetConfigurationFragment configurationFragment = TargetConfigurationFragment.newInstance(manual);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(mHostDetailsView.getId(), configurationFragment, TargetConfigurationFragment.class.getName());
                fragmentTransaction.commit();
                configurationFragment.prepareForTablet();
            }
            if (searchFragment != null) {
                searchFragment.setTablet(mHostDetailsView != null);
            }
        }
    }

    @Override
    public void onListFragmentInteraction(HostBean item) {
        TargetConfigurationFragment configurationFragment = (TargetConfigurationFragment)
                getSupportFragmentManager().findFragmentById(R.id.host_details_fragement);
        if (configurationFragment != null) {
            configurationFragment.updateTarget(item);
        } else {
            Intent intent = new Intent(this, TargetConfigurationActivity.class);
            intent.putExtra(HostBean.EXTRA, item);
//            PendingIntent pendingIntent = TaskStackBuilder.create(this)
//                    .addNextIntentWithParentStack(intent)
//                    .getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//            builder.setContentIntent(pendingIntent);
            this.startActivity(intent);
        }
    }
}
