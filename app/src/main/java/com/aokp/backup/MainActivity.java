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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TextView;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends Activity {

    TextView mErrorMessage = null;
    boolean skipWarningDialog = false;

    TabHost mTabHost;
    TabManager mTabManager;
    AOKPBackup application;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (AOKPBackup) getApplicationContext();
//        Log.i("B", Restore.getRomControlPid());
        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        bar.addTab(bar.newTab()
                .setText("Backup")
                .setTabListener(new TabListener<BackupFragment>(
                        this, "backup", BackupFragment.class, savedInstanceState)));
        bar.addTab(bar.newTab()
                .setText("Restore")
                .setTabListener(new TabListener<RestoreFragment>(
                        this, "restore", RestoreFragment.class, savedInstanceState)));

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

        if (savedInstanceState == null) {
            check();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
        super.onSaveInstanceState(outState);
    }

    public void check() {
        new CheckTask(this).execute();
    }

    public void noRoot() {
        showError(R.string.no_root);
    }

    public void notAOKP() {
        WarningDialog warning = new WarningDialog();
        warning.show(getFragmentManager(), "warning");
    }

    public void addNotAokpMessage() {
        showError(R.string.not_aokp);
    }

    public void showError(int errorStringResourceId) {
        getActionBar().removeAllTabs();
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.error);
        mErrorMessage = (TextView) findViewById(R.id.error);
        mErrorMessage.setText(errorStringResourceId);
    }

    public static class WarningDialog extends DialogFragment {

        MainActivity mActivity;
        CheckBox dontShowAgain;
        CheckBox textRead;
        Button set;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mActivity = (MainActivity) getActivity();
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

    public class CheckTask extends AsyncTask<Void, Void, Integer> {

        MainActivity activity;

        final static int RESULT_NO_ROOT = 0;
        final static int RESULT_NOT_AOKP = 1;
        final static int RESULT_OK = 2;
        final static int RESULT_SD_NA = 3;

        public CheckTask(Activity a) {
            activity = (MainActivity) a;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (!Shell.SU.available()) {
                return RESULT_NO_ROOT;
            }

            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return RESULT_SD_NA;
            } else {
                File sdFolder = new File(Environment.getExternalStorageDirectory(), "AOKP_Backup");
                if (sdFolder.exists() && !new File(sdFolder, ".nomedia").exists()) {
                    try {
                        new File(sdFolder, ".nomedia").createNewFile();
                    } catch (IOException e) {
                    }
                }
            }

            if (Prefs.getShowNotAokpWarning(getApplicationContext())) {
                if (!application.isAndroidVersionSupported() || !application.isAOKPVersionSupported()) {
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
                case RESULT_SD_NA:
                    activity.showError(R.string.error_sd_not_mounted);
                    break;
                case RESULT_OK:
                    // tabs added by defualt
                    break;
            }

        }

    }

    public static class TabListener<T> implements ActionBar.TabListener {
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

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = (Fragment) Fragment.instantiate(mActivity, mClass.getName(),
                        mArgs);
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

    public static class TabManager implements TabHost.OnTabChangeListener {
        private final Activity mActivity;
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

        public TabManager(Activity activity, TabHost tabHost, int containerId) {
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
            info.fragment = mActivity.getFragmentManager().findFragmentByTag(tag);
            if (info.fragment != null && !info.fragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
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
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
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
                mActivity.getFragmentManager().executePendingTransactions();
            }
        }
    }
}
