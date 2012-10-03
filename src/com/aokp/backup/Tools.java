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
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;

public class Tools {

    static final String TAG = "Tools";

    private static Tools instance;

    HashMap<String, String> props;

    public static ShellCommand.SH sh = new ShellCommand().sh;
    public static ShellCommand.SH su = new ShellCommand().su;

    public Tools() {
        readBuildProp();
    }

    public static Tools getInstance() {
        if (instance == null)
            return new Tools();
        else
            return instance;
    }

    public static File getBackupDirectory(Context c) {
        if (Prefs.getBackupPermanent(c)) {
            return new File(Environment.getExternalStorageDirectory(), "AOKP_Backup");
        } else {
            return new File(c.getExternalFilesDir(null), "backups");
        }
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

    public static String getROMVersion() {
        String modVersion = Tools.getInstance().getProp("ro.modversion");
        String cmVersion = Tools.getInstance().getProp("ro.cm.version");
        String aokpVersion = Tools.getInstance().getProp("ro.aokp.version");
        if (modVersion != null)
            return modVersion;
        else if (cmVersion != null)
            return cmVersion;
        else if (aokpVersion != null)
            return aokpVersion;
        else
            return "<none>";
    }

    public static String getAOKPVersion() {
        String version = Tools.getInstance().getProp("ro.goo.version");
        return version != null ? version : "999";
    }

    public String getProp(String key) {
        if (props != null && props.containsKey(key))
            return props.get(key);

        return null;
    }

    private void readBuildProp() {
        props = new HashMap<String, String>();
        try {
            FileInputStream fstream = new FileInputStream("/system/build.prop");
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                // System.out.println(strLine);
                String[] line = strLine.split("=");
                if (line.length > 1)
                    props.put(line[0], line[1]);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            // throw new FileNotFoundException("Failed to delete file: " + f);
            Log.d("AOKP.Backup", "Failed to delete file: " + f);
    }

    public static void writeFileToSD(String fileContents, File fileToWrite) {
        if (fileContents == null)
            return;
        if (fileToWrite == null)
            return;

        if (!fileToWrite.exists()) {
            try {
                fileToWrite.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Writer outWriter;
        try {
            outWriter = new BufferedWriter(new FileWriter(fileToWrite));
            outWriter.write(fileContents);
            outWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void chmodAndOwn(File f, String chmod, String chownUser) {
        new ShellCommand().su
                .runWaitFor("chown " + chownUser + ":" + chownUser
                        + " " + f.getAbsolutePath());
        new ShellCommand().su
                .runWaitFor("chmod" + chmod + " " + f.getAbsolutePath());
    }
}
