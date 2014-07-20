package org.ohmage.funf;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import edu.mit.media.funf.FunfManager;
import org.json.JSONObject;
import org.ohmage.streams.StreamContract;
import org.ohmage.streams.StreamPointBuilder;
import org.ohmage.streams.StreamWriter;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, OhmageFunfManager.class));
    }

     @Override
     protected void onStart(){
         super.onStart();
         // if ohmage is not installed, prompt user to install it.
         if(!StreamContract.checkContentProviderExists(this.getContentResolver())){
             // 1. Instantiate an AlertDialog.Builder with its constructor
             AlertDialog.Builder builder = new AlertDialog.Builder(this);

             // 2. Chain together various setter methods to set the dialog characteristics
             builder.setMessage("Please install ohmage. The ohmage is required to collect data.")
                     .setTitle("Please install ohmage")
                     .setCancelable(false)
                     .setPositiveButton("Click here to install!", new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialogInterface, int i) {
                             Intent intent = new Intent(Intent.ACTION_VIEW);
                             intent.setData(Uri.parse("market://details?id=org.ohmage.app"));
                             MainActivity.this.startActivity(intent);
                         }
                     });
             // show dialog
             builder.show();

         }
     }

}
