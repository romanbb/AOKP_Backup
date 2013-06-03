
package com.aokp.backup.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import com.aokp.backup.R;
import com.aokp.backup.util.SVal;
import com.aokp.backup.util.Tools;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.io.IOException;

public class ICSBackup extends Backup {

    public static final int CAT_GENERAL_UI = 0;
    public static final int CAT_NAVIGATION_BAR = 1;
    public static final int CAT_LOCKSCREEN_OPTS = 2;
    public static final int CAT_POWER_MENU_OPTS = 3;
    public static final int CAT_WEATHER = 4;
    public static final int CAT_POWER_SAVER = 5;
    public static final int CAT_LED = 6;
    public static final int CAT_SB_GENERAL = 7;
    public static final int CAT_SB_TOGGLES = 8;
    public static final int CAT_SB_CLOCK = 9;
    public static final int CAT_SB_BATTERY = 10;
    public static final int CAT_SB_SIGNAL = 11;
    public static final int CAT_LIGHT_LEVELS = 12;

    public static final int NUM_CATS = 13;

    public ICSBackup(Context c, File zip) throws IOException {
        super(c, zip);
    }

    public ICSBackup(Context c, String name) {
        super(c, name);
    }


    @Override
    public int getNumCats() {
        return NUM_CATS;
    }

    @Override
    protected String[] getSettingsCategory(int categoryIndex) {
        Resources res = mContext.getResources();
        switch (categoryIndex) {
            case CAT_GENERAL_UI:
                return res.getStringArray(R.array.cat_general_ui);
            case CAT_NAVIGATION_BAR:
                return res.getStringArray(R.array.cat_navigation_bar);
            case CAT_LED:
                return res.getStringArray(R.array.cat_led);
            case CAT_LOCKSCREEN_OPTS:
                return res.getStringArray(R.array.cat_lockscreen);
            case CAT_POWER_MENU_OPTS:
                return res.getStringArray(R.array.cat_powermenu);
            case CAT_POWER_SAVER:
                return res.getStringArray(R.array.cat_powersaver);
            case CAT_SB_BATTERY:
                return res.getStringArray(R.array.cat_statusbar_battery);
            case CAT_SB_CLOCK:
                return res.getStringArray(R.array.cat_statusbar_clock);
            case CAT_SB_GENERAL:
                return res.getStringArray(R.array.cat_statusbar_general);
            case CAT_SB_SIGNAL:
                return res.getStringArray(R.array.cat_statusbar_signal);
            case CAT_SB_TOGGLES:
                return res.getStringArray(R.array.cat_statusbar_toggles);
            case CAT_WEATHER:
                return res.getStringArray(R.array.cat_weather);
            case CAT_LIGHT_LEVELS:
                return res.getStringArray(R.array.cat_custom_backlight);
            default:
                return null;
        }
    }

    @Override
    public boolean handleBackupSpecialCase(String setting) {
        if (setting.equals("disable_boot_animation")) {
            if (!new File("/system/media/bootanimation.zip").exists()) {
                mSpecialCaseKeys.add(new SVal(setting, "1"));
            }

            return true;
        } else if (setting.equals("disable_boot_audio")) {
            if (!new File("/system/media/boot_audio.mp3").exists()) {
                mSpecialCaseKeys.add(new SVal(setting, "1"));
            }

            return true;
        } else if (setting.equals("disable_bug_mailer")) {
            if (!new File("/system/bin/bugmailer.sh").exists()) {
                mSpecialCaseKeys.add(new SVal(setting, "1"));
            }

            return true;
        } else if (setting.equals("navigation_bar_icons")) {
            String outDir = Tools.getTempBackupDirectory(mContext, false).getAbsolutePath();
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 5; i++) {
                // String iconIntent = "navigation_custom_app_intent_" + i;
                // String iconIntentValue = Settings.System.getString(resolver,
                // iconIntent);
                // Log.i(TAG, "intent value: " + iconIntentValue);
                // if (iconIntentValue != null)
                // mSpecialCaseKeys.add(new SVal(iconIntent, iconIntentValue));
                //
                // String iconLongPressIntent =
                // "navigation_longpress_app_intent_" + i;
                // String iconLongPressIntentValue =
                // Settings.System.getString(resolver,
                // iconLongPressIntent);
                // if (iconLongPressIntentValue != null)
                // mSpecialCaseKeys.add(new SVal(iconLongPressIntent,
                // iconLongPressIntentValue));

                String iconSetting = "navigation_custom_app_icon_" + i;
                String iconValue = Settings.System.getString(resolver, iconSetting);
                if (iconValue != null) {
                    if (iconValue.length() > 0) {
                        mBackupValues.add(new SVal(iconSetting, iconValue));
                        String cmd = "cp /data/data/com.aokp.romcontrol/files/navbar_icon_" + i
                                + ".png " + outDir + "/";
                        Shell.SU.run(cmd);
                    }
                }
            }

            return true;
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getTempBackupDirectory(mContext, false).getAbsolutePath();


            Shell.SU.run("cp /data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg "
                    + outDir);

            return true;
        } else if (setting.equals("lockscreen_icons")) {
            String outDir = Tools.getTempBackupDirectory(mContext, false).getAbsolutePath();

            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 8; i++) {
                // String intentSetting = "lockscreen_custom_app_intent_" + i;
                // String intentValue = Settings.System.getString(resolver,
                // intentSetting);
                // if (intentValue != null)
                // mSpecialCaseKeys.add(new SVal(intentSetting, intentValue));

                String set = "lockscreen_custom_app_icon_" + i;
                String val = Settings.System.getString(resolver, set);
                if (val != null) {
                    mSpecialCaseKeys.add(new SVal(set, val));
                    File f = new File(Uri.parse(val).getPath());
                    if (f.exists()) {
                        Shell.SU.run("cp /data/data/com.aokp.romcontrol/files/lockscreen_icon_" + i
                                + ".png " + outDir);
                    }
                }
            }


