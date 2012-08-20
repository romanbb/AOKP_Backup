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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.aokp.backup.R;
import com.aokp.backup.SVal;
import com.aokp.backup.ShellCommand;
import com.aokp.backup.Tools;
import com.aokp.backup.R.array;
import com.aokp.backup.categories.Categories;

public abstract class Backup {

    public static final String TAG = "Backup";

    private int NUM_CATS;

    Context mContext;
    ArrayList<ArrayList<SVal>> backupValues = new ArrayList<ArrayList<SVal>>(NUM_CATS);
    boolean[] catsToBackup = new boolean[NUM_CATS];

    ArrayList<SVal> currentSVals;

    String mName;

    File mBackupDir;

    public Backup(Context c, boolean[] categories, String name) {
        mContext = c;
        mName = name;
        catsToBackup = categories;
        mBackupDir = Tools.getBackupDirectory(mContext, name);
    }

    public boolean backupSettings() {

        if (mBackupDir.exists()) {
            new ShellCommand().su.runWaitFor("rm -r " + mBackupDir.getAbsolutePath());
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
    
    public File zipBackup() {
        
        return null;
    }
    
    public void restoreBackupFromZip(File zip) {
        
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

        File dir = Tools.getBackupDirectory(mContext, mName);
        if (dir.exists()) {
            try {
                Tools.delete(dir);
            } catch (IOException e) {
                Log.d("AOKP.backup", "error deleting dir", e);
            }
        }
        dir.mkdirs();
        File backup = new File(dir, "settings.cfg");

        Tools.writeFileToSD(output.toString(), backup);

        Tools.writeFileToSD(Tools.getAOKPVersion(),
                new File(Tools.getBackupDirectory(mContext, mName),
                        "aokp.version"));

        return true;
    }

    public abstract boolean shouldHandleSpecialCase(String setting);

}
