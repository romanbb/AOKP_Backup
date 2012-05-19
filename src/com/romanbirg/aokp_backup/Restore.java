
package com.romanbirg.aokp_backup;

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

    public Restore(Context c) {
        mContext = c;
    }

    public boolean restoreSettings(String name, boolean[] catsToRestore) throws IOException {
        this.name = name;
        readRestore();
        for (int i = 0; i < catsToRestore.length; i++) {
            if (catsToRestore[i]) {
                restoreSettings(i);
            }
        }
        return true;
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
            String[] line = strLine.split("=");
            if (line.length > 1)
                settingsFromFile.put(line[0], new SVal(line[0], line[1]));
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
            if (!settingsFromFile.containsKey(s))
                continue;

            SVal settingToRestore = settingsFromFile.get(s);
            if (!shouldHandleSpecialCase(settingToRestore)) {
                restoreSetting(settingToRestore);
            }
        }
    }

    public boolean shouldHandleSpecialCase(SVal s) {
        String setting = s.setting;
        String value = s.val;

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
                String iconIntent = "navigation_custom_app_intent_" + i;
                if (settingsFromFile.containsKey(iconIntent))
                    restoreSetting(settingsFromFile.get(iconIntent));

                String iconLPIntent = "navigation_longpress_app_intent_" + i;
                if (settingsFromFile.containsKey(iconLPIntent))
                    restoreSetting(settingsFromFile.get(iconLPIntent));

                // navigation_custom_app_icon_0
                String settingName = "navigation_custom_app_icon_" + i;

                new ShellCommand().su
                        .run("rm -f /data/data/com.aokp.romcontrol/files/" + settingName + ".*");
                if (!settingsFromFile.containsKey(settingName)) {
                    restoreSetting(settingName, "", false);
                    continue;
                }

                new ShellCommand().su
                        .run("cp " + outDir.getAbsolutePath() + "/" + settingName
                                + ".* /data/data/com.aokp.romcontrol/files/");
                restoreSetting(settingsFromFile.get(settingName));
            }
            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();

            new ShellCommand().su
                    .run("rm -f /data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.*");
            new ShellCommand().su
                    .run("cp " + outDir + "/lockscreen_wallpaper.*"
                            + " /data/data/com.aokp.romcontrol/files/");

            return true;
        } else if (setting.equals("lockscreen_icons")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            for (int i = 0; i < 8; i++) {
                String iconIntent = "lockscreen_custom_app_intent_" + i;
                if (settingsFromFile.containsKey(iconIntent))
                    restoreSetting(settingsFromFile.get(iconIntent));

                String settingName = "lockscreen_custom_app_icon_" + i;
                File iconToRestore = new File(outDir, settingName);

                new ShellCommand().su
                        .run("rm -f /data/data/com.aokp.romcontrol/files/" + settingName + ".*");
                if (!settingsFromFile.containsKey(settingName)) {
                    restoreSetting(settingName, "", false);
                    continue;
                }

                new ShellCommand().su
                        .run("cp " + iconToRestore.getAbsolutePath() + ".* "
                                + "/data/data/com.aokp.romcontrol/files/");
                restoreSetting(settingsFromFile.get(settingName));

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
