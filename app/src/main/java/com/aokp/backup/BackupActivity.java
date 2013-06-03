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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.aokp.backup.BackupService.BackupFileSystemChange;
import com.aokp.backup.backup.BackupFactory;
import com.aokp.backup.ui.BackupListFragment;
import com.aokp.backup.ui.Prefs;
import com.aokp.backup.ui.SlidingCheckboxView;
import com.aokp.backup.util.Tools;
import com.squareup.otto.Subscribe;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupActivity extends Activity {

    private static final String TAG = BackupActivity.class.getSimpleName();
    SlidingCheckboxView mSlidingCats;
    Animation mSlideInTopAnimation;
    Animation mSlideOutTopAnimation;
    private EditText mNewBackupNameEditText;
    private ImageView mNewBackupSaveButton;

    MenuItem mUseExternalStorageMenuItem;
    MenuItem mBackupMenuItem;

    BackupListFragment mBackupListFragment;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_frame);

        mSlidingCats = (SlidingCheckboxView) findViewById(R.id.sliding_checkboxes);
        mSlidingCats.setVisibility(View.GONE);
        mSlidingCats.init(BackupFactory.getCategoryArrayResourceId());
        mSlidingCats.bringToFront();

        mSlideInTopAnimation = AnimationUtils.loadAnimation(BackupActivity.this, R.anim.slide_in_top);
        mSlideOutTopAnimation = AnimationUtils.loadAnimation(BackupActivity.this, R.anim.slide_out_top);


        mBackupListFragment = new BackupListFragment();
        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment, mBackupListFragment)
                .commit();

        if (getIntent() != null && getIntent().hasExtra("restore_completed")) {
            showRestoreCompleteDialog();
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.backup_activity, menu);

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
                    mSlideOutTopAnimation.setAnimationListener(new AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mSlidingCats.setVisibility(View.INVISIBLE);

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
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

    private void slideOutCategories() {
        if (mSlidingCats != null) {
            mSlideOutTopAnimation.setFillEnabled(true);
            mSlidingCats.startAnimation(mSlideOutTopAnimation);
        }
    }

    private void slideInCategories() {
        if (mSlidingCats != null) {
            mSlidingCats.startAnimation(mSlideInTopAnimation);
            mSlidingCats.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mUseExternalStorageMenuItem.setVisible(!mBackupMenuItem.isActionViewExpanded());
        mBackupMenuItem.setVisible(mBackupListFragment.isVisible());
        return super.onPrepareOptionsMenu(menu);
    }
}