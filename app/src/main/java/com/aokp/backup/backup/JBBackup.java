
package com.aokp.backup.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import com.aokp.backup.categories.JBCategories;
import com.aokp.backup.util.SVal;
import com.aokp.backup.util.Tools;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.util.List;

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
                File iconFile = new File("/data/data/com.aokp.romcontrol/files/navbar_icon_" + i + ".png");
                if (iconValue != null && iconFile.exists()) {
                    // add the value to the file so we know to restore the default icon or something
                    currentSVals.add(new SVal(iconSetting, iconValue));
                    if (iconValue.length() > 0) {
                        // copy if there's an actual image there
                        String cmd = "cp '" + iconFile.getAbsolutePath() + "' '" + outDir + "'";
                        Shell.SU.run(cmd);
                    }
                }
            }

            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            File wallpaper = new File("/data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg");
            if (wallpaper.exists())
                Shell.SU.run("cp '" + wallpaper.getAbsolutePath() + "' '"
                        + outDir + "'");
            return true;
        } else if (setting.equals("notification_wallpaper")) {
            File wallpaper = new File("/data/data/com.aokp.romcontrol/files/notification_wallpaper.jpg");
            if (wallpaper.exists())
                Shell.SU.run("cp '" + wallpaper + "' '"
                        + outDir + "'");
            return true;
        } else if (setting.equals("lockscreen_icons")) {
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 8; i++) {
                String set = "lockscreen_custom_app_icon_" + i;
                String val = Settings.System.getString(resolver, set);
                File iconFile = new File("/data/data/com.aokp.romcontrol/files/lockscreen_icon_" + i + ".png");
                if (val != null) {
                    currentSVals.add(new SVal(set, val));
                    File f = new File(Uri.parse(val).getPath());
                    if (f.exists()) {
                        Shell.SU.run("cp '" + iconFile + "' '" + outDir + "'");
                    }
                }
            }
            return true;
        } else if (setting.equals("rc_prefs")) {
            final String[] xmlFiles = {
                    "WeatherServicePreferences.xml",
                    "_has_set_default_values.xml",
                    "aokp_weather.xml",
                    "com.aokp.romcontrol_preferences.xml",
                    "vibrations.xml"
            };
            for (String xmlName : xmlFiles) {
                File xml = new File("/data/data/com.aokp.romcontrol/shared_prefs/" + xmlName);
                if (xml.exists()) {
                    List<String> result = Shell.SU.run("cp '" + xml.getAbsolutePath() + "' '" + outDir + xml.getName() + "'");
                    if (result != null) {
//                        Log.e(TAG, "run success");
                    } else {
                        Log.e(TAG, "error backing up: " + xmlName);
                    }
                }
            }
            return true;
        }

        return false;
    }
}
