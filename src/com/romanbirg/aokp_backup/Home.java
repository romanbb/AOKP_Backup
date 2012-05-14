
package com.romanbirg.aokp_backup;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

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
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            return inflater.inflate(R.layout.main, null);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

            inflater.inflate(R.menu.backup, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    public static class RestoreFragment extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
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
