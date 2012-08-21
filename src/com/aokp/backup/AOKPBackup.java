
package com.aokp.backup;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

import com.parse.Parse;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class AOKPBackup extends Application {

    Context mContext;
    boolean mParseFeaturesEnabled;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();

        AssetManager assets = mContext.getResources().getAssets();
        try {
            InputStream parseApplicationIdFile = assets.open("parse.application.id");
            String parseApplicationId = IOUtils.toString(parseApplicationIdFile);

            InputStream parseClientIdFile = assets.open("parse.application.id");
            String parseClientId = IOUtils.toString(parseClientIdFile);

            Parse.initialize(this, parseApplicationId, parseClientId);
            mParseFeaturesEnabled = true;
        } catch (IOException e) {
            e.printStackTrace();
            mParseFeaturesEnabled = false;
        }
    }

    public boolean isParseEnabled() {
        return mParseFeaturesEnabled;
    }

}
