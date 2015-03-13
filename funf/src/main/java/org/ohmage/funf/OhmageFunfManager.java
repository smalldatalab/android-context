package org.ohmage.funf;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.JsonObject;
import com.squareup.okhttp.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import edu.mit.media.funf.FunfManager;

/**
 * Created by changun on 7/18/14.
 */
public class OhmageFunfManager extends FunfManager {
    private static final String TAG = OhmageFunfManager.class.getSimpleName();


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void startForeground(){
        // create a foreground notification
        Notification noti = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.status_title))
                .setTicker(getResources().getString(R.string.status_bar))
                .setSmallIcon(R.drawable.ic_statistics)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0))
                .build();
        startForeground(this.hashCode(), noti);
    }


    @Override
    public void onCreate(){
        super.onCreate();
        startForeground();
        // Initialize DSU-related structures
        Log.i(TAG, "Initializing database");
        this.db = new DatabaseHandler(this);

    }

    @Override
    public IBinder onBind(Intent intent){
        super.onBind(intent);
        return null;
    }

    /** DSU Database access */
    private DatabaseHandler db;
    public DatabaseHandler getDatabase() {
        return db;
    }




}