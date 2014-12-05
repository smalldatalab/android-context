package org.ohmage.funf;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.plus.Plus;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.ohmage.funf.R;

import java.net.URI;
import java.net.URL;
import java.util.List;

public class MovesConnectorActivity extends ActionBarActivity {
    final static private int REQUEST_AUTHORIZE = 0;
    public static final int SUCCEEDED = 0;
    public static final int FAIL_TO_EXCHANGE_FOR_TOKEN = 1;
    public static final int EXCEPTION_OCCURRED = 2;
    public static final int FAIL_TO_GET_AUTHORIZATION = 3;
    private String key;
    private boolean afterConsent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transparent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!afterConsent) {
            key = getIntent().getStringExtra("key");
            doRequestAuthInApp();
        }
        afterConsent = false;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.moves_connector, menu);
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


    /**
     * App-to-app. Creates an intent with data uri starting moves://app/authorize/xxx (for more
     * details, see documentation link below) to be handled by Moves app. When Moves receives this
     * Intent it opens up a dialog asking for user to accept the requested permission for your app.
     * The result of this user interaction is delivered to
     * {@link #onActivityResult(int, int, android.content.Intent) }
     *
     * @see http://dev.moves-app.com/docs/api
     */
    private void doRequestAuthInApp() {

        Uri uri = createAuthUri("moves", "app", "/authorize").build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivityForResult(intent, REQUEST_AUTHORIZE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Moves app not installed", Toast.LENGTH_SHORT).show();
        }

    }
    private void fail (final int type){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent();
                MovesConnectorActivity.this.setResult(type, i);
                MovesConnectorActivity.this.finish();
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            switch (requestCode) {
                case REQUEST_AUTHORIZE:
                    afterConsent = true;
                    Uri resultUri = data.getData();
                    // parse the redirection uri and search for the "code" parameter
                    List<NameValuePair> queryParams;
                    queryParams = URLEncodedUtils.parse(new URI(resultUri.toString()), "UTF-8");
                    String code = null;
                    for (NameValuePair param : queryParams) {
                        if (param.getName().equals("code"))
                            code = param.getValue();
                    }
                    if (code == null) {
                        fail(FAIL_TO_GET_AUTHORIZATION);
                    }else{
                        new ConnectMovesToLifestreams().execute(code);
                    }

            }
        }catch(Exception e){
        }

    }
    private Uri.Builder createAuthUri(String scheme, String authority, String path) {
        return new Uri.Builder()
                .scheme(scheme)
                .authority(authority)
                .path(path)
                .appendQueryParameter("client_id", getString(R.string.moves_client_id))
                .appendQueryParameter("redirect_uri", getString(R.string.moves_redirect_uri))
                .appendQueryParameter("scope", getString(R.string.moves_scope))
                .appendQueryParameter("state", String.valueOf(SystemClock.uptimeMillis()));
    }

    private class ConnectMovesToLifestreams extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try{
                String code = params[0];
                DefaultHttpClient httpclient = new DefaultHttpClient();
                String url = String.format(getString(R.string.lifestreams_oauth_code_endpoint), "moves" ,code, key);
                HttpGet signin = new HttpGet(url);
                HttpResponse res = httpclient.execute(signin);
                if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent();
                            MovesConnectorActivity.this.setResult(SUCCEEDED, i);
                            MovesConnectorActivity.this.finish();
                        }
                    });
                }else {
                    fail(FAIL_TO_EXCHANGE_FOR_TOKEN);
                }
         
            } catch (Exception e) {
                fail(EXCEPTION_OCCURRED);
            }
            return null;
        }


    }

}
