package com.aokp.backup;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.parse.Parse;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class AOKPBackup extends Application {

	static final String TAG = "AOKP Backup";

	private static boolean mParseFeaturesEnabled = false;

	public void initParse() {
		if(!mParseFeaturesEnabled)
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

}
