
package com.aokp.backup.categories;

import android.content.Context;
import android.content.res.Resources;
import com.aokp.backup.R;

public class JBMR1Categories extends Categories {
    public static final int CAT_GENERAL_UI = 0;
    public static final int CAT_NAVIGATION_BAR = 1;
    public static final int CAT_LOCKSCREEN_OPTS = 2;
    public static final int CAT_POWERMENU = 3;
    public static final int CAT_LED_OPTIONS = 4;
    public static final int CAT_SOUND = 5;
    public static final int CAT_SB_TOGGLES = 6;
    public static final int CAT_SB_CLOCK = 7;
    public static final int CAT_SB_BATTERY = 8;
    public static final int CAT_SB_SIGNAL = 9;
    public static final int CAT_RIBBONS = 10;
    public static final int CAT_QUIET_HOURS = 11;
    public static final int CAT_PROFILE = 12;
    public static final int NUM_CATS = 13;

    @Override
    public String[] getSettingsCategory(Context c, int cat) {
        Resources res = c.getResources();
        switch (cat) {
            case CAT_GENERAL_UI:
                return res.getStringArray(R.array.jbmr1_cat_general_ui);
            case CAT_NAVIGATION_BAR:
                return res.getStringArray(R.array.jbmr1_cat_navigation_bar);
            case CAT_LED_OPTIONS:
                return res.getStringArray(R.array.jbmr1_cat_led);
            case CAT_LOCKSCREEN_OPTS:
                return res.getStringArray(R.array.jbmr1_cat_lockscreen);
            case CAT_SB_BATTERY:
                return res.getStringArray(R.array.jbmr1_cat_statusbar_battery);
            case CAT_SB_CLOCK:
                return res.getStringArray(R.array.jbmr1_cat_statusbar_clock);
            case CAT_SB_TOGGLES:
                return res.getStringArray(R.array.jbmr1_cat_statusbar_toggles);
            case CAT_POWERMENU:
                return res.getStringArray(R.array.jbmr1_cat_powermenu);
            case CAT_SOUND:
                return res.getStringArray(R.array.jbmr1_cat_sound);
            case CAT_SB_SIGNAL:
                return res.getStringArray(R.array.jbmr1_cat_statusbar_signal);
            case CAT_RIBBONS:
                return res.getStringArray(R.array.jbmr1_cat_ribbons);
            case CAT_QUIET_HOURS:
                return res.getStringArray(R.array.jbmr1_cat_quiet_hours);
            case CAT_PROFILE:
                return res.getStringArray(R.array.jbmr1_cat_profiles);
            default:
                return null;
        }
    }

}
