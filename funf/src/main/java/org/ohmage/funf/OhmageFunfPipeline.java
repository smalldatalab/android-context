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
            if(!(manager instanceof OhmageFunfManager)){
                throw new RuntimeException("ohmage funf pipeline only support ohmage funf manager");
            }
            HandlerThread thread = new HandlerThread(getClass().getName());
            thread.start();
            this.looper = thread.getLooper();
            this.handler = new Handler(looper);

            if (enabled == false) {
                for (final OhmageStream stream : streams) {

                     if(stream == null || stream.source == null){
                         continue;
                     }
                    // set up data uploader as the data listener
                    OhmageDataUploader uploader = new OhmageDataUploader((OhmageFunfManager)manager, stream, handler);
                    stream.source.setListener(uploader);
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
