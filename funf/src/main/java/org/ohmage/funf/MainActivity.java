package org.ohmage.funf;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;


public class MainActivity extends ActionBarActivity
 {
    private static final String TAG = "MainActivity";
     private static final int SIGNIN_DSU_REQUEST = 0;
     private static final int SIGNIN_LIFESTREAMS_REQUEST = 1;
     private static final int CONNECT_TO_MOVES_REQUEST = 2;

     private boolean inSignInProcess = false;
     private SharedPreferences sharedPreferences;
     private String dsuAccessToken, dsuRefreshToken, lifestreamsKey, lifestreamsUid;
     private Boolean isConnectedWithMoves;
     private ProgressDialog progress;

     private AlertDialog allowUsageStatisticsDialog ;
     private AlertDialog installMovesDialog;
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
         installMovesDialog =  new AlertDialog.Builder(this)
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
                     @TargetApi(Build.VERSION_CODES.L)
                     @Override
                     public void onClick(DialogInterface dialog, int which) {

                         Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                         startActivity(intent);
                     }
                 }).create();
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
     @TargetApi(Build.VERSION_CODES.L)
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
    protected void onResume(){
        super.onResume();
        // start service (if it have not been started)
        this.startService(new Intent(this, OhmageFunfManager.class));


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !checkUsageStatsPermission()){
            allowUsageStatisticsDialog.show();
        }
        else if(!isPackageExisted(getString(R.string.moves_package_name))){ // check if Moves app is installed
           installMovesDialog.show();
        }else{
            // get stored credentials
            sharedPreferences = getSharedPreferences(getString(R.string.credentials_pref_name), MODE_PRIVATE);
            dsuAccessToken = sharedPreferences.getString(getString(R.string.dsu_token_field), null);
            dsuRefreshToken = sharedPreferences.getString(getString(R.string.dsu_refresh_token_field), null);
            lifestreamsKey = sharedPreferences.getString(getString(R.string.lifestreams_key_field), null);
            lifestreamsUid = sharedPreferences.getString(getString(R.string.lifestreams_uid_field), null);
            isConnectedWithMoves = sharedPreferences.getBoolean(getString(R.string.if_connected_with_moves_field), false);
            // check if we have all the credentials
            if (!inSignInProcess && (dsuAccessToken == null || dsuRefreshToken == null || lifestreamsKey == null || lifestreamsUid == null || !isConnectedWithMoves)) {
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

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        inSignInProcess = true;
        if(requestCode == SIGNIN_DSU_REQUEST){
            if(responseCode == SignInDSU.SIGN_IN_SUCCEEDED){
                dsuAccessToken = intent.getStringExtra("access_token");
                dsuRefreshToken = intent.getStringExtra("refresh_token");
                sharedPreferences.edit()
                        .putString(getString(R.string.dsu_token_field), dsuAccessToken)
                        .putString(getString(R.string.dsu_refresh_token_field), dsuRefreshToken)
                        .apply();

                progress.setMessage("We are almost done. Please wait.");
                doConnectWithMoves(lifestreamsKey);
            }else{
                inSignInProcess = false;
                Toast.makeText(this, getString(R.string.sing_in_failed_toast), Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode == SIGNIN_LIFESTREAMS_REQUEST){
            if(responseCode == SignInLifestreams.SIGN_IN_SUCCEEDED){
                lifestreamsKey = intent.getStringExtra("key");
                lifestreamsUid = intent.getStringExtra("uid");
                sharedPreferences.edit()
                        .putString(getString(R.string.lifestreams_key_field), lifestreamsKey)
                        .putString(getString(R.string.lifestreams_uid_field), lifestreamsUid)
                        .apply();
                progress.setMessage("Just a few more seconds...");
                doSignInDSU();
            }else{
                inSignInProcess = false;
                progress.dismiss();
                Toast.makeText(this, getString(R.string.sing_in_failed_toast), Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode == CONNECT_TO_MOVES_REQUEST){
            if(responseCode == MovesConnectorActivity.SUCCEEDED){
                sharedPreferences.edit()
                        .putBoolean(getString(R.string.if_connected_with_moves_field), true)
                        .apply();
                isConnectedWithMoves = true;
                progress.dismiss();
                inSignInProcess = false;
                Toast.makeText(this, "Sign-in succeeded! You are all set.", Toast.LENGTH_SHORT).show();

            }else{
                progress.dismiss();
                inSignInProcess = false;
                Toast.makeText(this, getString(R.string.moves_connection_failed_toast), Toast.LENGTH_SHORT).show();
            }
        }

    }
     public boolean isPackageExisted(String targetPackage){
         List<ApplicationInfo> packages;
         PackageManager pm = this.getPackageManager();
         pm = getPackageManager();
         packages = pm.getInstalledApplications(0);
         for (ApplicationInfo packageInfo : packages) {
             if(packageInfo.packageName.equals(targetPackage)) return true;
         }
         return false;
     }
     void doSignInProcess(){
         if(progress != null){
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
     void doConnectWithMoves(String lifestreamsKey){
         Intent i = new Intent(this, MovesConnectorActivity.class);
         i.putExtra("key", lifestreamsKey);

         Log.i(TAG, "ConnectWithMoves");
         startActivityForResult(i, CONNECT_TO_MOVES_REQUEST);
     }
     void doSignInDSU(){
         Intent i = new Intent(this, SignInDSU.class);
         Log.i(TAG, "SignInDSU");
         startActivityForResult(i, SIGNIN_DSU_REQUEST);
     }
     void doSignInLifestreams(){
         Intent i = new Intent(this, SignInLifestreams.class);

         Log.i(TAG, "SignInLifestreams");
         startActivityForResult(i, SIGNIN_LIFESTREAMS_REQUEST);
     }
     void doShowMovesInPlayStore(){

         Log.i(TAG, "InstallMoves");
         final String movesPackageName = getString(R.string.moves_package_name); // getPackageName() from Context or Activity object
         try {
             startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + movesPackageName)));
         } catch (android.content.ActivityNotFoundException anfe) {
             startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + movesPackageName)));
         }
     }



}
