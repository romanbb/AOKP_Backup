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
import com.aokp.backup.ParseHelpers;
import com.aokp.backup.util.SVal;
import com.aokp.backup.util.Tools;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import eu.chainfire.libsuperuser.Shell;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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
    String parseBackupId = null;
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
        } else {
            try {
                synchronized (ParseHelpers.sLock) {
                    FileUtils.deleteDirectory(destination);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // remove current settings and extract zip
        try {
            synchronized (ParseHelpers.sLock) {
                FileUtils.deleteDirectory(destination);
            }
        } catch (IOException e) {
            Log.d(TAG, "deleting backup directory", e);
            return false;
        }

        try {
            synchronized (ParseHelpers.sLock) {
                Tools.unzip(zip, destination);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
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
            synchronized (ParseHelpers.sLock) {
                Tools.zip(backup.mBackupDir, zip);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Log.i(TAG, "uploading to Parse");
        try {
            ParseFile f = new ParseFile(zip.getName(),
                    IOUtils.toByteArray(new FileInputStream(zip)));
            f.saveInBackground();

            final ParseObject b = new ParseObject("Backup");
            b.put("gooVersion", Tools.getAOKPGooVersion());
            b.put("zippedBackup", f);
            b.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    try {
                        String backupId = b.getObjectId();
                        ParseHelpers.getInstance(c).addId(backupId);
                        synchronized (ParseHelpers.sLock) {
                            Tools.writeFileToSD(backupId, new File(backup.mBackupDir, "id"));
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        // TODO that sux bro
                    }
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
                    String val = Settings.Secure.getString(resolver, setting);
                    if (val != null)
                        currentSVals.add(new SVal(setting, val));
                } else {
                    String val = Settings.System.getString(resolver, setting);
                    if (val != null)
                        currentSVals.add(new SVal(setting, val));
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
