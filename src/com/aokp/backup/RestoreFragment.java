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

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class RestoreFragment extends Fragment {

    private static final String KEY_CATS = "categories";
    private static final String KEY_CHECK_ALL = "checkAll";
    public static final String TAG = "RestoreFragment";

    String[] cats;
    CheckBox[] checkBoxes;
    CheckBox restoreAll;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        cats = getActivity().getApplicationContext().getResources()
                .getStringArray(R.array.categories);
        checkBoxes = new CheckBox[cats.length];

    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!isDetached()) {
            outState.putBooleanArray(KEY_CATS, getCheckedBoxes());
            outState.putBoolean(KEY_CHECK_ALL, getShouldRestoreAll());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.restore, container, false);
        LinearLayout categories = (LinearLayout) v.findViewById(R.id.categories);
        for (int i = 0; i < cats.length; i++) {
            CheckBox b = new CheckBox(getActivity());
            b.setTag(cats[i]);
            categories.addView(b);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        restoreAll = (CheckBox) getView().findViewById(R.id.restore_all);
        restoreAll.setOnClickListener(mBackupAllListener);

        boolean[] checkStates = null;
        boolean allChecked;

        if (savedInstanceState != null) {
            checkStates = savedInstanceState.getBooleanArray(KEY_CATS);
            allChecked = savedInstanceState.getBoolean(KEY_CHECK_ALL);
        } else {
            allChecked = true;
        }

        // checkStates could have been not commited properly if it was detached
        if (savedInstanceState == null || checkStates == null) {
            checkStates = new boolean[cats.length];
            for (int i = 0; i < checkStates.length; i++) {
                checkStates[i] = true;
            }
        }

        for (int i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i] = (CheckBox) getView().findViewWithTag(cats[i]);
        }
        updateState(!allChecked);
        restoreAll.setChecked(allChecked);

        for (int i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i].setText(cats[i]);
            checkBoxes[i].setChecked(checkStates[i]);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.restore, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_restore:
                if (!new ShellCommand().canSU(true)) {
                    Toast.makeText(getActivity(), "Couldn't aquire root! Operation Failed",
                            Toast.LENGTH_LONG);
                    return true;
                }
                RestoreDialog restore = RestoreDialog.newInstance(getCheckedBoxes());
                restore.show(getFragmentManager(), "restore");
                break;
            case R.id.prefs:
                Intent p = new Intent(getActivity(), Preferences.class);
                getActivity().startActivity(p);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private boolean getShouldRestoreAll() {
        if (restoreAll != null)
            return restoreAll.isChecked();

        return true;
    }

    private boolean[] getCheckedBoxes() {
        boolean[] boxStates = new boolean[cats.length];
        for (int i = 0; i < cats.length; i++) {
            boxStates[i] = checkBoxes[i].isChecked();
        }
        return boxStates;
    }

    View.OnClickListener mBackupAllListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateState();
        }
    };

    private void updateState(boolean newState) {
        for (CheckBox b : checkBoxes) {
            b.setEnabled(newState);
        }
    }

    private void updateState() {
        CheckBox box = (CheckBox) getView().findViewById(R.id.restore_all);
        boolean newState = !box.isChecked();
        for (CheckBox b : checkBoxes) {
            b.setEnabled(newState);
        }
    }

    public static class RestoreDialog extends DialogFragment {
        static RestoreDialog newInstance() {
            return new RestoreDialog();
        }

        public static RestoreDialog newInstance(boolean[] checkedBoxes) {
            RestoreDialog r = new RestoreDialog();
            r.catsToBackup = checkedBoxes;
            return r;
        }

        boolean[] catsToBackup;

        String[] fileIds;
        File[] files;
        File backupDir;
        ArrayList<String> availableBackups = new ArrayList<String>();
        static int fileIndex = 0;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            backupDir = Tools.getBackupDirectory(getActivity());

            // This filter only returns directories
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            };
            files = backupDir.listFiles(fileFilter);
            if (files == null) {
                files = new File[0];
                fileIds = new String[0];
            } else {
                fileIds = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    fileIds[i] = files[i].getName();
                }
            }


            if (files.length > 0)
                return new AlertDialog.Builder(getActivity())
                        .setSingleChoiceItems(fileIds, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                fileIndex = which;

                            }
                        })
                        .setTitle("Restore")
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteBackup(fileIds[fileIndex]);
                                dialog.dismiss();
                                Toast.makeText(getActivity(),
                                        files[fileIndex].getName() + " deleted!",
                                        Toast.LENGTH_LONG).show();

                            }
                        })
                        .setPositiveButton("Restore", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (fileIndex  >= 0)
                                    restore(fileIds[fileIndex]);
                            }
                        })
                        .create();
            else
                return new AlertDialog.Builder(getActivity())
                        .setTitle("Restore")
                        .setMessage("Nothing to restore!")
                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
        }

        protected void deleteBackup(String string) {
            File deleteMe = new File(backupDir, string);
            String command = "rm -r " + deleteMe.getAbsolutePath() + "/";
            Log.w(TAG, command);
            new ShellCommand().su.runWaitFor(command);
        }

        private void restore(String name) {
            new RestoreTask(getActivity(), catsToBackup, name).execute();

        }

        public class RestoreTask extends AsyncTask<Void, Void, Integer> {

            AlertDialog d;
            Activity context;
            Restore r;
            String name = null;
            boolean[] cats = null;

            public RestoreTask(Activity context, boolean[] cats, String name) {
                this.context = context;
                this.name = name;
                this.cats = cats;
                r = new Restore(context);
            }

            @Override
            protected void onPreExecute() {
                d = new AlertDialog.Builder(context)
                        .setMessage("Restore in progress")
                        .create();
                d.show();
                Tools.mountRw();
            }

            protected Integer doInBackground(Void... v) {
                return r.restoreSettings(name, cats);
            }

            protected void onPostExecute(Integer result) {
                d.dismiss();
                Tools.mountRo();
                if (result == 0) {
                    new AlertDialog.Builder(context)
                            .setTitle("Restore successful!")
                            .setMessage("You should reboot right now!")
                            .setCancelable(false)
                            .setNeutralButton("I'll reboot later", null)
                            .setPositiveButton("Reboot", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new ShellCommand().su.run("reboot");
                                }
                            }).create().show();
                } else if (result == 1) {
                    new AlertDialog.Builder(context)
                            .setTitle("Restore failed!")
                            .setMessage("Try again or report this error if it keeps happening.")
                            .setCancelable(false)
                            .setNeutralButton("Ok", null)
                            .create().show();
                } else if (result == 2) {
                    new AlertDialog.Builder(context)
                            .setTitle("Restore failed!")
                            .setMessage("Your AOKP version is not supported yet (or is too old)!!")
                            .setCancelable(false)
                            .setNeutralButton("Ok", null)
                            .create().show();
                } else if (result == 2) {
                    new AlertDialog.Builder(context)
                            .setTitle("Restore failed!")
                            .setMessage("Are you running AOKP??")
                            .setCancelable(false)
                            .setNeutralButton("Ok", null)
                            .create().show();
                } else {
                    Toast.makeText(context.getApplicationContext(), "Restore failed!!!!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
