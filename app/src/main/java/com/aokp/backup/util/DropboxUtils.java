package com.aokp.backup.util;

import android.os.Build;
import android.util.Log;
import com.aokp.backup.backup.Backup;
import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by roman on 6/16/13.
 */
public class DropboxUtils {

    private static final String TAG = DropboxUtils.class.getSimpleName();

    public static final DbxPath PATH_DEVICE = new DbxPath(DbxPath.ROOT, Build.DEVICE);

    public static boolean isBackupSyncedToDropbox(DbxAccountManager dbxAccountManager, Backup backup) throws DbxException {
        boolean ret = false;

        if (dbxAccountManager != null && dbxAccountManager.hasLinkedAccount()) {
            DbxAccount account = dbxAccountManager.getLinkedAccount();
            if (account != null && backup != null) {

                DbxFileSystem dbxfs = DbxFileSystem.forAccount(account);

                if (dbxfs != null && backup != null && !backup.isOldStyleBackup()) {

                    DbxPath path = new DbxPath(DropboxUtils.PATH_DEVICE, backup.getZipFile().getName());
                    if (dbxfs.exists(path)) {
                        DbxFileInfo fileInfo = dbxfs.getFileInfo(path);
                        ret = FileUtils.sizeOf(backup.getZipFile()) == fileInfo.size;
                    }
                } else {
                    Log.d(TAG, "dbxfs or backup was null");
                }
            }

        }

        return ret;
    }


    public static boolean isBackupSyncedToDropbox(DbxAccountManager dbxAccountManager, File backup) throws DbxException {
        boolean ret = false;

        if (dbxAccountManager != null && dbxAccountManager.hasLinkedAccount()) {
            DbxAccount account = dbxAccountManager.getLinkedAccount();

            if (account != null && backup != null && backup.exists()) {

                DbxFileSystem dbxfs = DbxFileSystem.forAccount(account);

                if (dbxfs != null && !backup.isDirectory()) {

                    DbxPath path = new DbxPath(DropboxUtils.PATH_DEVICE, backup.getName());
                    if (dbxfs.exists(path)) {
                        DbxFileInfo fileInfo = dbxfs.getFileInfo(path);
                        ret = FileUtils.sizeOf(backup) == fileInfo.size;
                    }
                } else {
                    Log.d(TAG, "dbxfs or backup was null");
                }
            }
        }
        return ret;
    }
}
