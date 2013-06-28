package com.aokp.backup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import com.aokp.backup.backup.Backup;
import com.aokp.backup.backup.BackupFactory;
import com.aokp.backup.util.Tools;
import eu.chainfire.libsuperuser.Shell;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by roman on 6/1/13.
 */
public class BackupService extends Service {

    // ones currently in progress
    public static final int BACKUP_NOTIFICATION_ID = 21385;
    public static final int RESTORE_NOTIFICATION_ID = 21386;

    public static final int BACKUP_COMPLETED_NOTIFICATION_ID = 21387;
    public static final int RESTORE_COMPLETED_NOTIFICATION_ID = 21388;

    public static final String ACTION_NEW_BACKUP
            = "com.aokp.backup.BackupService.ACTION_NEW_BACKUP";
    public static final String ACTION_RESTORE_BACKUP
            = "com.aokp.backup.BackupService.ACTION_RESTORE_BACKUP";

    private static final String TAG = BackupService.class.getSimpleName();
    private static final String ACTION_CLEAR_NOTIFICATION
            = "com.aokp.backup.BackupService.ACTION_CLEAR_NOTIFICATION";

    private AsyncTask<String, Void, Boolean> mUpdateTask;

    private Notification mNotification;
    private PendingIntent mPender;

    private BroadcastReceiver mNotificationClickedReceiver;

