package org.ohmage.funf;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DSUService extends Service {
    private static String TAG = DSUService.class.getSimpleName();
    // SyncAdapter object
    private DSUSyncAdapter sSyncAdapter;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();

    // SyncAdapter object
    private DSUAuthenticator sAuth;
    // Object to use as a thread-safe lock
    private static final Object sAuthLock = new Object();

    public DSUService() {
        Log.i(TAG, "Initializing sync adapter");

        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new DSUSyncAdapter(this, true);
            }
        }
        synchronized (sAuthLock) {
            if (sAuth == null) {
                sAuth = new DSUAuthenticator(this);
            }
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        if(intent.getAction().equals("android.accounts.AccountAuthenticator")){
            return sAuth.getIBinder();
        }else if(intent.getAction().equals("android.content.SyncAdapter")) {
            return sSyncAdapter.getSyncAdapterBinder();
        }
        return null;
    }
}
