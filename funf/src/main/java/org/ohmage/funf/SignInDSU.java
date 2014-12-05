package org.ohmage.funf;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class SignInDSU extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /* Request code used to invoke sign in user interactions. */
    private static final int AUTH_CODE_REQUEST_CODE = 1;
    private static final int REQUEST_RESOLVE_ERROR = 2;

    /* Response code used to communicate the sign in result */
    public static final int SIGN_IN_SUCCEEDED = 1;
    public static final int FAILED_TO_GET_AUTH_CODE = 2;
    public static final int FAILED_TO_SIGN_IN = 3;
    public static final int INVALID_ACCESS_TOKEN = 4;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    private boolean afterConsent = false;

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
        if(!afterConsent) {
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

    public void fail(final int reason){
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
        if(result.hasResolution()){
            try {
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {

                fail(FAILED_TO_GET_AUTH_CODE);
            }
        }else {
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
        protected Void doInBackground(Void... params) {


            // ** Step 1. Obtain Google One-Time Auth Code **
            Bundle appActivities = new Bundle();
            String clientId = getString(R.string.dsu_google_client_id);
            String authScope = getString(R.string.dsu_google_scope);
            String scopes = "oauth2:server:client_id:" + clientId +
                            ":api_scope:" + authScope;

            try {

                String code = GoogleAuthUtil.getToken(
                        org.ohmage.funf.SignInDSU.this,                             // Context context
                        Plus.AccountApi.getAccountName(mGoogleApiClient),  // String accountName
                        scopes,                                            // String scope
                        appActivities                                      // Bundle bundle
                );
                // Clear the token cache so that we will get a new one next time
                GoogleAuthUtil.clearToken(org.ohmage.funf.SignInDSU.this, code);

                code = "fromApp_" + code;
                String codeUrl = getString(R.string.dsu_code)
                                 + "?client_id="
                                 + getString(R.string.dsu_client_id)
                                 +"&code=" + code;

            // ** Step 2. Submit this code to DSU **

                HttpClient httpclient = new DefaultHttpClient();
                HttpGet signin = new HttpGet(codeUrl);
                signin.setHeader(new BasicHeader("Authorization", "Basic " + getString(R.string.dsu_client_auth)));
                HttpResponse res = httpclient.execute(signin);
                // Check if sign-in succeeded
                if (res.getStatusLine().getStatusCode() != 200) {
                    throw new Exception("Fail to sign in DSU");
                }

             // ** Step 3. Check Returned Access Tokens **

                String response = EntityUtils.toString(res.getEntity());
                final JSONObject token = new JSONObject(response);
                if(token.has("access_token") && token.has("refresh_token")) {
                    final String accessToken = token.getString("access_token");
                    final String refreshToken = token.getString("refresh_token");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent();
                            i.putExtra("access_token", accessToken);
                            i.putExtra("refresh_token", refreshToken);

                            org.ohmage.funf.SignInDSU.this.setResult(SIGN_IN_SUCCEEDED, i);
                            org.ohmage.funf.SignInDSU.this.finish();
                        }
                    });
                }else{
                    fail(INVALID_ACCESS_TOKEN);
                }

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