            mSpecialCaseKeys.add(new SVal(setting, "1"));
            return true;
        } else if (setting.equals("private_weather_prefs")) {

            return true;
        }

        return false;
    }

    public boolean okayToRestore() {
        int minimumGooVersion = 0;
//        if (minimumGooVersion == -1) {
//            return false;
//        }
        final int maximumGooVersion = 19;

        try {
            int currentVersion = Tools.getAOKPGooVersion();

            if (currentVersion <= maximumGooVersion && currentVersion >= minimumGooVersion)
                return true;
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public boolean handleRestoreSpecialCase(SVal sval) {
        String setting = sval.getKey();
        String value = sval.getValue();

        if (setting.equals("disable_boot_animation") && value.equals("1")) {
            if (new File("/system/media/bootanimation.zip").exists()) {
                Shell.SU.run("mv /system/media/bootanimation.zip /system/media/bootanimation.unicorn");
            }
        } else if (setting.equals("disable_boot_audio") && value.equals("1")) {
            if (new File("/system/media/boot_audio.mp3").exists()) {
                Shell.SU.run("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
            }
        } else if (setting.equals("disable_bug_mailer") && value.equals("1")) {
            if (new File("/system/bin/bugmailer.sh").exists()) {
                Shell.SU.run("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.unicorn");
            }
        } else if (setting.equals("navigation_bar_icons")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);
            for (int i = 0; i < 7; i++) {
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
//                if (settingsFromFile.containsKey(settingName)) {
                String cmd = "cp " + outDir.getAbsolutePath() + "/" + iconName
                        + " /data/data/com.aokp.romcontrol/files/";
                Shell.SU.run(cmd);
//                    restoreSetting(settingsFromFile.get(settingName));
//                } else {
//                    restoreSetting(settingName, "", false);
            }
        } else if (setting.equals("lockscreen_wallpaper")) {
            String outDir = Tools.getTempRestoreDirectory(mContext, false).getAbsolutePath();

            Shell.SU.run("rm -f /data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg");
            Shell.SU.run("cp " + outDir + "/lockscreen_wallpaper.jpg"
                    + " /data/data/com.aokp.romcontrol/files/");

        } else if (setting.equals("lockscreen_icons")) {
            String outDir = Tools.getTempRestoreDirectory(mContext, false).getAbsolutePath();
            for (int i = 0; i < 8; i++) {
                // String iconIntent = "lockscreen_custom_app_intent_" + i;
                // if (settingsFromFile.containsKey(iconIntent))
                // restoreSetting(settingsFromFile.get(iconIntent));

                String settingName = "lockscreen_custom_app_icon_" + i;
                File iconToRestore = new File(outDir, settingName);

                Shell.SU.run("rm -f /data/data/com.aokp.romcontrol/files/" + settingName + ".*");
//                if (settingsFromFile.containsKey(settingName)) {
                    Shell.SU.run("cp " + iconToRestore.getAbsolutePath() + ".* "
                            + "/data/data/com.aokp.romcontrol/files/");
//                    restoreSetting(settingsFromFile.get(settingName));
//                } else {
//                    restoreSetting(settingName, "", false);
//                }

            }
            return true;
        } else if (setting.equals("private_weather_prefs")) {

            return true;
        }

        return false;
    }

    protected boolean restoreSetting(String setting, String value, boolean secure) {
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
        return restoreSetting(setting.getRealSettingString(), setting.getValue(), setting.isSecure());
    }
}
