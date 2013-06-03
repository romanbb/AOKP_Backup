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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Old backup structure:
 * $backup_root/$name/
 * <p/>
 * <p/>
 * New backup structure
 * $backup_root/$name.zip
 */
public abstract class Backup {

    public static final String TAG = "Backup";
    private static final String SETTINGS_FILE = "settings.cfg";
    private static final String BACKUP_INFO_FILE = "aokp.backup";
    final Context mContext;
    @Deprecated
    ArrayList<ArrayList<SVal>> mBackupValuePairs;
    @Deprecated
    boolean[] mCategoriesToBackup;
    // meta
    String mName;
    long mBackupDate = -1;
    List<Integer> mCategoryFilter;
    List<SVal> mBackupValues;
    List<SVal> mSpecialCaseKeys;
    @Deprecated
    File mBackupDir;
    File mZip;

    // restore stuff
    File rcFilesDir = null;
    File rcPrefsDir = null;

    public boolean isOldStyleBackup() {
        return mOldStyleBackup;
    }

    private boolean mOldStyleBackup;

    public Backup(Context c, String name) {
        mContext = c;
        mName = name;
        init();
        readFromSystem();
    }

    @Deprecated
    public Backup(Context c, boolean[] categories, String name) {
        mContext = c;
    }

    public File getZipFile() {
        return mZip;
    }

    /**
     * From zip. Or you can try to send in a directory....
     */
    public Backup(Context c, File zip) throws IOException {
        mContext = c;
        mName = zip.getName();
        mZip = zip;
        init();
        readSettingsFromZip();
    }

    private void init() {
        mSpecialCaseKeys = new ArrayList<SVal>();
        mBackupDir = Tools.getBackupDirectory(mContext, mName);
        rcFilesDir = new File("/data/data/com.aokp.romcontrol/files/");
        rcPrefsDir = new File("/data/data/com.aokp.romcontrol/shared_prefs/");

        mBackupValues = new ArrayList<SVal>();
    }

