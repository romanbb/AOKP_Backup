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

package com.aokp.backup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;

public class Restore {

    public static final String TAG = "Restore";

    Context mContext;

    HashMap<String, SVal> settingsFromFile;
    String name;

    public static final int ERROR_IOEXCEPTION = 1;
    public static final int ERROR_RESTORE_UNSUPPORTED = 2;
    public static final int ERROR_NOT_AOKP = 3;

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

    public boolean okayToRestore() {
        int minimumGooVersion;
        try {
            minimumGooVersion = getBackupVersion();
        } catch (IOException e) {
            e.printStackTrace();
            return true; // TODO handle this better!
        }
        final int maximumGooVersion = 19; // milestone 6

        int currentVersion = -1;
        currentVersion = Integer.parseInt(Tools.getAOKPVersion());

        if (currentVersion <= maximumGooVersion && currentVersion >= minimumGooVersion)
            return true;

        return false;
    }

    private int getBackupVersion() throws IOException {
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

    private void readRestore() throws IOException {
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
        Resources res = mContext.getResources();
        String[] settingsArray = null;
        switch (category) {
            case Categories.CAT_GENERAL_UI:
                settingsArray = res.getStringArray(R.array.cat_general_ui);
                break;
            case Categories.CAT_NAVIGATION_BAR:
                settingsArray = res.getStringArray(R.array.cat_navigation_bar);
                break;
            case Categories.CAT_LED:
                settingsArray = res.getStringArray(R.array.cat_led);
                break;
            case Categories.CAT_LOCKSCREEN_OPTS:
                settingsArray = res.getStringArray(R.array.cat_lockscreen);
                break;
            case Categories.CAT_POWER_MENU_OPTS:
                settingsArray = res.getStringArray(R.array.cat_powermenu);
                break;
            case Categories.CAT_POWER_SAVER:
                settingsArray = res.getStringArray(R.array.cat_powersaver);
                break;
            case Categories.CAT_SB_BATTERY:
                settingsArray = res.getStringArray(R.array.cat_statusbar_battery);
                break;
            case Categories.CAT_SB_CLOCK:
                settingsArray = res.getStringArray(R.array.cat_statusbar_clock);
                break;
            case Categories.CAT_SB_GENERAL:
                settingsArray = res.getStringArray(R.array.cat_statusbar_general);
                break;
            case Categories.CAT_SB_SIGNAL:
                settingsArray = res.getStringArray(R.array.cat_statusbar_signal);
                break;
            case Categories.CAT_SB_TOGGLES:
                settingsArray = res.getStringArray(R.array.cat_statusbar_toggles);
                break;
            case Categories.CAT_WEATHER:
                settingsArray = res.getStringArray(R.array.cat_weather);
                break;
        }

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

    public boolean shouldHandleSpecialCase(String setting) {

        String value = "";
        if (settingsFromFile.containsKey(setting))
            value = settingsFromFile.get(setting).val;

        if (setting.equals("disable_boot_animation") && value.equals("1")) {
            if (new File("/system/media/bootanimation.zip").exists()) {
                new ShellCommand().su
                        .run("mv /system/media/bootanimation.zip /system/media/bootanimation.unicorn");
            }
            return true;
        } else if (setting.equals("disable_boot_audio") && value.equals("1")) {
            if (new File("/system/media/boot_audio.mp3").exists()) {
                new ShellCommand().su
                        .run("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
            }

            return true;
        } else if (setting.equals("disable_bug_mailer") && value.equals("1")) {
            if (new File("/system/bin/bugmailer.sh").exists()) {
                new ShellCommand().su
                        .run("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.unicorn");
            }

            return true;
        } else if (setting.equals("navigation_bar_icons")) {
            File outDir = Tools.getBackupDirectory(mContext, name);
            for (int i = 0; i < 5; i++) {
                // String iconIntent = "navigation_custom_app_intent_" + i;
                // if (settingsFromFile.containsKey(iconIntent))
                // restoreSetting(settingsFromFile.get(iconIntent));
                //
                // String iconLPIntent = "navigation_longpress_app_intent_" + i;
                // if (settingsFromFile.containsKey(iconLPIntent))
                // restoreSetting(settingsFromFile.get(iconLPIntent));

                // navigation_custom_app_icon_0
                String settingName = "navigation_custom_app_icon_" + i;
                String iconName = "navbar_icon_" + i + ".png";
                Log.i(TAG, iconName);
                new ShellCommand().su
                        .runWaitFor("rm -f /data/data/com.aokp.romcontrol/files/" + iconName);
                if (settingsFromFile.containsKey(settingName)) {
                    String cmd = "cp " + outDir.getAbsolutePath() + "/" + iconName
                            + " /data/data/com.aokp.romcontrol/files/";
                    new ShellCommand().su.runWaitFor(cmd);
                    restoreSetting(settingsFromFile.get(settingName));
                } else {
                    restoreSetting(settingName, "", false);
                }

            }
            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();

            new ShellCommand().su
                    .run("rm -f /data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg");
            new ShellCommand().su
                    .run("cp " + outDir + "/lockscreen_wallpaper.jpg"
                            + " /data/data/com.aokp.romcontrol/files/");

            return true;
        } else if (setting.equals("lockscreen_icons")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            for (int i = 0; i < 8; i++) {
                // String iconIntent = "lockscreen_custom_app_intent_" + i;
                // if (settingsFromFile.containsKey(iconIntent))
                // restoreSetting(settingsFromFile.get(iconIntent));

                String settingName = "lockscreen_custom_app_icon_" + i;
                File iconToRestore = new File(outDir, settingName);

                new ShellCommand().su
                        .run("rm -f /data/data/com.aokp.romcontrol/files/" + settingName + ".*");
                if (settingsFromFile.containsKey(settingName)) {
                    new ShellCommand().su
                            .run("cp " + iconToRestore.getAbsolutePath() + ".* "
                                    + "/data/data/com.aokp.romcontrol/files/");
                    restoreSetting(settingsFromFile.get(settingName));
                } else {
                    restoreSetting(settingName, "", false);
                }

            }
            return true;
        } else if (setting.equals("private_weather_prefs")) {

            return true;
        }

        return false;
    }

    private boolean restoreSetting(String setting, String value, boolean secure) {
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
        return restoreSetting(setting.getRealSettingString(), setting.val, setting.isSecure());
    }
}
