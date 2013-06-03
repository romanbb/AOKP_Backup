package com.aokp.backup.ui;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.aokp.backup.AOKPBackup;
import com.aokp.backup.BackupService.BackupFileSystemChange;
import com.aokp.backup.R.drawable;
import com.aokp.backup.R.menu;
import com.aokp.backup.backup.Backup;

import com.aokp.backup.R;
import com.squareup.otto.Subscribe;

import java.text.DateFormat;
import java.util.List;

/**
 * Created by roman on 6/2/13.
 */
public class BackupListFragment extends ListFragment {

    ArrayAdapter<Backup> mAdapter;
    LayoutInflater mInflater;

    Handler mHandler;

    private Runnable mRefreshDataRunnable = new Runnable() {
        @Override
        public void run() {
            getBackups();
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdapter = new BackupListArrayAdapter(getActivity(), R.id.backup_name, getBackups());
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.backup_list_fragment, menu);
    }

    @Override
    public void onPause() {
        super.onPause();
        AOKPBackup.getBus().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        AOKPBackup.getBus().register(this);
        refresh();
    }

    private List<Backup> getBackups() {
        return ((AOKPBackup) getActivity().getApplication()).findBackups();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
    }


    @Subscribe
    public void onBackupComplete(BackupFileSystemChange event) {
        if (event != null) {
            refresh();
        }
    }

    private void refresh() {
        mHandler.removeCallbacks(mRefreshDataRunnable);
        mHandler.post(mRefreshDataRunnable);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment, BackupDetailFragment.newDetailsFragment(mAdapter.getItem(position)), "backup_detail")
                .addToBackStack("backup_detail")
                .commit();
    }

    private class BackupListArrayAdapter extends ArrayAdapter<Backup> {

        public BackupListArrayAdapter(Context context, int textViewResourceId, List<Backup> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_row_backup, parent, false);
            }

            Backup b = getItem(position);

            TextView name = (TextView) convertView.findViewById(R.id.backup_name);
            TextView date = (TextView) convertView.findViewById(R.id.backup_date);
            TextView meta = (TextView) convertView.findViewById(R.id.meta_text);
            ImageView icon = (ImageView) convertView.findViewById(R.id.imageView);

            name.setText(b.getName());
            date.setText(DateFormat.getDateInstance().format(b.getBackupDate()));

            if (b.isOldStyleBackup()) {
                meta.setText(R.string.old_style_backup);
                icon.setImageResource(R.drawable.ic_backup_old);
            } else {
                meta.setText(R.string.backup);
                icon.setImageResource(drawable.ic_backup);
            }

            return convertView;
        }
    }

}
