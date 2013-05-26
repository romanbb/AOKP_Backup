/*
 * Copyright (C) 2012 Roman Birg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aokp.backup.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.aokp.backup.util.SVal;
import com.aokp.backup.util.Tools;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public abstract class Backup {

    public static final String TAG = "Backup";

    private int NUM_CATS;

    Context mContext;
    ArrayList<ArrayList<SVal>> backupValues = new ArrayList<ArrayList<SVal>>(NUM_CATS);
    boolean[] catsToBackup = new boolean[NUM_CATS];

    ArrayList<SVal> currentSVals;

    final String mName;
    final File mBackupDir;

    public Backup(Context c, boolean[] categories, String name) {
        mContext = c;
        mName = name;
        catsToBackup = categories;
        mBackupDir = Tools.getBackupDirectory(mContext, mName);
    }

//    public Backup(Context c, String name, File fromZip) {
//        // instantiate object expecting to restore it from a zip
//        this(c, new boolean[0], name);
//        restoreBackupFromZip(fromZip, new File(mBackupDir, name));
//    }

    public boolean backupSettings() {

        if (mBackupDir.exists()) {
            Shell.SU.run("rm -r " + mBackupDir.getAbsolutePath());
        }
        mBackupDir.mkdirs();

        for (int i = 0; i < catsToBackup.length; i++) {
            backupValues.add(i, new ArrayList<SVal>());
            if (catsToBackup[i]) {
                backupSettings(i);
            }
        }
        return writeBackupSetings();
    }

    /**
     * Extract backup from zip and overwrite this backup's settings
     *
     * @param zip file to restore from
     * @return whether the operation was successful
     */
    public static boolean restoreBackupFromZip(File zip, File destination) {
        if (!zip.exists()) {
            Log.d(TAG, "restoreFromBackupZip(zip) zip doesn't exist");
            return false;
        }

        if (!destination.exists()) {
            Log.d(TAG, "restoreFromBackupZip(zip) mBackupDir doesn't exist");
            return false;
        }

        return false;
    }

    /**
     * Convert a current backup object(folder) into a ZIP file
     *
     * @return returns the file where the zip of the backup is located, null if
     *         the operation failed
     */
    public static File zipBackup(final Context c, final Backup backup) {

        File zip = new File(Tools.getBackupDirectory(c), backup.mName + ".zip");
        // zip up the folder
        try {
                Tools.zip(backup.mBackupDir, zip);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return zip;
    }

    protected abstract String[] getSettingsCategory(int categoryIndex);

    private void backupSettings(int category) {
        ContentResolver resolver = mContext.getContentResolver();
        currentSVals = new ArrayList<SVal>();
        String[] settings = getSettingsCategory(category);

        if (settings == null) {
            Log.w(TAG, "couldn't find array of settings for category: "
                    + category);
            return;
        }

        for (String setting : settings) {
            if (!shouldHandleSpecialCase(setting)) {
                if (setting.startsWith("secure.")) {
                    try {
                        String val = Settings.Secure.getString(resolver, setting);
                        if (val != null)
                            currentSVals.add(new SVal(setting, val));
                    } catch (Exception e) {
                        Log.e(TAG, "couldn't restore: " + setting);
                    }
                } else {
                    try {
                        String val = Settings.System.getString(resolver, setting);
                        if (val != null)
                            currentSVals.add(new SVal(setting, val));
                    } catch (Exception e) {
                        Log.e(TAG, "couldn't restore: " + setting);
                    }
                }
            }
        }
        backupValues.add(category, currentSVals);
    }

    public boolean writeBackupSetings() {
        StringBuilder output = new StringBuilder();
        for (ArrayList<SVal> array : backupValues) {
            for (SVal pair : array) {
                output.append(pair.toString() + "\n");
            }
        }

        File backup = new File(mBackupDir, "settings.cfg");
        Tools.writeFileToSD(output.toString(), backup);

        Tools.writeFileToSD(Tools.getAOKPGooVersion().toString(),
                new File(Tools.getBackupDirectory(mContext, mName),
                        "aokp.version"));

        return true;
    }

    public abstract boolean shouldHandleSpecialCase(String setting);

}
