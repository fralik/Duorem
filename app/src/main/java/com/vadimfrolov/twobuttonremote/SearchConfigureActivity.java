package com.vadimfrolov.twobuttonremote;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ProgressBar;

import com.vadimfrolov.twobuttonremote.Network.HostBean;

public class SearchConfigureActivity extends AppCompatActivity
    implements HostSearchFragment.OnListFragmentInteractionListener
    /*implements HostListFragment.OnHostSelectedListener*/ {

    ProgressBar progressBarFooter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_configure);
    }

    @Override
    public void onListFragmentInteraction(HostBean item) {
        //getFragmentManager().findFragmentById()
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
