
package com.aokp.backup;

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import com.aokp.backup.util.Tools;
import com.parse.Parse;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class AOKPBackup extends Application {

    static final String TAG = "AOKP Backup";

    private static boolean mParseFeaturesEnabled = false;

    public void initParse() {
        if (!mParseFeaturesEnabled)
            return;
        AssetManager assets = getApplicationContext().getResources()
                .getAssets();
        try {
            InputStream parseApplicationIdFile = assets
                    .open("parse.application.id");
            String parseApplicationId = IOUtils
                    .toString(parseApplicationIdFile);

            InputStream parseClientIdFile = assets.open("parse.client.id");
            String parseClientId = IOUtils.toString(parseClientIdFile);

            Parse.initialize(this, parseApplicationId, parseClientId);
            mParseFeaturesEnabled = true;

            Log.i(TAG, "client key: " + parseClientId);
            Log.i(TAG, "app key: " + parseApplicationId);
        } catch (IOException e) {
            Log.i(TAG, "disabling Parse features");
            mParseFeaturesEnabled = false;
        }
    }

    public static boolean isParseEnabled() {
        return false;
    }

    public boolean isAOKPVersionSupported() {
        if (Tools.getOfficialAOKPVersion() > 0) {
            return true;
        }
        if (Tools.getROMVersion().startsWith("aokp_")) {
            return true;
        }


        return false;
    }

    public boolean isAndroidVersionSupported() {
        switch (Tools.getAndroidVersion()) {
//            case Build.VERSION_CODES.JELLY_BEAN_MR1:
            case Build.VERSION_CODES.JELLY_BEAN:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                return true;
        }

        return false;
    }

}
