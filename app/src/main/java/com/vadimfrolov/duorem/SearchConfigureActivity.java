/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;

public class SearchConfigureActivity extends ActivityNet
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

        mHostDetailsView = (ViewGroup) findViewById(R.id.host_details_fragement);
        if (savedInstanceState != null) {
            // The fragment manager will handle restoring them if we are being restored from a save state
        } else {
            mHostDetailsView = (ViewGroup) findViewById(R.id.host_details_fragement);
            if (mHostDetailsView != null) {
                HostBean manual = new HostBean();
                manual.resetForView();
                manual.hardwareAddress = NetInfo.NOMAC;
                manual.ipAddress = NetInfo.NOIP;
                manual.broadcastIp = NetInfo.NOIP;

                TargetConfigurationFragment configurationFragment = TargetConfigurationFragment.newInstance(manual);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(mHostDetailsView.getId(), configurationFragment, TargetConfigurationFragment.class.getName());
                fragmentTransaction.commit();
            }
        }
    }

    @Override
    protected void updateNetworkStatus() {
        HostSearchFragment fragment = (HostSearchFragment)
                getSupportFragmentManager().findFragmentById(R.id.host_list_fragment);
        if (fragment != null) fragment.updateNetworkStatus();
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
