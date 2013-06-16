package com.aokp.backup;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import com.aokp.backup.BackupService.BackupFileSystemChange;
import com.aokp.backup.R.id;
import com.aokp.backup.backup.BackupFactory;
import com.aokp.backup.ui.BackupListFragment;
import com.aokp.backup.ui.Prefs;
import com.aokp.backup.ui.SlidingCheckboxView;
import com.aokp.backup.util.Tools;
import com.dropbox.sync.android.DbxAccountManager;
import com.squareup.otto.Subscribe;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupActivity extends Activity {

    private static final String TAG = BackupActivity.class.getSimpleName();
    private static final int REQUEST_LINK_TO_DBX = 5;

    SlidingCheckboxView mSlidingCats;

    private EditText mNewBackupNameEditText;
    private ImageView mNewBackupSaveButton;

    MenuItem mSyncWithDropboxMenuItem;
    MenuItem mUseExternalStorageMenuItem;
    MenuItem mBackupMenuItem;

    BackupListFragment mBackupListFragment;

    private DbxAccountManager mDbxAcctMgr;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_frame);

        mSlidingCats = (SlidingCheckboxView) findViewById(R.id.sliding_checkboxes);
        mSlidingCats.setVisibility(View.GONE);
        mSlidingCats.init(BackupFactory.getCategoryArrayResourceId());
        mSlidingCats.bringToFront();


        mBackupListFragment = new BackupListFragment();
        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment, mBackupListFragment)
                .commit();

        if (getIntent() != null && getIntent().hasExtra("restore_completed")) {
            showRestoreCompleteDialog();
        }

        if (BuildConfig.DROPBOX_ENABLED) {
            mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(),
                    getString(R.string.dropbox_app_key),
                    getString(R.string.dropbox_app_secret));
            startService(new Intent(BackupActivity.this, DropboxSyncService.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AOKPBackup.getBus().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AOKPBackup.getBus().unregister(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                // ... Start using Dropbox files.
                mSyncWithDropboxMenuItem.setChecked(true);
                startService(new Intent(BackupActivity.this, DropboxSyncService.class));
            } else {
                // ... Link failed or was cancelled by the user.
                mSyncWithDropboxMenuItem.setChecked(false);
                Toast.makeText(BackupActivity.this, "Link with Dropbox cancelled or failed", Toast.LENGTH_SHORT).show();
                startService(new Intent(DropboxSyncService.ACTION_UNLINK));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Subscribe
    public void onBackupFileSystemChange(BackupFileSystemChange event) {
        if (mUseExternalStorageMenuItem != null) {
            mUseExternalStorageMenuItem.setChecked(Prefs.getUseExternalStorage(this));
        }
    }

    private void showRestoreCompleteDialog() {
        new Builder(this)
                .setTitle("Restore complete!")
                .setMessage("You should reboot!")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                Shell.SU.run("reboot");
                            }
                        });
                    }
                })
                .setNegativeButton("No", null)
                .create()
                .show();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.use_external_storage:
                mUseExternalStorageMenuItem.setChecked(!mUseExternalStorageMenuItem.isChecked());
                Prefs.setUseExternalStorage(this, mUseExternalStorageMenuItem.isChecked());
                AOKPBackup.getBus().post(new BackupFileSystemChange());
                return true;

            case R.id.sync_with_dropbox:
                if (mSyncWithDropboxMenuItem.isChecked()) {

                    // unlink
                    startService(new Intent(DropboxSyncService.ACTION_UNLINK));
                    mSyncWithDropboxMenuItem.setChecked(false);
                } else {
                    // link account
                    mDbxAcctMgr.startLink((Activity) BackupActivity.this, REQUEST_LINK_TO_DBX);
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.backup_activity, menu);

        mSyncWithDropboxMenuItem = menu.findItem(id.sync_with_dropbox);
        if (BuildConfig.DROPBOX_ENABLED) {
            mSyncWithDropboxMenuItem.setChecked(mDbxAcctMgr.hasLinkedAccount());
            mSyncWithDropboxMenuItem.setVisible(true);
        } else {
            mSyncWithDropboxMenuItem.setVisible(false);
        }

        mUseExternalStorageMenuItem = menu.findItem(R.id.use_external_storage);
        mUseExternalStorageMenuItem.setChecked(Prefs.getUseExternalStorage(this));

        mBackupMenuItem = menu.findItem(R.id.menu_backup_go);

        mNewBackupNameEditText = (EditText) mBackupMenuItem.getActionView().findViewById(R.id.save_name);
        mNewBackupNameEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {


                return false;
            }
        });
        mNewBackupSaveButton = (ImageView) mBackupMenuItem.getActionView().findViewById(R.id.save);
        mNewBackupSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mNewBackupNameEditText.getText().toString();
                if (text == null || text.isEmpty()) {
                    text = mNewBackupNameEditText.getHint().toString();
                }

                if (mBackupMenuItem != null) {
                    mBackupMenuItem.collapseActionView();
                }

                if (text != null && !text.isEmpty()) {
                    final String backupName = text.trim();
                    Intent dobackup = new Intent(BackupActivity.this, BackupService.class);
                    dobackup.setAction(BackupService.ACTION_NEW_BACKUP);
                    dobackup.putExtra("name", backupName);
                    mSlidingCats.addCategoryFilter(dobackup);
                    startService(dobackup);
                }
            }
        });

        mBackupMenuItem.setOnActionExpandListener(new OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // slide in categories
                slideInCategories();


                if (mNewBackupNameEditText != null) {
                    mNewBackupNameEditText.setHint(getNewBackupNameHint());
                }
                invalidateOptionsMenu();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (mSlidingCats != null) {
                    slideOutCategories();

                }
                invalidateOptionsMenu();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);


    }

    private String getNewBackupNameHint() {
        // suggest new backup name
        Date today = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE_MMM_d");
        String name = sdf.format(today);

        int uniqueCounter = 1;

        while (backupExists(name)) {
            name = sdf.format(today) + "-" + uniqueCounter++;

            if (uniqueCounter > 10) {
                return "";
            }
        }

        return name;
    }

    private boolean backupExists(String name) {
        return new File(Tools.getBackupDirectory(this), name).exists()
                || new File(Tools.getBackupDirectory(this), name + ".zip").exists();
    }

    public void slideOutCategories() {
        if (mSlidingCats != null) {
            mSlidingCats.slideOutCategories();
        }
    }

    public void slideInCategories() {
        if (mSlidingCats != null) {
            mSlidingCats.slideInCategories();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mUseExternalStorageMenuItem.setVisible(!mBackupMenuItem.isActionViewExpanded());
        mBackupMenuItem.setVisible(mBackupListFragment.isVisible());
        return super.onPrepareOptionsMenu(menu);
    }
}