
package com.aokp.backup;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import com.aokp.backup.backup.Backup;
import com.aokp.backup.backup.BackupFactory;
import com.aokp.backup.util.Tools;
import com.squareup.otto.Bus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AOKPBackup extends Application {

    static final String TAG = "AOKP Backup";

    List<Backup> mBackups;

    static Bus sBus;

    @Override
    public void onCreate() {
        super.onCreate();
        sBus = new Bus();
        mBackups = new ArrayList<Backup>();
    }

    public static Bus getBus() {
        return sBus;
    }

    public List<Backup> findBackups() {
        mBackups.clear();

        File backupDir = Tools.getBackupDirectory(this);

        // This filter only returns directories

        File[] files = backupDir.listFiles(Tools.getBackupFileFilter(this));
        if (files == null) {
            Log.d(TAG, "no backups found");
        } else {
            for (int i = 0; i < files.length; i++) {
                try {
                    mBackups.add(BackupFactory.fromZipOrDirectory(this, files[i]));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return mBackups;
    }

    public boolean isAOKPVersionSupported() {
        if (Tools.getAOKPGooVersion() > 0) {
            return true;
        }
        if (Tools.getROMVersion().contains("aokp")) {
            return true;
        } else {
            Log.e(TAG, "ROM version: " + Tools.getROMVersion());
        }


        return false;
    }

    public boolean isAndroidVersionSupported() {
        switch (Tools.getAndroidVersion()) {
            case Build.VERSION_CODES.JELLY_BEAN_MR1:
            case Build.VERSION_CODES.JELLY_BEAN:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                return true;
        }

        return false;
    }

}
