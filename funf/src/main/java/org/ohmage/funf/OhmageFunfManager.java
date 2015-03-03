package org.ohmage.funf;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.ohmage.streams.StreamContract;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by changun on 7/18/14.
 */
public class OhmageFunfManager extends FunfManager {
    private static final String TAG = "OhmageFunfManager";
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

        // Initialize DSU-related structures
        Log.i(TAG, "Initializing database and syncer");
        this.db = new DatabaseHandler(this);
        this.mAccount = createSyncAccount(this);
        this.syncer = new DSUSyncer(this,true);
    }

    @Override
    public IBinder onBind(Intent intent){
        super.onBind(intent);
        return syncer.getSyncAdapterBinder();
    }

    /** DSU Database access */
    private DatabaseHandler db;

    public DatabaseHandler getDatabase() {
        return db;
    }

    /** DSU Syncing */
    public static final String ACCOUNT_AUTH = "org.ohmage.funf.syncer";
    public static final String ACCOUNT_TYPE = "dummy.com";
    public static final String ACCOUNT_NAME = "dummy";
    private final String DSU_URL = "https://lifestreams.smalldata.io/dsu/dataPoints";

    private Account mAccount;
    private DSUSyncer syncer;

    /** Function for creating a dummy Account */
    public static Account createSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
//        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
//            // Does this work w/o a content provider?
//            context.getContentResolver().setIsSyncable(newAccount, ACCOUNT_AUTH, 1);
//            return newAccount;
//        } else {
//            /* The account exists or some other error occurred. */
//            return null;
//        }

        return newAccount;
    }

    /** Function for uploading the data to the DSU Server */
    public void uploadData() {
        List<ProbeObject> probes = db.getAll();
        for (ProbeObject probe : probes) {
            JsonObject json = probe.getJson();
            // Post the contents of the probe to the DSU
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost post = new HttpPost(DSU_URL);
                post.setHeader(new BasicHeader("Authorization", "Bearer " + R.string.dsu_client_auth));
                post.setHeader(new BasicHeader("Content-type", "application/json"));
                post.setEntity(new StringEntity(json.toString()));
                HttpResponse res = httpclient.execute(post);

                // Check if sign-in succeeded
                if (res.getStatusLine().getStatusCode() != 201) {
                    throw new Exception("Fail to write to DSU");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Remove the probe from the database
            db.delete(probe);
        }
        updateLastUploadTime(new Date().getTime());
    }

    /** SyncAdapter class for sending data to the DSU */
    public class DSUSyncer extends AbstractThreadedSyncAdapter {
        public DSUSyncer(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
        }

        @Override
        public void onPerformSync(Account account,
                                  Bundle extras, String authority, ContentProviderClient provider,
                                  SyncResult syncResult) {
            Log.i(TAG, "Syncing");
            uploadData();
        }
    }
}