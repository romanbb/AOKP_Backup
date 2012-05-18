
package com.romanbirg.aokp_backup;

import java.util.Date;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class Home extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);

        Tab tab = actionBar.newTab()
                .setText(R.string.backup)
                .setTabListener(new TabListener<BackupFragment>(
                        this, "backup", BackupFragment.class));
        actionBar.addTab(tab);

        tab = actionBar.newTab()
                .setText(R.string.restore)
                .setTabListener(new TabListener<RestoreFragment>(
                        this, "restore", RestoreFragment.class));
        actionBar.addTab(tab);
    }

    public static class BackupFragment extends Fragment {

        private static final String KEY_CATS = "categoreis";
        private static final String KEY_CHECK_ALL = "checkAll";

        String[] cats;
        CheckBox[] checkBoxes;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            cats = getActivity().getApplicationContext().getResources()
                    .getStringArray(R.array.categories);
            checkBoxes = new CheckBox[cats.length];

        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putBooleanArray(KEY_CATS, getCheckedBoxes());
            outState.putBoolean(KEY_CHECK_ALL, getShouldBackupAll());
            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            View v = inflater.inflate(R.layout.main, null);
            CheckBox backupAll = (CheckBox) v.findViewById(R.id.backup_all);
            backupAll.setOnClickListener(mBackupAllListener);

            LinearLayout categories = (LinearLayout) v.findViewById(R.id.categories);
            for (int i = 0; i < cats.length; i++) {
                CheckBox b = new CheckBox(getActivity());
                b.setTag(cats[i]);
                categories.addView(b);
            }

            return v;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            boolean[] checkStates;
            boolean allChecked;

            if (savedInstanceState != null) {
                checkStates = savedInstanceState.getBooleanArray(KEY_CATS);
                allChecked = savedInstanceState.getBoolean(KEY_CHECK_ALL);
            } else {
                allChecked = true;
                checkStates = new boolean[cats.length];
                for (int i = 0; i < checkStates.length; i++) {
                    checkStates[i] = true;
                }
            }

            for (int i = 0; i < checkBoxes.length; i++) {
                checkBoxes[i] = (CheckBox) getView().findViewWithTag(cats[i]);
                checkBoxes[i].setText(cats[i]);
                checkBoxes[i].setChecked(checkStates[i]);
                checkBoxes[i].setEnabled(!allChecked);
            }

            super.onViewCreated(view, savedInstanceState);
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
                    BackupDialog backup = BackupDialog.newInstance(getCheckedBoxes());
                    backup.show(getFragmentManager(), "backup");
                    break;

            }
            return super.onOptionsItemSelected(item);
        }

        private boolean getShouldBackupAll() {
            CheckBox backupAll = (CheckBox) getView().findViewById(R.id.backup_all);
            if (backupAll != null)
                return backupAll.isChecked();

            return false;
        }

        private boolean[] getCheckedBoxes() {
            boolean[] boxStates = new boolean[cats.length];
            for (int i = 0; i < cats.length; i++) {
                CheckBox check = (CheckBox) getView().findViewWithTag(cats[i]);
                boxStates[i] = check.isChecked();
            }
            return boxStates;
        }

        View.OnClickListener mBackupAllListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateState();
            }
        };

        private void updateState() {
            CheckBox box = (CheckBox) getView().findViewById(R.id.backup_all);
            boolean newState = !box.isChecked();
            for (CheckBox b : checkBoxes) {
                b.setEnabled(newState);
            }
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

    public static class RestoreFragment extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }

    public static class ProgressDialog extends DialogFragment {

    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /**
         * Constructor used each time a new tab is created.
         * 
         * @param activity The host Activity, used to instantiate the fragment
         * @param tag The identifier tag for the fragment
         * @param clz The fragment's Class, used to instantiate the fragment
         */
        public TabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        /* The following are each of the ActionBar.TabListener callbacks */

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            // Check if the fragment is already initialized
            if (mFragment == null) {
                // If not, instantiate and add it to the activity
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Usually do nothing.
        }
    }
}
