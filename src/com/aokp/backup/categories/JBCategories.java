
package com.aokp.backup.categories;

import android.content.Context;
import android.content.res.Resources;

import com.aokp.backup.R;

public class JBCategories extends Categories {
    public static final int CAT_GENERAL_UI = 0;
    public static final int CAT_NAVIGATION_BAR = 1;
    public static final int CAT_LOCKSCREEN_OPTS = 2;
    public static final int CAT_WEATHER = 4;
    public static final int CAT_LED_OPTIONS = 6;
    public static final int CAT_SOUND = 7;
    public static final int CAT_SB_TOGGLES = 8;
    public static final int CAT_SB_CLOCK = 9;
    public static final int CAT_SB_BATTERY = 10;
    // public static final int CAT_POWER_MENU_OPTS = 3;
    // public static final int CAT_POWER_SAVER = 5;

    public static final int NUM_CATS = 9;

    @Override
    public String[] getSettingsCategory(Context c, int cat) {
        Resources res = c.getResources();
        switch (cat) {
            case CAT_GENERAL_UI:
                return res.getStringArray(R.array.jbcat_general_ui);
            case CAT_NAVIGATION_BAR:
                return res.getStringArray(R.array.jbcat_navigation_bar);
            case CAT_LED_OPTIONS:
                return res.getStringArray(R.array.jbcat_led);
            case CAT_LOCKSCREEN_OPTS:
                return res.getStringArray(R.array.jbcat_lockscreen);
            case CAT_SB_BATTERY:
                return res.getStringArray(R.array.jbcat_statusbar_battery);
            case CAT_SB_CLOCK:
                return res.getStringArray(R.array.jbcat_statusbar_clock);
            case CAT_SB_TOGGLES:
                return res.getStringArray(R.array.jbcat_statusbar_toggles);
            case CAT_WEATHER:
                return res.getStringArray(R.array.jbcat_weather);
            case CAT_SOUND:
                return res.getStringArray(R.array.jbcat_sound);
            default:
                return null;
        }
    }

}
