package com.aokp.backup;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by roman on 6/3/13.
 */
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {

        // clear it either way
        NotificationManager not = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        not.cancel(BackupService.RESTORE_COMPLETED_NOTIFICATION_ID);

    }
}
