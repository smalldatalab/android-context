package org.ohmage.funf;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonElement;

import java.util.List;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.pipeline.Pipeline;

/**
 * Created by changun on 7/14/14.
 */
public class OhmageFunfPipeline implements Pipeline {

    private static final String TAG = "OhmageFunfPipeline";
    private boolean enabled = false;
    private Looper looper;
    private Handler handler;

    static class OhmageStream {
        String id;
        int version;
        StartableDataSource source;
    }

    public OhmageFunfPipeline() {
    }

    @Configurable
    protected List<OhmageStream> streams = null;

    @Override
    public void onCreate(final FunfManager manager) {

        Log.i(TAG, "Beginning pipeline");

        if (!(manager instanceof OhmageFunfManager)) {
            throw new RuntimeException("ohmage funf pipeline only support ohmage funf manager");
        }

        Log.i(TAG, "Checked manager instance");

        HandlerThread thread = new HandlerThread(getClass().getName());
        thread.start();
        this.looper = thread.getLooper();
        this.handler = new Handler(looper);

        Log.i(TAG, "Finished initial setup");

        if (enabled == false) {
            Log.i(TAG, "Checking streams");
            for (final OhmageStream stream : streams) {
                Log.i(TAG, "Creating uploader for stream");
                if (stream == null || stream.source == null) {
                    continue;
                }
                // set up data uploader as the data listener
                DSUDataWriter uploader = new DSUDataWriter((OhmageFunfManager) manager, stream, handler);
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
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    destroyDataSources();
                    looper.quit();
                }
            });
        }

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
