package org.ohmage.funf;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import edu.mit.media.funf.FunfManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by changun on 7/18/14.
 */
public class OhmageFunfManager extends FunfManager {
    private SharedPreferences prefs;
    public void updateLastUploadtime(long time){
        this.prefs.edit().putLong("LAST_UPLOAD_TIME", time).commit();
    }
    public void startForeground(final String text){
        // create a foreground notification
        final Notification notification = new Notification(R.drawable.ic_statistics,
                getResources().getString(R.string.status_bar),
                System.currentTimeMillis());
        // intent for launching main activity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this,
                getResources().getString(R.string.status_title),   // notification title
                text,                                              // notification content
                pendingIntent);

        startForeground(this.hashCode(), notification);
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long lastUploadTime = prefs.getLong("LAST_UPLOAD_TIME", -1);
                if(lastUploadTime > 0) {
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
    @Override
    public void onCreate(){
        super.onCreate();
         this.prefs = this.getSharedPreferences(this.getClass().getName(), MODE_PRIVATE);
        // make this service running in foreground
        startForeground("Just started!");
    }
}
