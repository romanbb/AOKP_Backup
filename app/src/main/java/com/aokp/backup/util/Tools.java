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

package com.aokp.backup.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.aokp.backup.ui.Prefs;
import eu.chainfire.libsuperuser.Shell;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Tools {

    static final String TAG = "Tools";
    static final boolean DEBUG = true;

    private static Tools instance;
    private static String sMyPid;

    HashMap<String, String> props;

    static String mROMControlPid;

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
        File f = null;
        if (Prefs.getUseExternalStorage(c)) {
            f = Tools.getExternalBackupHome();
        } else {
            f = Tools.getInternalBackupHome(c);
        }
        f.mkdirs();
        return f;
    }

    @Deprecated
    public static File getBackupDirectory(Context c, String name) {
        File d = new File(getBackupDirectory(c), name);
        if (!d.exists()) {
            d.mkdir();
        }
        return d;
    }

    public static File getTempBackupDirectory(Context c, boolean create) {
        File d = new File(c.getCacheDir(), ".zipping");
        if (create) {
            FileUtils.deleteQuietly(d);
            d.mkdir();
        }
        return d;
    }

    public static File getTempRestoreDirectory(Context c, boolean create) {
        File d = new File(c.getCacheDir(), ".restoring");
        if (create) {
            FileUtils.deleteQuietly(d);
            d.mkdir();
        }
        return d;
    }

    public static File getInternalBackupHome(Context c) {
        return new File(c.getCacheDir(), "backups");
    }

    public static File getExternalBackupHome() {

        return new File(Environment.getExternalStorageDirectory(), "AOKP_Backup");
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

    @Deprecated
    public static boolean mountRo() {
        final String mounts[] = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            final String device = mounts[0];
            final String path = mounts[1];
            final String point = mounts[2];
            List<String> result = Shell.SU.run("mount -o ro,remount -t " + point
                    + " " + device
                    + " " + path);
            if (result != null && !result.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static boolean mountRw() {
        final String mounts[] = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            final String device = mounts[0];
            final String path = mounts[1];
            final String point = mounts[2];
            List<String> result = Shell.SU.run("mount -o rw,remount -t " + point
                    + " " + device
                    + " " + path);
            if (result != null && !result.isEmpty()) {
                return true;
            }
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

    public static Integer getAOKPGooVersion() {
        String version = Tools.getInstance().getProp("ro.goo.version");
        try {
            return version != null && !version.isEmpty() ? Integer.parseInt(version) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static void setROMControlPid(String pid) {
        mROMControlPid = pid;
    }

    public static String getRomControlPid() {
        if (mROMControlPid != null) {
            return mROMControlPid;
        }
        List<String> result = Shell.SU.run("ls -ld /data/data/com.aokp.romcontrol/ | awk '{print $3}' | less");
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }

        return null;
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

    public static String readFileToString(File f) throws IOException {
        DataInputStream in = null;
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fstream = new FileInputStream(f);
            in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                sb.append(strLine);
            }
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
            }
        }
        return sb.toString();
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

        Writer outWriter = null;
        try {
            outWriter = new BufferedWriter(new FileWriter(fileToWrite));
            outWriter.write(fileContents);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "error", e);
        } finally {
            if (outWriter != null)
                try {
                    outWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static List<String> getChmodAndOwnCommand(File f, String chmod, String chownUser) {
        List<String> commands = new ArrayList<String>();
        commands.add("chown " + chownUser + ":" + chownUser
                + " " + f.getAbsolutePath());
        commands.add("chmod " + chmod + " " + f.getAbsolutePath());
        return commands;
    }

    @Deprecated
    public static void chmodAndOwn(File f, String chmod, String chownUser) {
        Shell.SU.run(getChmodAndOwnCommand(f, chmod, chownUser));
    }


    public static void zip(File directory, File zipfile) throws IOException {
        URI base = directory.toURI();
        Deque<File> queue = new LinkedList<File>();
        queue.push(directory);
        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();
                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    } else {
                        zout.putNextEntry(new ZipEntry(name));
                        copy(kid, zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    public static String md5(File filename) {
        InputStream in;
        try {
            in = new FileInputStream(filename);

            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            in.close();

            byte[] bytes = md.digest();

            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                sb.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4));
                sb.append("0123456789ABCDEF".charAt((b & 0x0F)));
            }
            String hex = sb.toString();
            return hex.toLowerCase();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void unzip(File file, File directory) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryDestination = new File(directory, entry.getName());
            entryDestination.getParentFile().mkdirs();
            InputStream in = zipFile.getInputStream(entry);
            OutputStream out = new FileOutputStream(entryDestination);
            IOUtils.copy(in, out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }

    private static void copy(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }

    public static String getMyPid(Context c) {
        if (sMyPid != null) {
            return sMyPid;
        }
        List<String> result = Shell.SU.run("ls -ld /data/data/" + c.getPackageName() + "/ | awk '{print $3}' | less");
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }

        return null;
    }

    public static IOFileFilter getIOBackuPFileFilter() {
        return new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    if (file.getName().endsWith(".zip")) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean accept(File file, String s) {
                return accept(file);
            }
        };
    }

    public static FileFilter getBackupFileFilter(final Context c, final boolean acceptOldBackups) {
        final FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return acceptOldBackups;
                } else if (file.getName().endsWith(".zip")) {
                    return true;
                }
                return false;
            }
        };
        return fileFilter;
    }

    public static FileFilter getBackupFileFilter(final Context c) {
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else if (file.getName().endsWith(".zip")) {
                    return true;
                }
                return false;
            }
        };
        return fileFilter;
    }
}
