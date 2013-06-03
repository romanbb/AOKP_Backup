package com.aokp.backup.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.aokp.backup.BackupService;
import com.aokp.backup.R;
import com.aokp.backup.backup.Backup;
import com.aokp.backup.backup.BackupFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by roman on 6/3/13.
 */
public class BackupDetailFragment extends Fragment {

    Backup mBackup;

    public static BackupDetailFragment newDetailsFragment(Backup backup) {
        Bundle b = new Bundle();

        BackupDetailFragment f = new BackupDetailFragment();
        b.putString("path", backup.getZipFile().getAbsolutePath());
        f.setArguments(b);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey("path")) {
            String path = getArguments().getString("path");
            try {
                mBackup = BackupFactory.fromZipOrDirectory(getActivity(), new File(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.backup_details_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_backup:
                Intent deleteThatGuy = new Intent(getActivity(), BackupService.class);
                deleteThatGuy.setAction(BackupService.ACTION_DELETE_BACKUP);
                deleteThatGuy.putExtra("path", mBackup.getZipFile().getAbsolutePath());

                getActivity().startService(deleteThatGuy);
                getFragmentManager().popBackStack();
                return true;

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.backup_detail_fragment, container, false);


        return root;
    }
}
