
package com.romanbirg.aokp_backup;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.aokp.backup.R;

public class Home extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        if (savedInstanceState != null) {
            getActionBar().setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

        new CheckTask(this).execute();

    }

    class CheckTask extends AsyncTask<Void, Void, Void> {

        Activity activity;

        public CheckTask(Activity a) {
            activity = a;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!new ShellCommand().canSU()) {
                activity.getActionBar().removeAllTabs();
                activity.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                activity.getActionBar().setDisplayShowHomeEnabled(true);
                activity.setContentView(R.layout.error);
                TextView error = (TextView) activity.findViewById(R.id.error);
                error.setText(R.string.no_root);
                return null;
            }

            if (!Tools.getROMVersion().startsWith("aokp")) {
                activity.getActionBar().removeAllTabs();
                activity.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                activity.getActionBar().setDisplayShowHomeEnabled(true);
                activity.setContentView(R.layout.error);
                TextView error = (TextView) activity.findViewById(R.id.error);
                error.setText(R.string.not_aokp);
                return null;
            }

            return null;
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
