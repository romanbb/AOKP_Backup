package com.aokp.backup;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.aokp.backup.util.DropboxUtils;
import com.aokp.backup.util.Tools;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by roman on 6/15/13.
 */
@SuppressLint("NewApi")
public class DropboxSyncService extends IntentService {

    private static final String TAG = DropboxSyncService.class.getSimpleName();
    public static final String ACTION_DELETE_BACKUP = "com.aokp.backup.DropboxSyncService.ACTION_DELETE_BACKUP";
    public static final String ACTION_UNLINK = "com.aokp.backup.DropboxSyncService.ACTION_UNLINK";

    private DbxAccountManager mDbxAcctMgr;

    private DbxFileSystem mDbxFs;

    @SuppressLint("NewApi")
    public DropboxSyncService() {
        super(DropboxSyncService.class.getSimpleName());
    }

    /*

    Dropbox sync support

        DbFs will look like this:
        root/
            currentdevice/
                backup.zip
                backup1.zip
            otherdevice/
                backup.zip
            tabletdevice/
                backup.zip

     */

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void refresh() {
        Log.d(TAG, "sending refresh!");
        sendBroadcast(RefreshReceiver.getBroadcastIntent());
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (!BuildConfig.DROPBOX_ENABLED) {
            return;
        }

        File backupDir = Tools.getBackupDirectory(this);

        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(),
                getString(R.string.dropbox_app_key), getString(R.string.dropbox_app_secret));


        if (!mDbxAcctMgr.hasLinkedAccount()) {
            return;
        }

        if (ACTION_DELETE_BACKUP.equals(intent.getAction())) {
            String backup = intent.getStringExtra("backup");

            try {
                dbxFs().delete(new DbxPath(DropboxUtils.PATH_DEVICE, backup));

            } catch (DbxException e) {
                Log.e(TAG, "DbxEception: " + e.getMessage(), e);
            } finally {
                refresh();
            }

        } else if (ACTION_UNLINK.equals(intent.getAction())) {

            try {
                mDbxAcctMgr.unlink();
            } finally {
                refresh();
            }

        } else {

            try {
                // wait for first sync
                dbxFs().awaitFirstSync();
                if (!dbxFs().exists(DropboxUtils.PATH_DEVICE)) {


                    // first sync for this device; just upload current backups!
                    dbxFs().createFolder(DropboxUtils.PATH_DEVICE);

                }

                dbxFs().syncNowAndWait();

                // next make sure all local files are up to date.
                List<DbxFileInfo> dbxFileInfos = dbxFs().listFolder(DropboxUtils.PATH_DEVICE);
                for (DbxFileInfo dbxFileInfo : dbxFileInfos) {
                    // copy to backup dir if it's newer.
                    File backupZip = new File(backupDir, dbxFileInfo.path.getName());

                    // always sync if it's old or not.
                    if (!DropboxUtils.isBackupSyncedToDropbox(mDbxAcctMgr, backupZip)) {
                        // db has newer version of backup!
                        DbxFile dbxFile = dbxFs().open(dbxFileInfo.path);

                        FileUtils.copyInputStreamToFile(new BufferedInputStream(dbxFile.getReadStream()), backupZip);
                        dbxFile.close();

                        Log.d(TAG, "copied dropbox/" + dbxFile.getPath().getName() + " to local storage");

                        refresh();
                        //copied to local file
                    } else {
                        Log.d(TAG, "local file: " + backupZip.getName() + " is newer than dropbox version, not downloading it");
                    }
                }

                // only copy new backups to dbx
                File[] backups = backupDir.listFiles(Tools.getBackupFileFilter(this, false));
                for (File backupFile : backups) {

                    if (!DropboxUtils.isBackupSyncedToDropbox(mDbxAcctMgr, backupFile)) {

                        DbxPath dbxPath = new DbxPath(DropboxUtils.PATH_DEVICE, backupFile.getName());
                        if (dbxFs().exists(dbxPath)) {
                            // they are different sizes, and the newest one should be stored locally already.
                            dbxFs().delete(dbxPath);

                            dbxFs().syncNowAndWait();
                        }

                        DbxFile dbxFile = dbxFs().create(dbxPath);
                        long bytesWritten = FileUtils.copyFile(backupFile, dbxFile.getWriteStream());
                        dbxFile.close();
                        Log.d(TAG, "wrote " + bytesWritten + " bytes to dropbox/" + dbxFile.getPath().getName());
                        refresh();
                    } else {

                        Log.d(TAG, "no need to upload " + backupFile.getName() + " to dropbox");

                    }
                }
            } catch (DbxException e) {
                Log.e(TAG, "DbxEception: " + e.getMessage(), e);
            } catch (IOException e) {
                Log.e(TAG, "IOException!", e);
            } finally {
                refresh();
            }
        }
    }

    private DbxFileSystem dbxFs() {
        if (mDbxFs == null) {
            try {
                mDbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            } catch (Unauthorized unauthorized) {
                Log.e(TAG, "Unauthorized to sync!", unauthorized);
                return null;
            }
        }
        return mDbxFs;
    }
}
