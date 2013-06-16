package com.aokp.backup.ui;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import com.aokp.backup.R;
import com.aokp.backup.R.dimen;
import com.aokp.backup.R.id;

import java.util.ArrayList;

/**
 * Created by roman on 6/1/13.
 */
public class SlidingCheckboxView extends FrameLayout {

    String[] mChecks;
    private AnimatorSet mAnimatorSet;
    private ViewGroup mAttachToMe;

    Animation mSlideInTopAnimation;
    Animation mSlideOutTopAnimation;

    public SlidingCheckboxView(Context context) {
        super(context);
    }

    public SlidingCheckboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingCheckboxView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(int checkBoxResourceArray) {

        mSlideInTopAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_top);
        mSlideOutTopAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_top);

        mSlideOutTopAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                SlidingCheckboxView.this.setVisibility(View.INVISIBLE);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        View.inflate(getContext(), R.layout.sliding_checkbox_layout, this);

        mChecks = getResources().getStringArray(checkBoxResourceArray);
//        mCheckBoxes = new CheckBox[mChecks.length];

        int height = getContext().getResources().getDimensionPixelSize(dimen.checkbox_height);


        mAttachToMe = (ViewGroup) findViewById(id.attach_checkbox_root);
        for (int i = 0; i < mChecks.length; i++) {
            CheckBox newbox = new CheckBox(getContext());
            newbox.setText(mChecks[i]);
            newbox.setTag(mChecks[i]);
            newbox.setChecked(true);
            newbox.setHeight(height);
            mAttachToMe.addView(newbox);
        }
    }

    public void addCategoryFilter(Intent backupIntent) {
        ArrayList<Integer> categoryFilter = new ArrayList<Integer>();
        boolean addFilter = false;

        if (mChecks != null) {
            for (int i = 0; i < mChecks.length; i++) {
                CheckBox box = (CheckBox) mAttachToMe.findViewWithTag(mChecks[i]);
                if (box != null) {
                    if (!box.isChecked()) {
                        addFilter = true;
                    } else {
                        categoryFilter.add(i);
                    }
                }
            }
            if (addFilter && backupIntent != null) {
                backupIntent.putIntegerArrayListExtra("category_filter", categoryFilter);
            }
        }
    }

    public void slideOutCategories() {
        mSlideOutTopAnimation.setFillEnabled(true);
        startAnimation(mSlideOutTopAnimation);
    }

    public void slideInCategories() {
        startAnimation(mSlideInTopAnimation);
        setVisibility(View.VISIBLE);
    }

}
