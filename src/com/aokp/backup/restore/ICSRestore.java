
package com.aokp.backup.restore;

import android.content.Context;
import android.util.Log;

import com.aokp.backup.util.ShellCommand;
import com.aokp.backup.util.Tools;

import java.io.File;

public class ICSRestore extends Restore {

    public ICSRestore(Context c) {
        super(c);
    }

    public boolean okayToRestore() {
        int minimumGooVersion = getAOKPBackupVersionInteger();
        if (minimumGooVersion == -1) {
            return false;
        }
        final int maximumGooVersion = 19;

        try {
            int currentVersion = Integer.parseInt(Tools.getAOKPVersion());

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
}
