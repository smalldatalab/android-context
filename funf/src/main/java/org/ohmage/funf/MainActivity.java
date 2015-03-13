package org.ohmage.funf;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    private static final int SIGNIN_DSU_REQUEST = 0;
    private static final int SIGNIN_LIFESTREAMS_REQUEST = 1;
    private static final int CONNECT_TO_MOVES_REQUEST = 2;

    private boolean inSignInProcess = false;
    private ProgressDialog progress;
    private ConnectionStore conn;

    private AlertDialog allowUsageStatisticsDialog;
    private AlertDialog installMovesDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installMovesDialog = new AlertDialog.Builder(this)
                .setTitle("Install Moves App")
                .setMessage("Dear Participant: \nYou need to install Moves app for the study. The installation will be started shortly.")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doShowMovesInPlayStore();
                    }
                }).create();
        allowUsageStatisticsDialog = new AlertDialog.Builder(this)
                .setTitle("Allow Access to App Usage Data")
                .setMessage(
                        "Dear Participant: \nTo proceed, please approve Context app to access your app usage statistics. " +
                                "This data is important in generating contextual recall cues.")
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);
                    }
                }).create();
        conn = new ConnectionStore(this);
        setContentView(R.layout.activity_main);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.signin_again:
                doSignInProcess();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
     @TargetApi(Build.VERSION_CODES.LOLLIPOP)
     private boolean checkUsageStatsPermission(){
         try {
             UsageStatsManager mUsageStatsManager = (UsageStatsManager) this.getSystemService("usagestats");
             long time = System.currentTimeMillis();
             // We get usage stats for the last 1 seconds
             List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, (int) (time - 10000), time);
             return !stats.isEmpty();
         }catch(Exception e){
             return false;
         }
     };

    @Override
    protected void onResume() {
        super.onResume();
        // start service (if it have not been started)
        this.startService(new Intent(this, OhmageFunfManager.class));


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !checkUsageStatsPermission()){
            allowUsageStatisticsDialog.show();
        }
        else if (!isPackageExisted(getString(R.string.moves_package_name))) { // check if Moves app is installed
            installMovesDialog.show();
        } else {
            if (!inSignInProcess && (!conn.isConnectedWithDSU() || !conn.isConnectedWithMoves() || !conn.isConnectedWithLifestreams())) {
                new AlertDialog.Builder(this)
                        .setTitle("Sign in the study")
                        .setMessage("Dear Participant: \nWe will start the sign-in process. Please make sure the phone is connected to the Internet.")
                        .setCancelable(false)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doSignInProcess();
                            }
                        }).create().show();
            }

        }
        inSignInProcess = false;

    }

    String lifestreamsKey;

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        inSignInProcess = true;

        if (requestCode == SIGNIN_LIFESTREAMS_REQUEST) {
            if (responseCode == RESULT_OK) {
                conn.setConnectedWithLifestreams(true);
                progress.setMessage("Just a few more seconds...");
                lifestreamsKey = intent.getStringExtra("key");
                doSignInDSU();
            } else {
                inSignInProcess = false;
                progress.dismiss();
                Toast.makeText(this, getString(R.string.sing_in_failed_toast), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CONNECT_TO_MOVES_REQUEST) {
            if (responseCode == MovesConnectorActivity.SUCCEEDED) {
                conn.setConnectedWithMoves(true);
                progress.dismiss();
                inSignInProcess = false;
                Toast.makeText(this, "Sign-in succeeded! You are all set.", Toast.LENGTH_SHORT).show();

            } else {
                progress.dismiss();
                inSignInProcess = false;
                Toast.makeText(this, getString(R.string.moves_connection_failed_toast), Toast.LENGTH_SHORT).show();
            }
        }

    }

    public boolean isPackageExisted(String targetPackage) {
        List<ApplicationInfo> packages;
        PackageManager pm = this.getPackageManager();
        pm = getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage)) return true;
        }
        return false;
    }

    void doSignInProcess() {
        if (progress != null) {
            progress.dismiss();
        }
        progress = new ProgressDialog(MainActivity.this);
        progress.setTitle("Sign In");
        progress.setMessage("We are signing you in! Please wait.");
        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();
        doSignInLifestreams();
    }

    void doConnectWithMoves(String lifestreamsKey) {
        Intent i = new Intent(this, MovesConnectorActivity.class);
        i.putExtra("key", lifestreamsKey);
        startActivityForResult(i, CONNECT_TO_MOVES_REQUEST);
    }

    void doSignInDSU() {
        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

        accountManager.addAccount(DSUAuth.ACCOUNT_TYPE, DSUAuth.ACCESS_TOKEN_TYPE, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    if(future.getResult().getString(AccountManager.KEY_ACCOUNT_NAME)!=null) {
                        conn.setConnectedWithDSU(true);
                        doConnectWithMoves(lifestreamsKey);
                        progress.setMessage("We are almost done. Please wait.");
                    }else{
                        throw new Exception("No account created");
                    }

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            inSignInProcess = false;
                            Toast.makeText(MainActivity.this, getString(R.string.sing_in_failed_toast), Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }

            }
        }, new Handler());

    }

    void doSignInLifestreams() {
        Intent i = new Intent(this, SignInLifestreams.class);
        startActivityForResult(i, SIGNIN_LIFESTREAMS_REQUEST);
    }

    void doShowMovesInPlayStore() {
        final String movesPackageName = getString(R.string.moves_package_name); // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + movesPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + movesPackageName)));
        }
    }


}
