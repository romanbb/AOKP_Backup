package com.aokp.backup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;
import com.aokp.backup.R.drawable;
import com.aokp.backup.backup.Backup;
import com.aokp.backup.backup.BackupFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by roman on 6/1/13.
 */
public class BackupService extends Service {

    private static final int BACKUP_NOTIFICATION_ID = 21385;
    private static final int BACKUP_COMPLETED_NOTIFICATION_ID = 21387;

    public static final String ACTION_NEW_BACKUP = "com.aokp.backup.BackupService.ACTION_NEW_BACKUP";
    public static final String ACTION_DELETE_BACKUP = "com.aokp.backup.BackupService.ACTION_DELETE_BACKUP";

    private static final String TAG = BackupService.class.getSimpleName();

    private AsyncTask<String, Void, Boolean> mUpdateTask;

    private Notification mNotification;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AOKPBackup.getBus().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AOKPBackup.getBus().unregister(this);
        hideOngoingNotification();
        if (mUpdateTask != null) {
            mUpdateTask.cancel(true);
            mUpdateTask = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String action = intent.getAction();
        if (ACTION_NEW_BACKUP.equals(action)) {

            String name = intent.getStringExtra("name");
            if (mUpdateTask == null) {
                mUpdateTask = new AsyncTask<String, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        showOngoingNotification();
                    }

                    @Override
                    protected Boolean doInBackground(String... params) {
                        Backup b = BackupFactory.getNewBackupObject(getApplicationContext(), params[0]);
                        try {
                            return b.handleBackup();
                        } catch (Exception e) {
                            Log.e(TAG, "Backup failed with exception: " + e.getMessage(), e);
                            return false;
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        super.onPostExecute(success);
                        AOKPBackup.getBus().post(new BackupFileSystemChange(success));
                        hideOngoingNotification();
                        notifyBackupDone(success);
                        stopSelf();
                    }
                }.execute(name);
            }
        } else if (ACTION_DELETE_BACKUP.equals(action)) {

            final String path = intent.getStringExtra("path");

            mUpdateTask = new AsyncTask<String, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(String... params) {
                    Backup deleteMe = null;
                    try {
                        deleteMe = BackupFactory.fromZipOrDirectory(BackupService.this, new File(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                    return deleteMe.deleteFromDisk();
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    super.onPostExecute(success);
                    Toast.makeText(BackupService.this, "Backup deleted", Toast.LENGTH_SHORT).show();

                    AOKPBackup.getBus().post(new BackupFileSystemChange(success));
                    stopSelf();
                }
            }.execute(path);
        }

        return START_NOT_STICKY;
    }

    private void notifyBackupDone(boolean success) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), drawable.ic_backup);
        String title = success ? "Backup completed" : "Backup failed!";
        Notification notify = new Builder(this)
                .setContentTitle(title)
                .setContentText("Tap to dismiss")
                .setAutoCancel(true)
                .setSmallIcon(drawable.ic_backup_old)
                .setLargeIcon(icon)
                .build();

        NotificationManager not = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        not.notify(BACKUP_COMPLETED_NOTIFICATION_ID, notify);


    }

    private void hideOngoingNotification() {
        NotificationManager not = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        not.cancel(BACKUP_NOTIFICATION_ID);
        mNotification = null;
    }

    private void showOngoingNotification() {
        mNotification = new NotificationCompat.Builder(this)
                .setContentTitle("AOKP Backup")
                .setContentText("Backup in progress...")
                .setOngoing(true)
                .setSmallIcon(drawable.ic_backup_old)
                .build();

        NotificationManager not = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        not.notify(BACKUP_NOTIFICATION_ID, mNotification);
    }


    public static class BackupFileSystemChange {
        public boolean success;

        public BackupFileSystemChange(Boolean success) {
            this.success = success;
        }
    }
}
