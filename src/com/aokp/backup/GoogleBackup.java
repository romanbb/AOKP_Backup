package com.aokp.backup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.aokp.backup.backup.Backup;
import com.aokp.backup.util.Tools;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class GoogleBackup extends BackupAgentHelper {

	static final String TAG = "BackupAgentHelper";

	@Override
	public void onCreate() {
		if (AOKPBackup.isParseEnabled()) {
			FileBackupHelper helper = new FileBackupHelper(
					getApplicationContext(), ParseHelpers.REMOTE_BACKUPS);
			addHelper("files", helper);
		}
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		if (AOKPBackup.isParseEnabled()) {
			synchronized (ParseHelpers.sLock) {
				super.onBackup(oldState, data, newState);
			}
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {

		if (AOKPBackup.isParseEnabled()) {
			synchronized (ParseHelpers.sLock) {
				super.onRestore(data, appVersionCode, newState);
			}

			// download the backups!
			for (String s : ParseHelpers.getInstance(getApplicationContext())
					.readIds()) {
				Log.d(TAG, "restoring backup with id: " + s);
				ParseQuery query = new ParseQuery("Backup");
				query.getInBackground(s, new GetCallback() {
					@Override
					public void done(ParseObject object, ParseException e) {
						final ParseFile backupZip = (ParseFile) object
								.get("zippedBackup");
						backupZip.getDataInBackground(new GetDataCallback() {
							@Override
							public void done(byte[] data, ParseException e) {
								final String name = backupZip.getName();
								final File zipFile = Tools.getBackupDirectory(
										getApplicationContext(), name);
								Log.e(TAG, "finished downloading " + name);
								try {
									IOUtils.write(data, new FileWriter(zipFile));
									Backup.restoreBackupFromZip(zipFile, Tools
											.getBackupDirectory(
													getApplicationContext(),
													name));
								} catch (IOException e1) {
									Log.e(TAG, "failed restoring " + name);
									e1.printStackTrace();
								}
							}
						});
					}
				});
			}
		}
	}
}
