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
