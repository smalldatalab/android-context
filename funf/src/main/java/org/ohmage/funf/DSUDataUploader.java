package org.ohmage.funf;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.AbstractThreadedSyncAdapter;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;
import org.ohmage.streams.StreamContract;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Ohmage Data Upload implements the Data Listener and submit the
 * data points to ohmage through content provider when they are available.
 * Created by changun on 7/18/14.
 */
public class DSUDataUploader implements Probe.DataListener {
	
        final private OhmageFunfManager manager;
        final SharedPreferences checkpointStore;
        final OhmageFunfPipeline.OhmageStream stream;
        final Long prevCheckpoint;
        final String key;
        final Handler handler;
        private final DSUSyncer syncer;
        private final DatabaseHandler db;
        private final String DSU_URL = "https://lifestreams.smalldata.io/dsu/dataPoints";

        @Override
        public void onDataReceived(final IJsonObject probeConfig, final IJsonObject probeData) {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    if (StreamContract.checkContentProviderExists(manager.getContentResolver())) {

                        // send with probe config data
                        JsonObject data = probeData.getAsJsonObject();
                        JsonObject config = probeConfig.getAsJsonObject();

                        // create timestamp
                        DateTime timestamp;
                        if(data.has("timestamp")){ // for timestamped probe data

                            // convert epoch second to epoch millis
                            long timestampLong = data.get("timestamp")
                                .getAsBigDecimal()
                                .multiply(new BigDecimal(1000)).longValue();
                            if(timestampLong <= prevCheckpoint){
                                //do not submit the data point whose time is before the prevCheckpoint
                                //Log.i("OhmageUpload", "Skip data point before prevCheckpoint " + probeConfig);
                                return;
                            }
                            timestamp = new DateTime(timestampLong);
                        } else {
                            // or use the current time
                            timestamp = new DateTime();
                        }

                        // add device information
                        String info =  Build.MODEL + ":" +
                            Build.PRODUCT + "-" + Build.TYPE
                            + " " + Build.VERSION.RELEASE
                            + " " + Build.ID
                            + " " + Build.VERSION.INCREMENTAL
                            + " " + Build.TAGS;
                            
                        // create and store the data in the database
                        db.add(new ProbeObject(data, config, timestamp.toString(), info));
                    }
                }
            });
        }

        @Override
        public void onDataCompleted(IJsonObject probeConfig, JsonElement newCheckpoint) {
            if(newCheckpoint != null && !newCheckpoint.isJsonNull()) {
                // convert from second to millis
                Long newCheckpointLong = newCheckpoint.getAsBigDecimal().multiply(new BigDecimal(1000)).longValue();
                // commit new checkpoint
                checkpointStore.edit().putLong(key, newCheckpointLong).commit();
                // notice that, we don't update the prevCheckpoint field, as we cannot be sure if this call
                // will occur before/or after the onDataReceived().
                Log.i("Checkpoint", "Committed checkpoint for " + probeConfig);
            }
        }

    /**
     * Function for uploading the data to the DSU Server
     * Called by the SyncAdapter in manager
     */
        public void uploadData() {
            List<ProbeObject> probes = db.getAll();
            for (ProbeObject probe : probes) {
                JsonObject json = probe.getJson();
                // Post the contents of the probe to the DSU
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost post = new HttpPost(DSU_URL);
                    post.setHeader(new BasicHeader("Authorization", "Bearer " + R.string.dsu_client_auth));
                    post.setHeader(new BasicHeader("Content-type", "application/json"));
                    post.setEntity(new StringEntity(json.toString()));
                    HttpResponse res = httpclient.execute(post);

                    // Check if sign-in succeeded
                    if (res.getStatusLine().getStatusCode() != 201) {
                        throw new Exception("Fail to write to DSU");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Remove the probe from the database
                db.delete(probe);
            }
            manager.updateLastUploadTime(new Date().getTime());
        }

    /**
     *
     * @param manager the funf manager object, which should also contains the Context.
     * @param stream the ohmage stream object. It contains the probe configuration and corresponding ohmage stream
     *               id and version.
     * @param handler handler for upload task.
     */
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        public DSUDataUploader(OhmageFunfManager manager, OhmageFunfPipeline.OhmageStream stream, Handler handler)      {
            this.manager = manager;
            this.stream = stream;
            this.handler = handler;

            // get the persistent checkpoint store
            this.checkpointStore = manager.getSharedPreferences("checkpoints", 0);
            // use the serialization of the stream object as the key to access the stored checkpoint
            this.key = manager.getGson().toJson(stream);
            // get app installed time
            long appInstalledDate = -1;
            try {
                appInstalledDate = manager
                        .getPackageManager()
                        .getPackageInfo(manager.getPackageName(), 0)
                        .firstInstallTime;
            } catch (Exception e) {
                Log.e("Checkpoint", "Cannot get package install date");
            }
            
            // get previous checkpoint or set it to be the app installed time
            this.prevCheckpoint = checkpointStore.getLong(key, appInstalledDate);

            this.db = new DatabaseHandler(manager.getApplicationContext());
            this.syncer = new DSUSyncer(manager.getApplicationContext(),true);
        }

    /** SyncAdapter class for sending data to the DSU */
    public class DSUSyncer extends AbstractThreadedSyncAdapter {
        public DSUSyncer(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
        }

        @Override
        public void onPerformSync(Account account,
                                  Bundle extras, String authority, ContentProviderClient provider,
                                  SyncResult syncResult) {
            uploadData();
        }
    }
}
