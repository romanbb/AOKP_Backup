
package com.aokp.backup;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class Home extends Activity {

    TextView mErrorMessage = null;
    boolean skipWarningDialog = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addTabs();

        if (savedInstanceState == null) {
            check();
        }
    }

    public void addTabs() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setTitle(R.string.actionbar_title);

        actionBar.addTab(actionBar.newTab()
                .setText("Backup")
                .setTabListener(new TabListener<BackupFragment>(
                        this, "backup", BackupFragment.class)));
        actionBar.addTab(actionBar.newTab()
                .setText("Restore")
                .setTabListener(new TabListener<RestoreFragment>(
                        this, "restore", RestoreFragment.class)));
    }

    public void check() {
        new CheckTask(this).execute();
    }

    public void noRoot() {
        getActionBar().removeAllTabs();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.error);
        mErrorMessage = (TextView) findViewById(R.id.error);
        mErrorMessage.setText(R.string.no_root);
    }

    public void notAOKP() {
        WarningDialog warning = new WarningDialog();
        warning.show(getFragmentManager(), "warning");
    }

    public void addNotAokpMessage() {
        getActionBar().removeAllTabs();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.error);
        mErrorMessage = (TextView) findViewById(R.id.error);
        mErrorMessage.setText(R.string.not_aokp);
    }

    public static class WarningDialog extends DialogFragment {

        Home mActivity;
        CheckBox dontShowAgain;
        CheckBox textRead;
        Button set;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mActivity = (Home) getActivity();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            setCancelable(false);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            getDialog().setTitle("Caution!"); // TODO make it a string
            getDialog().setCanceledOnTouchOutside(false);

            View v = inflater.inflate(R.layout.dialog_not_aokp_warning, container, false);
            Button cancel = (Button) v.findViewById(R.id.cancel);
            set = (Button) v.findViewById(R.id.okay);
            TextView text = (TextView) v.findViewById(R.id.content);
            dontShowAgain = (CheckBox) v.findViewById(R.id.dont_show);
            textRead = (CheckBox) v.findViewById(R.id.read_warning);

            text.setText(R.string.not_aokp_warning);

            textRead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox b = (CheckBox) v;
                    set.setEnabled(b.isChecked());
                }
            });

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.addNotAokpMessage();
                    getDialog().dismiss();
                }
            });

            set.setEnabled(false);
            set.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (textRead.isChecked()) {
                        if (dontShowAgain.isChecked())
                            Prefs.setShowNotAokpWarning(mActivity, false);

                        getDialog().dismiss();
                    }
                }
            });
            return v;
        }
    }

    class CheckTask extends AsyncTask<Void, Void, Integer> {

        Home activity;

        final static int RESULT_NO_ROOT = 0;
        final static int RESULT_NOT_AOKP = 1;
        final static int RESULT_OK = 2;

        public CheckTask(Activity a) {
            activity = (Home) a;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (!new ShellCommand().canSU()) {
                return RESULT_NO_ROOT;
            }

            if (Prefs.getShowNotAokpWarning(activity)) {
                if (!Tools.getROMVersion().startsWith("aokp")) {
                    return RESULT_NOT_AOKP;
                }
            }

            return RESULT_OK;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch (result) {
                case RESULT_NO_ROOT:
                    activity.noRoot();
                    break;
                case RESULT_NOT_AOKP:
                    activity.notAOKP();
                    break;
                case RESULT_OK:
                    // tabs added by defualt
                    break;
            }

        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private Fragment mFragment;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            this(activity, tag, clz, null);
        }

        public TabListener(Activity activity, String tag, Class<T> clz, Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state. If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }
}
