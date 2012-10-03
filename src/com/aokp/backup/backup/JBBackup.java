
package com.aokp.backup.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.aokp.backup.SVal;
import com.aokp.backup.ShellCommand;
import com.aokp.backup.ShellCommand.CommandResult;
import com.aokp.backup.Tools;
import com.aokp.backup.categories.JBCategories;

import java.io.File;

public class JBBackup extends Backup {

    String outDir;

    public JBBackup(Context c, boolean[] categories, String name) {
        super(c, categories, name);
        outDir = Tools.getBackupDirectory(mContext, mName).getAbsolutePath();
        if (!outDir.endsWith("/"))
            outDir += "/";

    }

    @Override
    protected String[] getSettingsCategory(int categoryIndex) {
        return new JBCategories().getSettingsCategory(mContext, categoryIndex);
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
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 7; i++) {
                String iconSetting = "navigation_custom_app_icon_" + i;
                String iconValue = Settings.System.getString(resolver, iconSetting);
                if (iconValue != null) {
                    if (iconValue.length() > 0) {
                        currentSVals.add(new SVal(iconSetting, iconValue));
                        String cmd = "cp /data/data/com.aokp.romcontrol/files/navbar_icon_" + i
                                + ".png " + outDir;
                        new ShellCommand().su
                                .runWaitFor(cmd);
                    }
                }
            }

            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            new ShellCommand().su
                    .runWaitFor("cp /data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg "
                            + outDir);
            return true;
        } else if (setting.equals("notification_wallpaper")) {
            new ShellCommand().su
                    .runWaitFor("cp /data/data/com.aokp.romcontrol/files/notification_wallpaper.jpg "
                            + outDir);
            return true;
        } else if (setting.equals("lockscreen_icons")) {
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 8; i++) {
                String set = "lockscreen_custom_app_icon_" + i;
                String val = Settings.System.getString(resolver, set);
                if (val != null) {
                    currentSVals.add(new SVal(set, val));
                    File f = new File(Uri.parse(val).getPath());
                    if (f.exists()) {
                        new ShellCommand().su
                                .runWaitFor("cp /data/data/com.aokp.romcontrol/files/lockscreen_icon_" + i
                                        + ".png " + outDir);
                    }
                }
            }
            return true;
        } else if (setting.equals("rc_prefs")) {
            final String[] xmlFiles = {
                    "WeatherServicePreferences.xml", "_has_set_default_values.xml",
                    "aokp_weather.xml", "com.aokp.romcontrol_preferences.xml", "vibrations.xml"
            };
            for (String xmlName : xmlFiles) {
                File xml = new File("/data/data/com.aokp.romcontrol/shared_prefs/" + xmlName);
                if (xml.exists()) {
                    String command = "cp " + xml.getAbsolutePath() + " " + outDir + xml.getName();
                    Log.e(TAG, command);
                    CommandResult cr = new ShellCommand().su.runWaitFor(command);
                    if(cr.success()) {
                        Log.e(TAG, "run success");
                    } else {
                        Log.e(TAG, "error: " + cr.stderr);
                    }
                }
            }
            return true;
        }

        return false;
    }
}
