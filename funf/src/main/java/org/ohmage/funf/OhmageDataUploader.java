package org.ohmage.funf;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import org.joda.time.DateTime;
import org.ohmage.streams.StreamContract;
import org.ohmage.streams.StreamPointBuilder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Ohmage Data Upload implements the Data Listener and submit the
 * data points to ohmage through content provider when they are available.
 * Created by changun on 7/18/14.
 */
public class OhmageDataUploader implements Probe.DataListener{
        final private OhmageFunfManager manager;
        final SharedPreferences checkpointStore;
        final OhmageFunfPipeline.OhmageStream stream;
        final Long prevCheckpoint;
        final String key;
        final Handler handler;

        @Override
        public void onDataReceived(final IJsonObject probeConfig, final IJsonObject probeData) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (StreamContract.checkContentProviderExists(manager.getContentResolver())) {
                        try {
                            // send with probe config data
                            JsonObject data = probeData.getAsJsonObject();
                            data.add("probeConfig", probeConfig);

                            // create timestamp
                            DateTime timestamp;
                            if(data.has("timestamp")){ // for timestamped prevCheckpoint
                                // convert epoch second to epoch millis
                                long timestampLong = data.get("timestamp")
                                        .getAsBigDecimal()
                                        .multiply(new BigDecimal(1000)).longValue();
                                if(timestampLong <= prevCheckpoint){
                                    //do not submit the data point whose time is before the prevCheckpoint
                                    Log.i("OhmageUpload", "Skip data point before prevCheckpoint " + probeConfig);
                                    return;
                                }
                                timestamp = new DateTime(timestampLong);

                            }else {
                                // or use the current time
                                timestamp = new DateTime();
                            }

                            // submit data point to ohmage
                            new StreamPointBuilder(stream.id, stream.version)
                                    .withId()
                                    .withTime(timestamp)
                                    .setData(data.toString())
                                    .write(manager.getContentResolver());

                            // notify manager that we just upload one datapoint
                            manager.updateLastUploadTime(new Date().getTime());
                        } catch (Exception e) {
                            Log.e("OhmageUpload", e.toString());
                        }

                    }
                }
            });
        }

        @Override
        public void onDataCompleted(IJsonObject probeConfig, JsonElement newCheckpoint) {
            if(newCheckpoint != null) {
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
     *
     * @param manager the funf manager object, which should also contains the Context.
     * @param stream the ohmage stream object. It contains the probe configuration and corresponding ohmage stream
     *               id and version.
     * @param handler handler for upload task.
     */
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        public OhmageDataUploader(OhmageFunfManager manager, OhmageFunfPipeline.OhmageStream stream, Handler handler)      {
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

        }
}
