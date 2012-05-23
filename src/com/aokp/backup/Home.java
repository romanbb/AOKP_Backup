
package com.aokp.backup;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class Home extends SherlockFragmentActivity {

    TextView mErrorMessage = null;
    boolean skipWarningDialog = false;

    TabHost mTabHost;
    TabManager mTabManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_tabs);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mTabManager = new TabManager(this, mTabHost, R.id.realtabcontent);

        mTabManager.addTab(mTabHost.newTabSpec("backup").setIndicator("Backup"),
                BackupFragment.class, savedInstanceState);
        mTabManager.addTab(mTabHost.newTabSpec("restore").setIndicator("Restore"),
                RestoreFragment.class, savedInstanceState);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }

        // getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //
        // ActionBar.Tab backup = getSupportActionBar().newTab();
        // backup.setText("Backup");
        // backup.setTabListener(new MyTabListener(this, "backup",
        // BackupFragment.class));
        // getSupportActionBar().addTab(backup);
        //
        // ActionBar.Tab restore = getSupportActionBar().newTab();
        // restore.setText("Restore");
        // restore.setTabListener(new MyTabListener(this, "restore",
        // RestoreFragment.class));
        // getSupportActionBar().addTab(restore);

        if (savedInstanceState == null) {
            check();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    public void check() {
        new CheckTask(this).execute();
    }

    public void noRoot() {
        getSupportActionBar().removeAllTabs();
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.error);
        mErrorMessage = (TextView) findViewById(R.id.error);
        mErrorMessage.setText(R.string.no_root);
    }

    public void notAOKP() {
        WarningDialog warning = new WarningDialog();
        warning.show(getSupportFragmentManager(), "warning");
    }

    public void addNotAokpMessage() {
        getSupportActionBar().removeAllTabs();
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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
                // return RESULT_NO_ROOT;
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

    public static class TabManager implements TabHost.OnTabChangeListener {
        private final FragmentActivity mActivity;
        private final TabHost mTabHost;
        private final int mContainerId;
        private final HashMap<String, TabInfo> mTabs = new HashMap<String, TabInfo>();
        TabInfo mLastTab;

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;
            private Fragment fragment;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabManager(FragmentActivity activity, TabHost tabHost, int containerId) {
            mActivity = activity;
            mTabHost = tabHost;
            mContainerId = containerId;
            mTabHost.setOnTabChangedListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mActivity));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state. If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            info.fragment = mActivity.getSupportFragmentManager().findFragmentByTag(tag);
            if (info.fragment != null && !info.fragment.isDetached()) {
                FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
                ft.detach(info.fragment);
                ft.commit();
            }

            mTabs.put(tag, info);
            mTabHost.addTab(tabSpec);
        }

        @Override
        public void onTabChanged(String tabId) {
            TabInfo newTab = mTabs.get(tabId);
            if (mLastTab != newTab) {
                FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
                if (mLastTab != null) {
                    if (mLastTab.fragment != null) {
                        ft.detach(mLastTab.fragment);
                    }
                }
                if (newTab != null) {
                    if (newTab.fragment == null) {
                        newTab.fragment = Fragment.instantiate(mActivity,
                                newTab.clss.getName(), newTab.args);
                        ft.add(mContainerId, newTab.fragment, newTab.tag);
                    } else {
                        ft.attach(newTab.fragment);
                    }
                }

                mLastTab = newTab;
                ft.commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        }
    }
}
