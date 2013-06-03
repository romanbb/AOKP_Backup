
package com.aokp.backup.ui;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import com.aokp.backup.R;
import eu.chainfire.libsuperuser.Shell;

public class Preferences extends PreferenceActivity {

    CheckBoxPreference mPermanentStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_main);
        mPermanentStorage = (CheckBoxPreference) findPreference("perm_storage");
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mPermanentStorage) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();

            if (checked) {
                // move from /sdcard/Android
                Shell.SU.run("mv /sdcard/Android/data/com.aokp.backup/files/backups /sdcard/AOKP_Backup/");
            } else {
                // move to /sdcard/Data
                Shell.SU.run("mv /sdcard/AOKP_Backup/ /sdcard/Android/data/com.aokp.backup/files/backups");

            }

            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
