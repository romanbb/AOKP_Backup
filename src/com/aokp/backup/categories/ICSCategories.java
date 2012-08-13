
package com.aokp.backup.categories;

import android.content.Context;
import android.content.res.Resources;

import com.aokp.backup.R;

public class ICSCategories extends Categories {
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

    @Override
    public String[] getSettingsCategory(Context c, int categoryIndex) {
        Resources res = c.getResources();
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

}
