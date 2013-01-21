
package com.aokp.backup.restore;

import android.content.Context;
import android.util.Log;
import com.aokp.backup.categories.ICSCategories;
import com.aokp.backup.util.Tools;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;

public class ICSRestore extends Restore {

    public ICSRestore(Context c) {
        super(c);
    }

    @Override
    public String[] getSettingsCategory(Context c, int cat) {
        return new ICSCategories().getSettingsCategory(c, cat);
    }

    public boolean okayToRestore() {
        int minimumGooVersion = getBackedupGooVersion();
        if (minimumGooVersion == -1) {
            return false;
        }
        final int maximumGooVersion = 19;

        try {
            int currentVersion = Tools.getAOKPGooVersion();

            if (currentVersion <= maximumGooVersion && currentVersion >= minimumGooVersion)
                return true;
        } catch (Exception e) {
        }
        return false;
    }

    public boolean shouldHandleSpecialCase(String setting) {

        String value = "";
        if (settingsFromFile.containsKey(setting))
            value = settingsFromFile.get(setting).getVal();

        if (setting.equals("disable_boot_animation") && value.equals("1")) {
            if (new File("/system/media/bootanimation.zip").exists()) {
                Shell.SU.run("mv /system/media/bootanimation.zip /system/media/bootanimation.unicorn");
            }
            return true;
        } else if (setting.equals("disable_boot_audio") && value.equals("1")) {
            if (new File("/system/media/boot_audio.mp3").exists()) {
                Shell.SU.run("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
            }

            return true;
        } else if (setting.equals("disable_bug_mailer") && value.equals("1")) {
            if (new File("/system/bin/bugmailer.sh").exists()) {
                Shell.SU.run("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.unicorn");
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
                Shell.SU.run("rm -f /data/data/com.aokp.romcontrol/files/" + iconName);
                if (settingsFromFile.containsKey(settingName)) {
                    String cmd = "cp " + outDir.getAbsolutePath() + "/" + iconName
                            + " /data/data/com.aokp.romcontrol/files/";
                    Shell.SU.run(cmd);
                    restoreSetting(settingsFromFile.get(settingName));
                } else {
                    restoreSetting(settingName, "", false);
                }

            }
            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();

            Shell.SU.run("rm -f /data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg");
            Shell.SU.run("cp " + outDir + "/lockscreen_wallpaper.jpg"
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

                Shell.SU.run("rm -f /data/data/com.aokp.romcontrol/files/" + settingName + ".*");
                if (settingsFromFile.containsKey(settingName)) {
                    Shell.SU.run("cp " + iconToRestore.getAbsolutePath() + ".* "
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
}
