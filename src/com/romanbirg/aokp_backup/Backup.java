
package com.romanbirg.aokp_backup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;

public class Backup {

    public static final String TAG = "Backup";

    // categories
    public static final int CAT_GENERAL_UI = 0;
    public static final int CAT_NAVIGATION_BAR = 1;
    public static final int CAT_LOCKSCREEN_OPTS = 2;
    public static final int CAT_POWER_MENU_OPTS = 3;
    public static final int CAT_WEATHER = 4;

    Context mContext;
    ArrayList<ArrayList<SVal>> backupValues = new ArrayList<ArrayList<SVal>>();
    boolean[] catsToBackup = new boolean[5];

    public Backup(Context c, boolean[] categories) {
        mContext = c;
        catsToBackup = categories;
    }

    public void backupSettings() {
        for (int i = 0; i < catsToBackup.length; i++) {
            if (catsToBackup[i]) {
                backupSettings(i);
            }
        }
    }

    private void backupSettings(int category) {
        Resources res = mContext.getResources();
        ContentResolver resolver = mContext.getContentResolver();
        ArrayList<SVal> vals = new ArrayList<SVal>();
        String[] settings = null;
        switch (category) {
            case CAT_GENERAL_UI:
                settings = res.getStringArray(R.array.cat_general_ui);
        }

        if (settings == null) {
            Log.e(TAG, "couldn't find array of settings for category: "
                    + category);
            return;
        }

        for (String setting : settings) {
            if (!shouldHandleSpecialCase(setting)) {
                String val = Settings.System.getString(resolver, setting);
                if (val != null)
                    vals.add(new SVal(setting, val));
            }
        }
        backupValues.add(category, vals);
    }

    public boolean writeBackupSetings(String backupName) {
        StringBuilder output = new StringBuilder();

        for (ArrayList<SVal> array : backupValues) {
            for (SVal pair : array) {
                output.append(pair.setting + "=" + pair.val + "\n");
            }
            output.append("\n");
        }

        File dir = new File(mContext.getExternalFilesDir(null), backupName);
        File backup = new File(dir, "");
        Writer outWriter;
        try {
            outWriter = new BufferedWriter(new FileWriter(backup));
            outWriter.write(output.toString());
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean shouldHandleSpecialCase(String setting) {
        if (setting.equals("disable_boot_animation")) {

            return true;
        } else if (setting.equals("disable_boot_audio")) {

            return true;
        } else if (setting.equals("disable_bug_mailer")) {

            return true;
        }

        return false;
    }

    public static class SVal {
        String setting;
        String val;

        public SVal(String setting, String val) {
            this.setting = setting;
            this.val = val;
        }

        public String toString() {
            return setting + "=" + val;
        }
    }

}