    private class NotificationClickReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("notification_id")) {
                switch (intent.getIntExtra("notification_id", 0)) {
                    case BACKUP_COMPLETED_NOTIFICATION_ID:
                        NotificationManager not = (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                        not.cancel(BACKUP_COMPLETED_NOTIFICATION_ID);
                        return;
                }
            }

            stopSelf();
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AOKPBackup.getBus().register(this);

        mNotificationClickedReceiver = new NotificationClickReceiver();
        registerReceiver(mNotificationClickedReceiver, new IntentFilter(ACTION_CLEAR_NOTIFICATION));
    }

    @Override
    public void onDestroy() {
        hideOngoingNotification();

        if (mNotificationClickedReceiver != null) {
            this.unregisterReceiver(mNotificationClickedReceiver);
        }

        AOKPBackup.getBus().unregister(this);

        if (mUpdateTask != null) {
            mUpdateTask.cancel(true);
            mUpdateTask = null;
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        final String action = intent.getAction();
        if (ACTION_NEW_BACKUP.equals(action)) {

            String name = intent.getStringExtra("name");
            if (mUpdateTask == null) {

                mUpdateTask = new AsyncTask<String, Void, Boolean>() {

                    Backup b;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        showOngoingNotification();
                    }

                    @Override
                    protected Boolean doInBackground(String... params) {
                        boolean success;
                        Tools.setROMControlPid(Tools.getRomControlPid());

                        b = BackupFactory.getNewBackupObject(BackupService.this, params[0]);

                        if (intent.hasExtra("category_filter")) {
                            ArrayList<Integer> filter = intent
                                    .getIntegerArrayListExtra("category_filter");
                            b.setCategoryFilter(filter);
                        }

                        try {
                            List<String> sudo = b.initBackup();
                            if (sudo != null && !sudo.isEmpty()) {
                                Shell.SU.run(sudo);
                            }

                            success = b.doBackupZip();
                        } catch (Exception e) {
                            Log.e(TAG, "Backup failed with exception: " + e.getMessage(), e);
                            success = false;
                        }

                        return success;
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        super.onPostExecute(success);
                        if (b != null) {
                            b.onBackupCompleted(success);

                            Intent dbxDelete = new Intent(BackupService.this, DropboxSyncService.class);
                            BackupService.this.startService(dbxDelete);
                        }
                        AOKPBackup.getBus().post(new BackupFileSystemChange(success));
                        hideOngoingNotification();
                        notifyBackupDone(success);
                        mUpdateTask = null;
                    }
                }.execute(name);
            }
        } else if (ACTION_RESTORE_BACKUP.equals(action)) {

            final NotificationManager not = (NotificationManager) this
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            not.cancel(BACKUP_COMPLETED_NOTIFICATION_ID);

            final String path = intent.getStringExtra("path");
            mUpdateTask = new AsyncTask<String, Void, Boolean>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    Notification notification = new NotificationCompat.Builder(BackupService.this)
                            .setContentTitle("AOKP Backup")
                            .setContentText("Restore in progress...")
                            .setOngoing(true)
                            .setSmallIcon(R.drawable.ic_noti_backup_complete)
                            .build();

                    not.notify(RESTORE_NOTIFICATION_ID, notification);
                }

                @Override
                protected Boolean doInBackground(String... params) {

                    boolean result;
                    try {
                        Backup b = BackupFactory
                                .fromZipOrDirectory(getApplicationContext(), new File(params[0]));
                        List<String> sudo = b.doRestore();

                        if (sudo != null) {
                            Shell.SU.run(sudo);
                            result = true;
                        } else if (sudo != null && sudo.isEmpty()) {
                            result = true;
                        } else {
                            result = false;
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Restore failed!!!!!", e);
                        result = false;
                    } finally {
                        FileUtils.deleteQuietly(
                                Tools.getTempRestoreDirectory(BackupService.this, false));
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    super.onPostExecute(success);
                    not.cancel(RESTORE_NOTIFICATION_ID);

                    BackupFileSystemChange event = new BackupFileSystemChange();
                    event.restored = success;

                    notifyRestoreDone(success);
                    mUpdateTask = null;
                }
            }.execute(path);
        }

        return START_NOT_STICKY;
    }

    /**
     * Restore is finished. Add notification until reboot.
     */
    private void notifyRestoreDone(Boolean success) {

        Intent backupComplete = new Intent(this, BackupActivity.class);
        backupComplete.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        backupComplete.putExtra("restore_completed", true);

        mPender = PendingIntent
                .getActivity(this, 0, backupComplete, PendingIntent.FLAG_CANCEL_CURRENT);

        Bitmap icon = BitmapFactory
                .decodeResource(getResources(), R.drawable.ic_noti_backup_complete_large);

        String title = success ? "Restore completed" : "Restore failed!";
        Notification notification = new Builder(this)
                .setContentTitle(title)
                .setContentText("Tap to reboot now!")
                .setOngoing(success)
                .setContentIntent(mPender)
                .setLargeIcon(icon)
                .setSmallIcon(R.drawable.ic_noti_backup_complete)
                .build();

        NotificationManager not = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        not.notify(RESTORE_COMPLETED_NOTIFICATION_ID, notification);

        stopSelf();
    }

    /**
     * Backup is done.
     */
    private void notifyBackupDone(boolean success) {
        Intent notify = new Intent(ACTION_CLEAR_NOTIFICATION);
        notify.putExtra("notification_id", BACKUP_COMPLETED_NOTIFICATION_ID);

        mPender = PendingIntent.getBroadcast(this, 0, notify, PendingIntent.FLAG_CANCEL_CURRENT);

        Bitmap icon = BitmapFactory
                .decodeResource(getResources(), R.drawable.ic_noti_backup_complete_large);
        String title = success ? "Backup completed" : "Backup failed!";
        Notification notification = new Builder(this)
                .setContentTitle(title)
                .setContentText("Tap to dismiss")
                .setAutoCancel(true)
                .setContentIntent(mPender)
                .setLargeIcon(icon)
                .setSmallIcon(R.drawable.ic_noti_backup_complete)
                .build();

        NotificationManager not = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        not.notify(BACKUP_COMPLETED_NOTIFICATION_ID, notification);
    }

    private void hideOngoingNotification() {
        NotificationManager not = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        not.cancel(BACKUP_NOTIFICATION_ID);
        mNotification = null;
    }

    private void showOngoingNotification() {
        mNotification = new NotificationCompat.Builder(this)
                .setContentTitle("AOKP Backup")
                .setContentText("Backup in progress...")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_noti_backup_complete)
                .build();

        NotificationManager not = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        not.notify(BACKUP_NOTIFICATION_ID, mNotification);
    }


    public static class BackupFileSystemChange {

        public boolean success;
        public boolean restored;

        public BackupFileSystemChange() {
        }

        public BackupFileSystemChange(Boolean success) {
            this.success = success;
        }
    }
}
