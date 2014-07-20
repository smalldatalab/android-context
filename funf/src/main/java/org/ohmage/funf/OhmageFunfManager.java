package org.ohmage.funf;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.text.format.DateUtils;
import edu.mit.media.funf.FunfManager;
import org.ohmage.streams.StreamContract;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by changun on 7/18/14.
 */
public class OhmageFunfManager extends FunfManager {
    private long lastUploadTime  = -1;
    public void updateLastUploadTime(long time){
        this.lastUploadTime = time;
    }
    private void createOhmageNotInstalledNotification(){
        // create a foreground notification
        final Notification notification = new Notification(
                R.drawable.ic_error, // error icon
                "Error: ohmage is not installed",
                System.currentTimeMillis());
        // intent for launching main activity
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=org.ohmage.app"));
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification.setLatestEventInfo(this,
                "Context data collector need ohmage to collect data",   // notification title
                "Click here to install ohmage",                    // notification content
                pendingIntent);

        startForeground(this.hashCode(), notification);
    }
    private void startForeground(){
        // create a foreground notification
        final Notification notification = new Notification(
                R.drawable.ic_statistics, // pie_chart icon
                getResources().getString(R.string.status_bar),
                System.currentTimeMillis());
        // intent for launching main activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this,
                getResources().getString(R.string.status_title),   // notification title
                "Just started!",                                   // notification content
                pendingIntent);

        startForeground(this.hashCode(), notification);

    }
    private void startPeriodicNotification(){
      // start a period task to update the notification bar
      Timer myTimer = new Timer();

        // intent for launching main activity
        Intent notificationIntent = new Intent(OhmageFunfManager.this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(OhmageFunfManager.this, 0, notificationIntent, 0);

        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(lastUploadTime > 0) {
                    final Notification notification = new Notification(R.drawable.ic_statistics,
                            getResources().getString(R.string.status_bar),
                            System.currentTimeMillis());
                    String relTimeSpanString = DateUtils.getRelativeTimeSpanString(OhmageFunfManager.this,
                            lastUploadTime).toString();
                    notification.setLatestEventInfo(OhmageFunfManager.this,
                            getResources().getString(R.string.status_title),   // notification title,
                            "Last data point at " + relTimeSpanString,         // notification content
                            pendingIntent);
                    startForeground(this.hashCode(), notification);
                }
            }
        }, 3000, 10 * 60 * 1000); // update notification every 10 mins
    }
    private boolean ohmageInstalled(){
        return StreamContract.checkContentProviderExists(OhmageFunfManager.this.getContentResolver());
    }
    @Override
    public void onCreate(){
        super.onCreate();
        // only show running notification when ohmage is installed
        if(ohmageInstalled()) {
            // make this service running in foreground
            startForeground();
            startPeriodicNotification();
        }else{
            // notify user to install ohmage
            createOhmageNotInstalledNotification();

            // register a broadcast receiver to check if ohmage has get installed
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            intentFilter.addAction(Intent.ACTION_PACKAGE_INSTALL);
            intentFilter.addDataScheme("package");
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                         if(ohmageInstalled()){
                             // reload pipelines when ohmage is installed
                             OhmageFunfManager.this.reload(); // restart the pipeline
                             startForeground();
                             startPeriodicNotification();
                         }
                }
            }, intentFilter);
        }

    }
}
