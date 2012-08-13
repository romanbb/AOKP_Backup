
package com.aokp.backup.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;

import com.aokp.backup.SVal;
import com.aokp.backup.ShellCommand;
import com.aokp.backup.Tools;
import com.aokp.backup.categories.ICSCategories;

import java.io.File;

public class ICSBackup extends Backup {

    public ICSBackup(Context c, boolean[] categories) {
        super(c, categories);

    }

    @Override
    protected String[] getSettingsCategory(int categoryIndex) {
        return new ICSCategories().getSettingsCategory(mContext, categoryIndex);
    }

    @Override
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
