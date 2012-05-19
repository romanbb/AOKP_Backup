
package com.romanbirg.aokp_backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class Tools {

    static final String TAG = "Tools";

    public static File getBackupDirectory(Context c) {
        return new File(c.getExternalFilesDir(null), "backups");
    }

    public static File getBackupDirectory(Context c, String name) {
        return new File(getBackupDirectory(c), name);
    }

    private static String[] getMounts(final String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"), 256);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.contains(path)) {
                    return line.split(" ");
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "/proc/mounts does not exist");
        } catch (IOException e) {
            Log.d(TAG, "Error reading /proc/mounts");
        }
        return null;
    }

    public static boolean mountRo() {
        final String mounts[] = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            final String device = mounts[0];
            final String path = mounts[1];
            final String point = mounts[2];
            return new ShellCommand().su.runWaitFor("mount -o ro,remount -t " + point
                    + " " + device
                    + " " + path).success();
        }
        return false;
    }

    public static boolean mountRw() {
        final String mounts[] = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            final String device = mounts[0];
            final String path = mounts[1];
            final String point = mounts[2];
            return new ShellCommand().su.runWaitFor("mount -o rw,remount -t " + point
                    + " " + device
                    + " " + path).success();
        }
        return false;
    }
}
