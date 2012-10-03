package com.aokp.backup;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.backup.BackupManager;
import android.content.Context;
import android.util.Log;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class ParseHelpers {

	static final String TAG = "ParseHelpers";
	static final String REMOTE_BACKUPS = "remote_backups";

	static ParseHelpers sIntance;
	public static final Object sLock = new Object();

	Context mContext;
	BackupManager mBackupManager;

	private ParseHelpers(Context c) {
		mContext = c;
		mBackupManager = new BackupManager(mContext);
	}

	public static ParseHelpers getInstance(Context c) {
		if (sIntance == null)
			sIntance = new ParseHelpers(c);

		return sIntance;
	}

	public void addId(String id) throws IOException {
		if (!AOKPBackup.isParseEnabled())
			return;

		Log.d(TAG, "adding id: " + id);
		ArrayList<String> ids = readIds();
		ids.add(id);
		writeIds(ids);
		mBackupManager.dataChanged();
	}

	public void removeId(final String id) throws IOException {
		if (!AOKPBackup.isParseEnabled())
			return;

		Log.d(TAG, "removing id: " + id);
		ArrayList<String> ids = readIds();
		ids.remove(id);
		writeIds(ids);
		mBackupManager.dataChanged();
		ParseQuery query = new ParseQuery("Backup");
		query.getInBackground(id, new GetCallback() {
			public void done(ParseObject object, ParseException e) {
				if (e == null) {
					// successfully retreived object
					object.deleteEventually();
				} else {
					// something went wrong
					Log.e(TAG, "could not remove Parse object id: " + id);
				}
			}
		});
	}

	public ArrayList<String> readIds() throws FileNotFoundException,
			IOException {
		if (!AOKPBackup.isParseEnabled())
			return new ArrayList<String>();
		;
		try {
			synchronized (sLock) {
				BufferedReader r = new BufferedReader(new InputStreamReader(
						mContext.openFileInput(REMOTE_BACKUPS)));
				StringBuilder total = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					total.append(line);
				}

				ArrayList<String> list = new ArrayList<String>(
						Arrays.asList(total.toString().split("\\n")));
				Log.e(TAG, "readIds count: " + list.size());
				return list;
			}
		} catch (IOException e) {
			// none!
			return new ArrayList<String>();
		}
	}

	private void writeIds(ArrayList<String> ids) throws IOException {
		if (!AOKPBackup.isParseEnabled())
			return;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			final String id = ids.get(i);
			if (id != null && !id.isEmpty()) {
				sb.append(id);
				if (i != (ids.size() - 1)) {
					sb.append("\n");
				}
			}
		}

		synchronized (sLock) {
			FileOutputStream fOut = mContext.openFileOutput(REMOTE_BACKUPS,
					Context.MODE_PRIVATE);
			OutputStreamWriter out = new OutputStreamWriter(fOut);
			out.write(sb.toString());
			out.close();
			// IOUtils.writeLines(ids, "\n",
			// mContext.openFileOutput(REMOTE_BACKUPS, Context.MODE_PRIVATE));
		}
	}

}
