
package com.aokp.backup.restore;

import android.content.Context;
import android.util.Log;

import com.aokp.backup.ShellCommand;
import com.aokp.backup.Tools;
import com.aokp.backup.ShellCommand.CommandResult;

import java.io.File;
import java.io.IOException;

public class JBRestore extends Restore {

    /**
     * minimum version to accept to restore jellybean settings. we don't want to
     * restore ICS settings on a JB ROM
     */
    private static final int MIN_JB_VERSION = 20;

    String rcUser;

    public JBRestore(Context c) {
        super(c);
        CommandResult cr = new ShellCommand().su
                .runWaitFor("ls -ld /data/data/com.aokp.romcontrol/ | awk '{print $3}'");
        rcUser = cr.stdout;
    }

    public boolean okayToRestore() {
        int minimumGooVersion = getAOKPBackupVersionInteger();
        if (minimumGooVersion == -1) {
            return false;
        }
        final int maximumGooVersion = 26;

        try {
            int currentVersion = Integer.parseInt(Tools.getAOKPVersion());

            if (currentVersion <= maximumGooVersion && currentVersion >= minimumGooVersion)
                return true;
        } catch (Exception e) {
        }
        return false;
    }

    public boolean shouldHandleSpecialCase(String setting) {

        File rcFilesDir = new File("/data/data/com.aokp.romcontrol/files/");
        if (!rcFilesDir.exists()) {
            new ShellCommand().su.runWaitFor("mkdir " + rcFilesDir.getAbsolutePath());
            Tools.chmodAndOwn(rcFilesDir, "0660", rcUser);
        }

        File rcPrefsDir = new File("/data/data/com.aokp.romcontrol/shared_prefs/");
        if (!rcPrefsDir.exists()) {
            new ShellCommand().su.runWaitFor("mkdir " + rcPrefsDir.getAbsolutePath());
            Tools.chmodAndOwn(rcPrefsDir, "0660", rcUser);
        }

        String value = "";
        if (settingsFromFile.containsKey(setting))
            value = settingsFromFile.get(setting).getVal();

        if (setting.equals("disable_boot_animation") && value.equals("1")) {
            if (new File("/system/media/bootanimation.zip").exists()) {
                new ShellCommand().su
                        .runWaitFor("mv /system/media/bootanimation.zip /system/media/bootanimation.unicorn");
            }
            return true;
        } else if (setting.equals("disable_boot_audio") && value.equals("1")) {
            if (new File("/system/media/boot_audio.mp3").exists()) {
                new ShellCommand().su
                        .runWaitFor("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
            }

            return true;
        } else if (setting.equals("disable_bug_mailer") && value.equals("1")) {
            if (new File("/system/bin/bugmailer.sh").exists()) {
                new ShellCommand().su
                        .runWaitFor("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.unicorn");
            }

            return true;
        } else if (setting.equals("navigation_bar_icons")) {
            File outDir = Tools.getBackupDirectory(mContext, name);
            for (int i = 0; i < 7; i++) {
                String settingName = "navigation_custom_app_icon_" + i;
                String iconName = "navbar_icon_" + i + ".png";
                File source = new File(outDir, iconName);
                File target = new File(rcFilesDir, iconName);

                // delete the current icon since we're restoring some
                new ShellCommand().su.runWaitFor("rm " + target.getAbsolutePath());
                if (settingsFromFile.containsKey(settingName)) {
                    new ShellCommand().su.runWaitFor("cp " + source.getAbsolutePath() + " "
                            + target.getAbsolutePath());
                    Tools.chmodAndOwn(target, "0660", rcUser);
                    restoreSetting(settingsFromFile.get(settingName));
                } else {
                    restoreSetting(settingName, "", false);
                }

            }
            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            File source = new File(outDir, "lockscreen_wallpaper.jpg");
            File target = new File(rcFilesDir, "lockscreen_wallpaper.jpg");
            new ShellCommand().su.runWaitFor("rm " + target.getAbsolutePath());
            new ShellCommand().su.runWaitFor("cp " + source.getAbsolutePath() + " "
                    + target.getAbsolutePath());
            Tools.chmodAndOwn(target, "0660", rcUser);

            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            File source = new File(outDir, "notification_wallpaper.jpg");
            File target = new File(rcFilesDir, "notification_wallpaper.jpg");
            new ShellCommand().su.runWaitFor("rm " + target.getAbsolutePath());
            new ShellCommand().su.runWaitFor("cp " + source.getAbsolutePath() + " "
                    + target.getAbsolutePath());
            Tools.chmodAndOwn(target, "0660", rcUser);

            return true;
        } else if (setting.equals("lockscreen_icons")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            for (int i = 0; i < 8; i++) {
                String settingName = "lockscreen_custom_app_icon_" + i;
                File iconToRestore = new File(outDir, settingName);

                new ShellCommand().su
                        .runWaitFor("rm " + rcFilesDir.getAbsolutePath() + settingName + ".*");
                if (settingsFromFile.containsKey(settingName)) {
                    new ShellCommand().su
                            .runWaitFor("cp " + iconToRestore.getAbsolutePath() + ".* "
                                    + rcFilesDir.getAbsolutePath());
                    Tools.chmodAndOwn(iconToRestore, "0660", rcUser);
                    restoreSetting(settingsFromFile.get(settingName));
                } else {
                    restoreSetting(settingName, "", false);
                }

            }
            return true;
        } else if (setting.equals("rc_prefs")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            String[] xmlFiles = {
                    "WeatherServicePreferences.xml", "_has_set_default_values.xml",
                    "aokp_weather.xml", "com.aokp.romcontrol_preferences.xml", "vibrations.xml"
            };
            if (rcUser != null && !rcUser.isEmpty()) {
                for (String xmlName : xmlFiles) {
                    File xml = new File(rcPrefsDir, xmlName);
                    if (xml.exists()) {
                        // remove previous
                        new ShellCommand().su.runWaitFor("rm " + xml.getAbsolutePath());
                        // copy backed up file
                        new ShellCommand().su.runWaitFor("cp " + outDir + "/" + xml.getName() + " "
                                + xml.getAbsolutePath());
                        Tools.chmodAndOwn(xml, "0660", rcUser);
                    }
                }
            } else {
                Log.e(TAG, "Error getting RC user");
            }
            return true;
        }

        return false;
    }
}