    public boolean handleBackup() {
        if (mOldStyleBackup) {
            // set the date to now
            mBackupDate = System.currentTimeMillis();

            // remove the old files after done writing below
        }


        // grab meta stuff
        List<String> metaLines = new LinkedList<String>();
        if (mName != null) {
            metaLines.add("backup_name=" + FilenameUtils.getName(mName));
        }

        if (mBackupDate != -1) {
            metaLines.add("backup_date=" + mBackupDate);
        }

        if (mCategoryFilter != null && !mCategoryFilter.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (Integer category : mCategoryFilter) {
                builder.append(String.valueOf(category) + "|");
            }

            // get rid of that |
            if (String.valueOf(builder.charAt(builder.length() - 1)).equals("\\|")) {
                builder.deleteCharAt(builder.length() - 1);
            }

            metaLines.add(builder.toString());
        }

        // grab actual settings
        List<String> settingsLines = new LinkedList<String>();
        if (mBackupValues != null && !mBackupValues.isEmpty()) {
            for (SVal setting : mBackupValues) {
                settingsLines.add(setting.toString());
            }
        }

//        File cacheDir = new File(mContext.getCacheDir(), ".zipping");
        File cacheDir = Tools.getTempBackupDirectory(mContext, true);
        if (cacheDir.exists()) {
            FileUtils.deleteQuietly(cacheDir);
        }

        if (!cacheDir.mkdir()) {
            Log.e(TAG, "error creating temp zip dir!");
            return false;
        }

        try {
            // write files
            FileUtils.writeLines(new File(cacheDir, BACKUP_INFO_FILE), metaLines);
            FileUtils.writeLines(new File(cacheDir, SETTINGS_FILE), settingsLines);

            // zip!
            File zip = new File(Tools.getBackupDirectory(mContext), FilenameUtils.getName(mName) + ".zip");
            Tools.zip(cacheDir, zip);
            FileUtils.touch(zip);

            // delete cache dir
            FileUtils.deleteQuietly(cacheDir);

            if (mOldStyleBackup) {
                FileUtils.deleteQuietly(Tools.getBackupDirectory(mContext, mName));
            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Backup failed!", e);
            return false;
        }
    }

    public void readSettingsFromZip() throws IOException {
        mBackupValues.clear();
        mSpecialCaseKeys.clear();

        // zip?
        if (mZip != null && !mZip.isDirectory() && FilenameUtils.isExtension(mZip.getAbsolutePath(), "zip") && mZip.exists()) {

            // new backup vals

            ZipFile zip = new ZipFile(mZip);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            // new name
            mName = zip.getName();

            ZipEntry entry = null;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.isDirectory()) {
                    // skip?
                } else {
                    if (entry.getName().equalsIgnoreCase(SETTINGS_FILE)) {

                        /**
                         * read backup values
                         */
                        List<String> lines = IOUtils.readLines(zip.getInputStream(entry));
                        for (String strLine : lines) {
                            mBackupValues.add(new SVal(strLine));
                        }


                    } else if (entry.getName().equalsIgnoreCase(BACKUP_INFO_FILE)) {

                        /**
                         * read backup meta data
                         */
                        List<String> lines = IOUtils.readLines(zip.getInputStream(entry));
                        for (String line : lines) {

                            // assume it's an SVal
                            if (line.contains("=")) {

                                // look for stuff we might want
                                SVal val = new SVal(line);
                                if ("backup_date".equals(val.getKey())) {
                                    mBackupDate = Long.valueOf(val.getValue());
                                } else if ("backup_categories".equals(val.getKey())) {

                                    // TODO make sure this doesn't fail
                                    String[] cats = val.getValue().split("\\|");
                                    mCategoryFilter = new ArrayList<Integer>();
                                    for (String cat : cats) {
                                        mCategoryFilter.add(Integer.parseInt(cat));
                                    }
                                } else if ("backup_name".equals(val.getKey())) {

                                    mName = val.getValue();
                                }
                            }
                        }

                    }
                }
            }
        } else if (mZip.isDirectory()) {
            /*
            no zip? must be a non-zip backup (old style). let's read the settings.
            don't delete yet, delete when writing.
             */
            mOldStyleBackup = true;
            File f = new File(Tools.getBackupDirectory(mContext, mName), "settings.cfg");
            if (!f.exists()) {
                Log.e(TAG, "settings.cfg doesn't exist!");
            }

            List<String> lines = IOUtils.readLines(new FileInputStream(f));
            for (String strLine : lines) {
                mBackupValues.add(new SVal(strLine));
            }
            mBackupDate = f.lastModified();
        }


    }

    /**
     * Old version of readFromSystem(). Does nothing.
     *
     * @return Returns false. Does nothing.
     */
    @Deprecated
    public boolean backupSettings() {
        return false;
    }

    protected abstract String[] getSettingsCategory(int categoryIndex);

    /**
     * Will fill this backup object with values from the current system,
     * which settings are determined by the class that implements the abstract methods in this class
     */
    private void readFromSystem() {
        ContentResolver resolver = mContext.getContentResolver();
        for (int i = 0; i < getNumCats(); i++) {
            String[] settings = getSettingsCategory(i);
            for (String setting : settings) {
                if (!handleBackupSpecialCase(setting)) {
                    try {
                        String val = Settings.System.getString(resolver, setting);
                        if (val != null) {
                            mBackupValues.add(new SVal(setting, val));
                        } else
                            Log.e(TAG, "couldn't backup: " + setting);
                    } catch (Exception e) {
                        Log.e(TAG, "couldn't backup: " + setting, e);
                    }
                }
            }
        }
//        mBackupValuePairs.add(category, currentSVals);
    }

    public boolean deleteFromDisk() {
        if (mOldStyleBackup) {
            if (mBackupDir.exists() && mBackupDir.isDirectory()) {
                FileUtils.deleteQuietly(mBackupDir);
                return true;
            }
        } else {
            if (mZip != null && mZip.exists()) {
                mZip.delete();
                return true;
            }
        }
        return false;
    }

    /**
     * Old style of writing the backup. Writes separate backup files to a folder.
     *
     * @return
     */
    @Deprecated
    public boolean writeBackupSetings() {
        StringBuilder output = new StringBuilder();
        for (ArrayList<SVal> array : mBackupValuePairs) {
            for (SVal pair : array) {
                output.append(pair.toString() + "\n");
            }
        }

        File backup = new File(mBackupDir, "settings.cfg");
        Tools.writeFileToSD(output.toString(), backup);

//        Tools.writeFileToSD(Tools.getAOKPGooVersion().toString(),
//                new File(Tools.getBackupDirectory(mContext, mName),
//                        "aokp.version"));


        return true;
    }

    /**
     * Run this to ask the implementing backup class to handle the special KEY.
     * <p/>
     * Should set setting's value to 1 if succeeded. 0 if failed (and it handled the event)
     *
     * @param setting the KEY from the backup
     * @return If this method returns false, we can assume it's a key/value pair to be written to Settings.System
     */
    abstract boolean handleBackupSpecialCase(String setting);

    abstract boolean handleRestoreSpecialCase(SVal setting);

    /**
     * Get the number of categories to query the super class
     *
     * @return the number of categories (they start with 0, the count would give you the count just like an array)
     */
    public abstract int getNumCats();

    public abstract boolean okayToRestore();

    /**
     * Restore values to the current devices from this backup object
     */
    public void handleRestore() {
        if (mSpecialCaseKeys != null && !mSpecialCaseKeys.isEmpty()) {
            for (SVal specialSettingKey : mSpecialCaseKeys) {
                handleRestoreSpecialCase(specialSettingKey);
            }

            for (SVal settingToRestore : mBackupValues) {
                if (!restoreSetting(settingToRestore)) {
                    Log.e(TAG, "failed restoring setting: " + settingToRestore.getKey());
                }
            }

        } else {

            /**
             * Special cases are meshed in or there are none. Either way, check for them again just to be sure.
             */

            for (SVal settingToRestore : mBackupValues) {
                if (!handleRestoreSpecialCase(settingToRestore)) {
                    if (!restoreSetting(settingToRestore)) {
                        Log.e(TAG, "failed restoring setting: " + settingToRestore.getKey());
                    }
                } else {
                    Log.d(TAG, "handled special case while restoring with key: " + settingToRestore);
                }
            }
        }

    }

    /**
     * Write the sval to the system
     *
     * @param settingToRestore the system setting pair to write
     * @return whether it was written.
     */
    private boolean restoreSetting(SVal settingToRestore) {
        if (settingToRestore.isSecure()) {
            Log.e(TAG, "Not restoring (it's secure!)! Tried to restore secure setting: " + settingToRestore.getKey());
            return false;
        }

        return Settings.System.putString(mContext.getContentResolver(),
                settingToRestore.getValue(), settingToRestore.getKey());
    }

    public String getName() {
        return mName;
    }

    public Date getBackupDate() {
        return new Date(mBackupDate);
    }

    public List<Integer> getCategoryFilter() {
        return mCategoryFilter;
    }
}
