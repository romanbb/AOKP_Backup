package com.aokp.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.aokp.backup.BackupService.BackupFileSystemChange;

/**
 * Created by roman on 6/16/13.
 */
public class RefreshReceiver extends BroadcastReceiver {

    public static final String ACTION_REFRESH = "com.aokp.backup.RefreshReceiver.ACTION_REFRESH";
    private static final String TAG = RefreshReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {
        AOKPBackup.getBus().post(new BackupFileSystemChange());
        Log.d(TAG, "sent refresh!");
    }

    public static Intent getBroadcastIntent() {
        Intent intent = new Intent();
        intent.setAction(ACTION_REFRESH);
        return intent;
    }
}
