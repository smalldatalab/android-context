package org.ohmage.funf;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.squareup.okhttp.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class SignInDSU extends  AccountAuthenticatorActivity  implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    /* Request code used to invoke sign in user interactions. */
    private static final int AUTH_CODE_REQUEST_CODE = 1;
    private static final int REQUEST_RESOLVE_ERROR = 2;

    /* Response code used to communicate the sign in result */
    public static final int FAILED_TO_GET_AUTH_CODE = 2;
    public static final int FAILED_TO_SIGN_IN = 3;
    public static final int INVALID_ACCESS_TOKEN = 4;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    private boolean afterConsent = false;

    final static String TAG = SignInDSU.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transparent);
        // start gplus integration
        // Init google plus integration
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!afterConsent) {
            mGoogleApiClient.connect();
        }
        afterConsent = false;
    }


    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == AUTH_CODE_REQUEST_CODE) {
            afterConsent = true;
            if (responseCode != RESULT_OK) {
                fail(FAILED_TO_GET_AUTH_CODE);
            } else {
                new SignIn().execute();
            }
        }


    }

    public void fail(final int reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                org.ohmage.funf.SignInDSU.this.setResult(reason);
                org.ohmage.funf.SignInDSU.this.finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sign_in_with_google, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {

                fail(FAILED_TO_GET_AUTH_CODE);
            }
        } else {
            fail(FAILED_TO_GET_AUTH_CODE);
        }
        fail(FAILED_TO_GET_AUTH_CODE);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        new SignIn().execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }
    private class SignIn extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... _) {

            // ** Step 1. Obtain Google One-Time Auth Code **
            Bundle appActivities = new Bundle();
            String scopes = "oauth2:email";

            try {

                String code = GoogleAuthUtil.getToken(
                        SignInDSU.this,                             // Context context
                        Plus.AccountApi.getAccountName(mGoogleApiClient),  // String accountName
                        scopes,                                            // String scope
                        appActivities                                      // Bundle bundle
                );
                Response response = DSUClient.signin(code);
                if(response.isSuccessful()){
                    try {
                        JSONObject token = new JSONObject(response.body().string());
                        final String accessToken = token.getString(DSUAuth.ACCESS_TOKEN_TYPE);
                        final String refreshToken = token.getString(DSUAuth.REFRESH_TOKEN_TYPE);
                        // Get an instance of the Android account manager
                        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

                        accountManager.addAccountExplicitly(DSUAuth.ACCOUNT, null, null);

                        // make the account syncable and automatically synced
                        ContentResolver.setIsSyncable(DSUAuth.ACCOUNT, ContextContentProvider.AUTHORITY, 1);
                        ContentResolver.setSyncAutomatically(DSUAuth.ACCOUNT, ContextContentProvider.AUTHORITY, true);
                        ContentResolver.setMasterSyncAutomatically(true);

                        accountManager.setAuthToken(DSUAuth.ACCOUNT, "access_token", accessToken);
                        accountManager.setAuthToken(DSUAuth.ACCOUNT, "refresh_token", refreshToken);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent();
                                i.putExtra(AccountManager.KEY_ACCOUNT_NAME, DSUAuth.ACCOUNT.name);
                                i.putExtra(AccountManager.KEY_ACCOUNT_TYPE, DSUAuth.ACCOUNT.type);
                                SignInDSU.this.setAccountAuthenticatorResult(i.getExtras());
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Fail to parse response from google-sign-in endpoint", e);
                        fail(INVALID_ACCESS_TOKEN);
                    }
                }else{
                    Log.e(TAG, response.body().string());
                    fail(INVALID_ACCESS_TOKEN);
                }

                // ** Step 3. Check Returned Access Tokens **

            } catch (UserRecoverableAuthException e) {
                // Requesting an authorization code will always throw
                // UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
                // because the user must consent to offline access to their data.  After
                // consent is granted control is returned to your activity in onActivityResult
                // and the second call to GoogleAuthUtil.getToken will succeed.
                startActivityForResult(e.getIntent(), AUTH_CODE_REQUEST_CODE);

            } catch (Exception e) {
                fail(FAILED_TO_SIGN_IN);
            }
            return null;
        }


    }

}
