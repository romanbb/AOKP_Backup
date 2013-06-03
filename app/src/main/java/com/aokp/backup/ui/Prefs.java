/*
 * Copyright (C) 2012 Roman Birg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aokp.backup.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

public class Prefs {
    static final String PREFS_BACKUP = "backup";
    static final String PREFS_APP = "prefs";

    public static final String KEY_DONT_SHOW_AOKP_WARNING = "skip_aokp_warning";

    public static final String KEY_PERM_STORAGE = "perm_storage";

    public static final String KEY_REMOTE_BACKUP_IDS = "remote_backups";

    public static boolean getShowNotAokpWarning(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DONT_SHOW_AOKP_WARNING, true);
    }

    public static boolean setShowNotAokpWarning(Context c, boolean use) {
        SharedPreferences prefs = c
                .getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE);
        return prefs.edit().putBoolean(KEY_DONT_SHOW_AOKP_WARNING, use).commit();
    }

    public static boolean getBackupPermanent(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getBoolean(KEY_PERM_STORAGE, true);
    }

    public static boolean setBackupLocation(Context c, String loc) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE);
        return prefs.edit().putString(KEY_PERM_STORAGE, loc).commit();
    }
}
