
package com.aokp.backup;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class BackupFragment extends SherlockFragment {

    public static String TAG = "BackupFragment";

    private static final String KEY_CATS = "categories";
    private static final String KEY_CHECK_ALL = "checkAll";

    String[] cats;
    CheckBox[] checkBoxes;
    CheckBox backupAll;

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
        outState.putBooleanArray(KEY_CATS, getCheckedBoxes());
        outState.putBoolean(KEY_CHECK_ALL, getShouldBackupAll());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        backupAll = (CheckBox) getView().findViewById(R.id.backup_all);
        backupAll.setOnClickListener(mBackupAllListener);

        boolean[] checkStates = new boolean[cats.length];
        boolean allChecked = true;
        boolean resetStates = true;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_CATS)) {
                checkStates = savedInstanceState.getBooleanArray(KEY_CATS);
                resetStates = false;
            }

            if (savedInstanceState.containsKey(KEY_CHECK_ALL))
                allChecked = savedInstanceState.getBoolean(KEY_CHECK_ALL);
        }

        if (resetStates) {
            checkStates = new boolean[cats.length];
            for (int i = 0; i < checkStates.length; i++) {
                checkStates[i] = true;
            }
        }

        for (int i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i] = (CheckBox) getView().findViewWithTag(cats[i]);
        }
        updateState(!allChecked);
        backupAll.setChecked(allChecked);

        for (int i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i].setText(cats[i]);
            checkBoxes[i].setChecked(checkStates[i]);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.backup, container, false);
        LinearLayout categories = (LinearLayout) v.findViewById(R.id.categories);
        for (int i = 0; i < cats.length; i++) {
            CheckBox b = new CheckBox(getActivity());
            b.setTag(cats[i]);
            categories.addView(b);
        }

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.backup, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_backup:
                if (!new ShellCommand().canSU(true)) {
                    Toast.makeText(getActivity(), "Couldn't aquire root! Operation Failed",
                            Toast.LENGTH_LONG);
                    return true;
                }
                BackupDialog backup = BackupDialog.newInstance(getCheckedBoxes());
                backup.show(getFragmentManager(), "backup");
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private boolean getShouldBackupAll() {
        if (backupAll != null)
            return backupAll.isChecked();

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
        CheckBox box = (CheckBox) getView().findViewById(R.id.backup_all);
        boolean newState = !box.isChecked();
        for (CheckBox b : checkBoxes) {
            b.setEnabled(newState);
        }
    }

    public static class BackupDialog extends DialogFragment {

        boolean[] cats;

        static BackupDialog newInstance(boolean[] checkedCats) {
            BackupDialog d = new BackupDialog();
            d.cats = checkedCats;
            return d;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            getDialog().setTitle("Name your backup"); // TODO make it a string
            getDialog().setCanceledOnTouchOutside(false);

            View v = inflater.inflate(R.layout.backup_dialog, container, false);
            EditText name = (EditText) v.findViewById(R.id.save_name);
            Button cancel = (Button) v.findViewById(R.id.cancel);
            Button set = (Button) v.findViewById(R.id.save);

            Date date = new Date();
            java.text.DateFormat dateFormat = android.text.format.DateFormat
                    .getDateFormat(getActivity());
            String currentDate = dateFormat.format(date);
            currentDate = currentDate.replace("/", "-");
            name.setText(currentDate);

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().dismiss();
                }
            });

            set.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newName = ((EditText) getDialog().findViewById(R.id.save_name))
                            .getText()
                            .toString();
                    backup(newName);
                    getDialog().dismiss();
                }
            });
            return v;
        }

        public void backup(String name) {
            new WorkTask(getActivity(), cats, name).execute();
        }

        public class WorkTask extends AsyncTask<Void, Boolean, Boolean> {

            AlertDialog d;
            Activity context;
            Backup b;
            String name;

            public WorkTask(Activity context, boolean[] cats, String name) {
                this.context = context;
                this.name = name;
                b = new Backup(context, cats);

            }

            @Override
            protected void onPreExecute() {
                d = new AlertDialog.Builder(context)
                        .setMessage("Backup in progress")
                        .create();
                d.show();
            }

            protected Boolean doInBackground(Void... files) {
                return b.backupSettings(name);
            }

            protected void onPostExecute(Boolean result) {
                if (d != null) {
                    Toast.makeText(context.getApplicationContext(), result ? "Backup successful!"
                            : "Backup failed!!!!", Toast.LENGTH_SHORT).show();
                    d.dismiss();
                }
            }
        }

    }
}
