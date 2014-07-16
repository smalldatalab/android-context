package org.ohmage.funf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.mit.media.funf.FunfManager;

public class BootBroadcastReceiver extends BroadcastReceiver {
    public BootBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, FunfManager.class));
    }
}
