
package com.aokp.backup;

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import com.aokp.backup.util.Tools;

import java.io.IOException;
import java.io.InputStream;

public class AOKPBackup extends Application {

    static final String TAG = "AOKP Backup";

    public boolean isAOKPVersionSupported() {
        if (Tools.getAOKPGooVersion() > 0) {
            return true;
        }
        if (Tools.getROMVersion().contains("aokp")) {
            return true;
        } else {
            Log.e(TAG, "ROM version: " + Tools.getROMVersion());
        }


        return false;
    }

    public boolean isAndroidVersionSupported() {
        switch (Tools.getAndroidVersion()) {
            case Build.VERSION_CODES.JELLY_BEAN_MR1:
            case Build.VERSION_CODES.JELLY_BEAN:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                return true;
        }

        return false;
    }

}
