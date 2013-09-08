package com.aokp.backup.backup;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import com.aokp.backup.R;
import com.aokp.backup.UnsupportedSDKVersionException;

import java.io.File;
import java.io.IOException;

/**
 * Created by roman on 6/2/13.
 */
public class BackupFactory {

    public static int getCategoryArrayResourceId() throws UnsupportedSDKVersionException {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            return R.array.categories;

        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN)
            return R.array.jbcategories;

        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
            return R.array.jbmr1_categories;

        else if (Build.VERSION.SDK_INT == VERSION_CODES.JELLY_BEAN_MR2)
            return R.array.jbmr2_categories;

        throw new UnsupportedSDKVersionException();
    }

    public static Backup getNewBackupObject(Context c, String backupName) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            return new ICSBackup(c, backupName);

        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN)
            return new JBBackup(c, backupName);

        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
            return new JBMR1Backup(c, backupName);

        else if (Build.VERSION.SDK_INT == VERSION_CODES.JELLY_BEAN_MR2)
            return new JBMR2Backup(c, backupName);

            return null;
    }

    public static Backup fromZipOrDirectory(Context c, File dirOrZip) throws IOException {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            return new ICSBackup(c, dirOrZip);
        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN)
            return new JBBackup(c, dirOrZip);
        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
            return new JBMR1Backup(c, dirOrZip);
        else if (Build.VERSION.SDK_INT == VERSION_CODES.JELLY_BEAN_MR2)
            return new JBMR2Backup(c, dirOrZip);

            return null;
    }


}
