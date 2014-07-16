package org.ohmage.funf;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.Probe;
import org.joda.time.DateTime;
import org.ohmage.streams.StreamContract;
import org.ohmage.streams.StreamPointBuilder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by changun on 7/14/14.
 */
public class OhmageFunfPipeline implements Pipeline{

    private boolean enabled = false;
    private Looper looper;
    private Handler handler;
    private FunfManager manager;
    SharedPreferences checkpoints;
    static class OhmageStream{
        String id;
        int version;
        StartableDataSource source;
    }
    public OhmageFunfPipeline() {  }

    @Configurable
    protected List<OhmageStream> streams = null;

        @Override
        public void onCreate(final FunfManager manager) {
            HandlerThread thread = new HandlerThread(getClass().getName());
            thread.start();
            this.looper = thread.getLooper();
            this.handler = new Handler(looper);
            this.manager = manager;
            this.checkpoints = manager.getSharedPreferences("checkpoints", 0);
            if (enabled == false) {
                for (final OhmageStream stream : streams) {
                    final String key = manager.getGson().toJson(stream);
                     if(stream == null || stream.source == null){
                         continue;
                     }
                    // set up data listener
                    stream.source.setListener(new Probe.DataListener(){
                        // get stored checkpoint (if any), notice that we don't update its value after the probe is running
                        final Long checkpoint =  checkpoints.getLong(key, -1);
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
                                            if(data.has("timestamp")){ // for timestamped checkpoint
                                                // convert epoch second to epoch millis
                                                long timestampLong = data.get("timestamp")
                                                                         .getAsBigDecimal()
                                                                         .multiply(new BigDecimal(1000)).longValue();
                                                if(timestampLong <= checkpoint){
                                                    //do not submit the data point whose time is before the checkpoint
                                                    Log.i("OhmageUpload", "Skip data point before checkpoint " + probeConfig);
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


                                            manager.updateLastUploadtime(new Date().getTime());
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
                                Long newCheckpointLong = newCheckpoint.getAsBigDecimal().multiply(new BigDecimal(1000)).longValue();
                                // commit checkpoint
                                checkpoints.edit().putLong(key, newCheckpointLong).commit();
                                // notice that, we don't update the checkpoint field, as we don't know if this call
                                // will occur before the onDataReceived().
                                Log.i("Checkpoint", "Committed checkpoint for " + probeConfig);
                            }

                        }
                    });

                    stream.source.start();

                }
            }
            enabled = true;

        }

        @Override
        public void onRun(String action, JsonElement config) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onDestroy() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    destroyDataSources();
                    looper.quit();
                }
            });

        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }


    private void destroyDataSources() {
        if (enabled == true) {

            for (OhmageStream stream : streams) {
                stream.source.stop();

            }
            enabled = false;
        }
    }



}
