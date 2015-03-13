package org.ohmage.funf;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonObject;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;

/**
 * Created by changun on 3/12/15.
 */
public class DSUSyncAdapter extends AbstractThreadedSyncAdapter{
        final static String TAG = DSUSyncAdapter.class.getSimpleName();
        private class TokenExpiredError extends Exception{};
        public DSUSyncAdapter(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
        }

        @Override
        public void onPerformSync(Account account,
                                  Bundle extras, String authority, ContentProviderClient provider,
                                  SyncResult syncResult) {
            Log.i(TAG, "Syncing");
            try {
                AccountManager accountManager = (AccountManager) getContext().getSystemService(getContext().ACCOUNT_SERVICE);
                List<ProbeObject> probes = ContextContentProvider.getAll(this.getContext());
                String token = accountManager.blockingGetAuthToken(DSUAuth.ACCOUNT, DSUAuth.ACCESS_TOKEN_TYPE, true);
                for (ProbeObject probe : probes) {
                    JsonObject json = probe.getJson();
                    Log.i(TAG, "Try to Upload " + json.toString());
                    Response response = DSUClient.postData(token, json.toString());
                    if(response.isSuccessful() || response.code() == 409) { // success or conflict(409)
                        // Remove the probe from the database
                        ContextContentProvider.delete(getContext(), probe);
                        syncResult.stats.numEntries ++;
                        syncResult.stats.numInserts ++;
                    }else if(response.code() == 401){// the token is invalid
                        accountManager.invalidateAuthToken(DSUAuth.ACCOUNT_TYPE, token);
                        throw new TokenExpiredError();
                    }else{// unknown response
                        Log.e(TAG, "Unknown response code(" + response.code() +") from DSU"  + response.body());
                        syncResult.stats.numSkippedEntries++;
                    }
                }
            } catch (AuthenticatorException e) {
                syncResult.stats.numAuthExceptions++;
                Log.e(TAG, "Sync error", e);
            } catch (OperationCanceledException e) {
                Log.e(TAG, "Sync error", e);
            } catch (IOException e) {
                syncResult.stats.numIoExceptions++;
                Log.e(TAG, "Sync error", e);
            } catch (TokenExpiredError tokenExpiredError) {
                Log.i(TAG, "Token expired");
            }
        }

}
