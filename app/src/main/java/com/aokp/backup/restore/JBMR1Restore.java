
package com.aokp.backup.restore;

import android.content.Context;
import android.util.Log;
import com.aokp.backup.categories.JBMR1Categories;
import com.aokp.backup.util.Tools;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;

public class JBMR1Restore extends Restore {

    /**
     * minimum version to accept to restore jellybean settings. we don't want to
     * restore ICS settings on a JB ROM
     */
    private static final int MIN_JBMR1_VERSION = 27;

    public JBMR1Restore(Context c) {
        super(c);
    }

    @Override
    public String[] getSettingsCategory(Context c, int cat) {
        return new JBMR1Categories().getSettingsCategory(c, cat);
    }


    public boolean okayToRestore() {
        boolean result = false;

        if (Tools.getROMVersion().contains("aokp")) {
            result = true;
        }

        return result;
    }

    public boolean shouldHandleSpecialCase(String setting) {
        String value = "";
        if (settingsFromFile.containsKey(setting))
            value = settingsFromFile.get(setting).getVal();

        if (setting.equals("disable_boot_animation") && value.equals("1")) {
            if (new File("/system/media/bootanimation.zip").exists()) {
//                Shell.SU.run("mv /system/media/bootanimation.zip /system/media/bootanimation.unicorn");
            }
            return true;
        } else if (setting.equals("disable_boot_audio") && value.equals("1")) {
            if (new File("/system/media/boot_audio.mp3").exists()) {
//                Shell.SU.run("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
            }

            return true;
        } else if (setting.equals("disable_bug_mailer") && value.equals("1")) {
            if (new File("/system/bin/bugmailer.sh").exists()) {
//                Shell.SU.run("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.unicorn");
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
                if (settingsFromFile.containsKey(settingName)) {
                    Shell.SU.run("cp " + source.getAbsolutePath() + " " + target.getAbsolutePath());
                    Tools.chmodAndOwn(target, "0660", rcUser);
                    restoreSetting(settingsFromFile.get(settingName));
                } else {
                    Shell.SU.run("rm " + target.getAbsolutePath());
                    restoreSetting(settingName, "", false);
                }

            }
            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            File source = new File(outDir, "lockscreen_wallpaper.jpg");
            File target = new File(rcFilesDir, "lockscreen_wallpaper.jpg");
            Shell.SU.run("rm " + target.getAbsolutePath());
            Shell.SU.run("cp " + source.getAbsolutePath() + " "
                    + target.getAbsolutePath());
            Tools.chmodAndOwn(target, "0660", rcUser);

            return true;
        } else if (setting.equals("notification_wallpaper")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            File source = new File(outDir, "notification_wallpaper.jpg");
            File target = new File(rcFilesDir, "notification_wallpaper.jpg");
            Shell.SU.run("rm " + target.getAbsolutePath());
            Shell.SU.run("cp " + source.getAbsolutePath() + " "
                    + target.getAbsolutePath());
            Tools.chmodAndOwn(target, "0660", rcUser);

            return true;
        } else if (setting.equals("lockscreen_icons")) {
            // not used anymore
            if (true)
                return true;
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            for (int i = 0; i < 8; i++) {
                String settingName = "lockscreen_custom_app_icon_" + i;
                File iconToRestore = new File(outDir, settingName);

                Shell.SU.run("rm " + rcFilesDir.getAbsolutePath() + settingName + ".*");
                if (settingsFromFile.containsKey(settingName)) {
                    Shell.SU.run("cp " + iconToRestore.getAbsolutePath() + ".* "
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
                        Shell.SU.run("rm " + xml.getAbsolutePath());
                        // copy backed up file
                        Shell.SU.run("cp " + outDir + "/" + xml.getName() + " "
                                + xml.getAbsolutePath());
                        Tools.chmodAndOwn(xml, "0660", rcUser);
                    }
                }
            } else {
                Log.e(TAG, "Error getting RC user");
            }
            return true;
        } else if (setting.equals("profile_xml")) {
            String outDir = Tools.getBackupDirectory(mContext, name).getAbsolutePath();
            File source = new File(outDir, "profiles.xml");
            File target = new File(dsFileDir, "profiles.xml");
            Shell.SU.run("rm " + target.getAbsolutePath());
            Shell.SU.run("cp " + source.getAbsolutePath() + " "
                    + target.getAbsolutePath());
            Tools.chmodAndOwn(target, "0660", rcUser);

            return true;
        }


        return false;
    }
}
