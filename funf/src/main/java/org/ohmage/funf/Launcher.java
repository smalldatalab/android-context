package org.ohmage.funf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Launcher extends BroadcastReceiver {
    public Launcher() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, OhmageFunfManager.class));
    }
}
