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

package com.aokp.backup.restore;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.aokp.backup.categories.ICSCategories;
import com.aokp.backup.util.SVal;
import com.aokp.backup.util.Tools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public abstract class Restore {

    public static final String TAG = "Restore";

    Context mContext;

    HashMap<String, SVal> settingsFromFile;
    String name;

    public static final int ERROR_IOEXCEPTION = 1;
    public static final int ERROR_RESTORE_UNSUPPORTED = 2;
    public static final int ERROR_NOT_AOKP = 3;
    public static final int ERROR_XOS = 4;

    public Restore(Context c) {
        mContext = c;
    }

    public int restoreSettings(String name, boolean[] catsToRestore) {
        this.name = name;

        try {
            if (!okayToRestore())
                return ERROR_RESTORE_UNSUPPORTED;
        } catch (NumberFormatException nfe) {
            return ERROR_NOT_AOKP;
        }

        try {
            readRestore();
        } catch (IOException e) {
            e.printStackTrace();
            return ERROR_IOEXCEPTION;
        }
        for (int i = 0; i < catsToRestore.length; i++) {
            if (catsToRestore[i]) {
                restoreSettings(i);
            }
        }
        return 0;
    }
    
    public abstract boolean okayToRestore();

    protected int getBackupVersion() throws IOException {
        String dir = new File(Tools.getBackupDirectory(mContext, name), "aokp.version")
                .getAbsolutePath();
        FileInputStream fstream = new FileInputStream(dir);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        while ((strLine = br.readLine()) != null) {
            return Integer.parseInt(strLine);
        }
        in.close();
        return Integer.MAX_VALUE;
    }

    protected void readRestore() throws IOException {
        String dir = new File(Tools.getBackupDirectory(mContext, name), "settings.cfg")
                .getAbsolutePath();
        settingsFromFile = new HashMap<String, SVal>();
        FileInputStream fstream = new FileInputStream(dir);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        while ((strLine = br.readLine()) != null) {
            // System.out.println(strLine);
            int split = strLine.indexOf("=");
            String setting = strLine.substring(0, split);
            String value = strLine.substring(split + 1, strLine.length());
            settingsFromFile.put(setting, new SVal(setting, value));

        }
        in.close();
    }

    private void restoreSettings(int category) {
        String[] settingsArray = new ICSCategories().getSettingsCategory(mContext, category);

        if (settingsArray == null) {
            Log.w(TAG, "couldn't find array of settings for category: "
                    + category);
            return;
        }

        for (String s : settingsArray) {
            SVal settingToRestore = settingsFromFile.get(s);
            if (!shouldHandleSpecialCase(s) && settingsFromFile.containsKey(s)) {
                restoreSetting(settingToRestore);
            }
        }
    }

    /**
     * Every setting should be checked through this function. It will check the
     * setting and perform any special actions for this specific setting.
     * 
     * @param setting setting name that needs will be checked
     * @return whether a special case was handled
     */
    public abstract boolean shouldHandleSpecialCase(String setting);

    protected boolean restoreSetting(String setting, String value, boolean secure) {
        // Log.d(TAG, "restoring: " + setting + " val: "
        // + value);
        // return true;
        if (secure)
            return Settings.Secure.putString(mContext.getContentResolver(),
                    setting, value);
        else
            return Settings.System.putString(mContext.getContentResolver(),
                    setting, value);

    }

    public boolean restoreSetting(SVal setting) {
        return restoreSetting(setting.getRealSettingString(), setting.getVal(), setting.isSecure());
    }
}
