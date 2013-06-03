package com.aokp.backup;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.aokp.backup.backup.Backup;
import com.aokp.backup.backup.BackupFactory;
import com.aokp.backup.ui.BackupListFragment;
import com.aokp.backup.ui.SlidingCheckboxView;
import com.squareup.otto.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BackupActivity extends Activity {

    private static final String TAG = BackupActivity.class.getSimpleName();
    SlidingCheckboxView mSlidingCats;
    Animation mSlideInTopAnimation;
    Animation mSlideOutTopAnimation;
    private EditText mNewBackupNameEditText;
    private ImageView mNewBacupSaveButton;

    MenuItem mPrefsMenuItem;
    MenuItem mBackupMenuItem;

    BackupListFragment mBackupListFragment;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_frame);

        mSlidingCats = (SlidingCheckboxView) findViewById(R.id.sliding_checkboxes);
        mSlidingCats.setVisibility(View.GONE);
        mSlidingCats.init(R.array.jbmr1_categories); // TODO get proper array!
        mSlidingCats.bringToFront();

        mSlideInTopAnimation = AnimationUtils.loadAnimation(BackupActivity.this, R.anim.slide_in_top);
        mSlideOutTopAnimation = AnimationUtils.loadAnimation(BackupActivity.this, R.anim.slide_out_top);


        mBackupListFragment = new BackupListFragment();
        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment, mBackupListFragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.backup_activity, menu);

        mPrefsMenuItem = menu.findItem(R.id.prefs);
        mBackupMenuItem = menu.findItem(R.id.menu_backup_go);

        mNewBackupNameEditText = (EditText) mBackupMenuItem.getActionView().findViewById(R.id.save_name);
        mNewBackupNameEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {


                return false;
            }
        });
        mNewBacupSaveButton = (ImageView) mBackupMenuItem.getActionView().findViewById(R.id.save);
        mNewBacupSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mNewBackupNameEditText.getText().toString();
                if (text == null || text.isEmpty()) {
                    text = mNewBackupNameEditText.getHint().toString();
                }

                final String backupName = text;

                // start backup
                Intent dobackup = new Intent(BackupActivity.this, BackupService.class);
                dobackup.setAction(BackupService.ACTION_NEW_BACKUP);
                dobackup.putExtra("name", backupName);
                startService(dobackup);
            }
        });

        mBackupMenuItem.setOnActionExpandListener(new OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // slide in categories
                if (mSlidingCats != null) {
                    mSlidingCats.startAnimation(mSlideInTopAnimation);
                    mSlidingCats.setVisibility(View.VISIBLE);
                }


                if (mNewBackupNameEditText != null) {
                    // suggest new backup name
                    Date today = new Date(System.currentTimeMillis());
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE_MMM_d");

                    mNewBackupNameEditText.setHint(sdf.format(today));
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
                    mSlideOutTopAnimation.setFillEnabled(true);
                    mSlidingCats.startAnimation(mSlideOutTopAnimation);

                }
                invalidateOptionsMenu();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);


    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mPrefsMenuItem.setVisible(!mBackupMenuItem.isActionViewExpanded());
        mBackupMenuItem.setVisible(mBackupListFragment.isVisible());
        return super.onPrepareOptionsMenu(menu);
    }
}