
package com.romanbirg.aokp_backup;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

public class Backup {

    // categories
    public static final int CAT_GENERAL_UI = 0;
    public static final int CAT_NAVIGATION_BAR = 1;
    public static final int CAT_LOCKSCREEN_OPTS = 2;
    public static final int CAT_POWER_MENU_OPTS = 3;
    public static final int CAT_WEATHER = 4;

    public static ArrayList<SVal> backupSettings(Context c, int category) {
        Resources res = c.getResources();
        ContentResolver resolver = c.getContentResolver();
        ArrayList<SVal> svals = new ArrayList<Backup.SVal>();
        switch (category) {
            case CAT_GENERAL_UI:
                String[] settings = res.getStringArray(R.array.cat_general_ui);
                for (String setting : settings) {
                    String val = Settings.System.getString(resolver, setting);
                    if (val != null)
                        svals.add(new SVal(setting, val));
                }
        }
        return svals;
    }

    public static class SVal {
        String setting;
        String val;

        public SVal(String setting, String val) {
            this.setting = setting;
            this.val = val;
        }

        public String toString() {
            return setting + "=" + val;
        }
    }

}
