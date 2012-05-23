
package com.aokp.backup;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    public static final String PREF_NAME = "backup";

    public static final String KEY_DONT_SHOW_AOKP_WARNING = "skip_aokp_warning";

    public static boolean getShowNotAokpWarning(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DONT_SHOW_AOKP_WARNING, true);
    }

    public static boolean setShowNotAokpWarning(Context c, boolean use) {
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
        return prefs.edit().putBoolean(KEY_DONT_SHOW_AOKP_WARNING, use).commit();
    }
}
