package org.ohmage.funf;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import java.net.URI;
import java.util.List;

public class SignInLifestreams extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /* Request code used to invoke sign in user interactions. */
    private static final int AUTH_CODE_REQUEST_CODE = 1;
    private static final int REQUEST_RESOLVE_ERROR = 2;

    /* Response code used to communicate the sign in result */
    public static final int FAILED_TO_GET_AUTH_CODE = 2;
    public static final int FAILED_TO_SIGN_IN = 3;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    private boolean afterConsent = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transparent);
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
                SignInLifestreams.this.setResult(reason);
                SignInLifestreams.this.finish();
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


            // ** Step 1. Obtain Access Token from Google API **
            try {
                String accessToken =
                        GoogleAuthUtil.getToken(
                                SignInLifestreams.this,
                                Plus.AccountApi.getAccountName(mGoogleApiClient),
                                getString(R.string.lifestreams_google_scope)
                        );

                // Clear the token cache so that we will get a new one next time
                GoogleAuthUtil.clearToken(SignInLifestreams.this, accessToken);

                // ** Step 2. use the access token to login **

                String signInUrl = getString(R.string.lifestreams_signin_url) + accessToken;
                DefaultHttpClient httpclient = new DefaultHttpClient();
                // prevent redirection
                httpclient.setRedirectHandler(new RedirectHandler() {
                    public URI getLocationURI(HttpResponse response,
                                              HttpContext context) {
                        return null;
                    }
                    public boolean isRedirectRequested(HttpResponse response,
                                                       HttpContext context) {
                        return false;
                    }
                });

                HttpGet signin = new HttpGet(signInUrl);
                HttpResponse res = httpclient.execute(signin);
                    // Check if sign-in succeeded
                    if(res.getFirstHeader("Location") != null)
                    {
                        // Extract the key from the redirect uri
                        String locationUrl = res.getFirstHeader("Location").getValue();
                        // parse the redirection uri and search for the "code" parameter
                        List<NameValuePair> queryParams;
                        queryParams = URLEncodedUtils.parse(new URI(locationUrl), "UTF-8");
                        String key = null, uid = null;
                        for (NameValuePair param : queryParams) {
                            if(param.getName().equals("key")){
                                key = param.getValue();

                            }else if (param.getName().equals("uid")){
                                uid = param.getValue();
                            }
                        }
                        if(key!=null && uid!=null) {
                            final String finalKey = key;
                            final String finalUid = uid;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent i = new Intent();
                                    i.putExtra("key", finalKey);
                                    i.putExtra("uid", finalUid);
                                    SignInLifestreams.this.setResult(RESULT_OK, i);
                                    SignInLifestreams.this.finish();
                                }
                            });
                            return null;
                        }
                    }

                fail(FAILED_TO_SIGN_IN);
                }catch (UserRecoverableAuthException e) {
                // Requesting an authorization code will always throw
                // UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
                // because the user must consent to offline access to their data.  After
                // consent is granted control is returned to your activity in onActivityResult
                // and the second call to GoogleAuthUtil.getToken will succeed.
                startActivityForResult(e.getIntent(), AUTH_CODE_REQUEST_CODE);

            } catch (Exception e) {
                Log.e("SignInLifestreams", "Sign in error", e);
                fail(FAILED_TO_SIGN_IN);
            }
            return null;
        }


    }

}
