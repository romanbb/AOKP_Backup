
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
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

public class Backup {

    public static final String TAG = "Backup";

    // categories

    private static int NUM_CATS = 12;

    Context mContext;
    ArrayList<ArrayList<SVal>> backupValues = new ArrayList<ArrayList<SVal>>(NUM_CATS);
    boolean[] catsToBackup = new boolean[NUM_CATS];

    ArrayList<SVal> currentSVals;

    String name = "***TEST***";

    public Backup(Context c, boolean[] categories) {
        mContext = c;
        catsToBackup = categories;
    }

    public boolean backupSettings(String name) {

        File dir = Tools.getBackupDirectory(mContext, name);
        if (dir.exists()) {
            new ShellCommand().su.runWaitFor("rm -r " + dir.getAbsolutePath());
        }
        dir.mkdirs();

        this.name = name;
        for (int i = 0; i < catsToBackup.length; i++) {
            backupValues.add(i, new ArrayList<SVal>());
            if (catsToBackup[i]) {
                backupSettings(i);
            }
        }
        return writeBackupSetings();
    }

    private void backupSettings(int category) {
        Resources res = mContext.getResources();
        ContentResolver resolver = mContext.getContentResolver();
        currentSVals = new ArrayList<SVal>();
        String[] settings = null;
        switch (category) {
            case Categories.CAT_GENERAL_UI:
                settings = res.getStringArray(R.array.cat_general_ui);
                break;
            case Categories.CAT_NAVIGATION_BAR:
                settings = res.getStringArray(R.array.cat_navigation_bar);
                break;
            case Categories.CAT_LED:
                settings = res.getStringArray(R.array.cat_led);
                break;
            case Categories.CAT_LOCKSCREEN_OPTS:
                settings = res.getStringArray(R.array.cat_lockscreen);
                break;
            case Categories.CAT_POWER_MENU_OPTS:
                settings = res.getStringArray(R.array.cat_powermenu);
                break;
            case Categories.CAT_POWER_SAVER:
                settings = res.getStringArray(R.array.cat_powersaver);
                break;
            case Categories.CAT_SB_BATTERY:
                settings = res.getStringArray(R.array.cat_statusbar_battery);
                break;
            case Categories.CAT_SB_CLOCK:
                settings = res.getStringArray(R.array.cat_statusbar_clock);
                break;
            case Categories.CAT_SB_GENERAL:
                settings = res.getStringArray(R.array.cat_statusbar_general);
                break;
            case Categories.CAT_SB_SIGNAL:
                settings = res.getStringArray(R.array.cat_statusbar_signal);
                break;
            case Categories.CAT_SB_TOGGLES:
                settings = res.getStringArray(R.array.cat_statusbar_toggles);
                break;
            case Categories.CAT_WEATHER:
                settings = res.getStringArray(R.array.cat_weather);
                break;
        }

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
                output.append(pair.setting + "=" + pair.val + "\n");
            }
        }

        File dir = Tools.getBackupDirectory(mContext, name);
        File backup = new File(dir, "settings.cfg");
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
            if (!new File("/system/media/bootanimation.zip").exists()) {
                currentSVals.add(new SVal(setting, "1"));
            }

            return true;
        } else if (setting.equals("disable_boot_audio")) {
            if (!new File("/system/media/boot_audio.mp3").exists()) {
                currentSVals.add(new SVal(setting, "1"));
            }

            return true;
        } else if (setting.equals("disable_bug_mailer")) {
            if (!new File("/system/bin/bugmailer.sh").exists()) {
                currentSVals.add(new SVal(setting, "1"));
            }

            return true;
        } else if (setting.equals("navigation_bar_icons")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 5; i++) {
                // String iconIntent = "navigation_custom_app_intent_" + i;
                // String iconIntentValue = Settings.System.getString(resolver,
                // iconIntent);
                // Log.i(TAG, "intent value: " + iconIntentValue);
                // if (iconIntentValue != null)
                // currentSVals.add(new SVal(iconIntent, iconIntentValue));
                //
                // String iconLongPressIntent =
                // "navigation_longpress_app_intent_" + i;
                // String iconLongPressIntentValue =
                // Settings.System.getString(resolver,
                // iconLongPressIntent);
                // if (iconLongPressIntentValue != null)
                // currentSVals.add(new SVal(iconLongPressIntent,
                // iconLongPressIntentValue));

                String iconSetting = "navigation_custom_app_icon_" + i;
                String iconValue = Settings.System.getString(resolver, iconSetting);
                if (iconValue != null) {
                    if (iconValue.length() > 0) {
                        currentSVals.add(new SVal(iconSetting, iconValue));
                        String cmd = "cp /data/data/com.aokp.romcontrol/files/navbar_icon_" + i
                                + ".png " + outDir + "/";
                        new ShellCommand().su
                                .runWaitFor(cmd);
                    }
                }
            }

            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();

            new ShellCommand().su
                    .run("cp /data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg "
                            + outDir);

            return true;
        } else if (setting.equals("lockscreen_icons")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 8; i++) {
                // String intentSetting = "lockscreen_custom_app_intent_" + i;
                // String intentValue = Settings.System.getString(resolver,
                // intentSetting);
                // if (intentValue != null)
                // currentSVals.add(new SVal(intentSetting, intentValue));

                String set = "lockscreen_custom_app_icon_" + i;
                String val = Settings.System.getString(resolver, set);
                if (val != null) {
                    currentSVals.add(new SVal(set, val));
                    File f = new File(Uri.parse(val).getPath());
                    if (f.exists()) {
                        new ShellCommand().su
                                .run("cp /data/data/com.aokp.romcontrol/files/lockscreen_icon_" + i
                                        + ".png " + outDir);
                    }
                }
            }

            return true;
        } else if (setting.equals("private_weather_prefs")) {

            return true;
        }

        return false;
    }

}
