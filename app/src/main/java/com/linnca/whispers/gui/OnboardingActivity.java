package com.linnca.whispers.gui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.OnboardingPagerAdapter;
import com.linnca.whispers.gui.widgets.ViewPagerCustomDuration;

public class OnboardingActivity extends AppCompatActivity {
    private int pageIndex = 0;
    private int maxPageIndex = 2;
    private Button nextButton;
    private Button finishButton;
    private List<ImageView> indicators = new ArrayList<>(3);

    private ViewGroup topView;
    private ViewPagerCustomDuration viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        topView = (ViewGroup)findViewById(R.id.onboarding_top_view);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPagerCustomDuration) findViewById(R.id.onboarding_viewpager);
        nextButton = (Button) findViewById(R.id.onboarding_next);
        finishButton = (Button) findViewById(R.id.onboarding_finish);
        viewPager.setAdapter(adapter);
        ImageView indicator1 = (ImageView) findViewById(R.id.onboarding_indicator1);
        ImageView indicator2 = (ImageView) findViewById(R.id.onboarding_indicator2);
        ImageView indicator3 = (ImageView) findViewById(R.id.onboarding_indicator3);
        indicators.add(indicator1);
        indicators.add(indicator2);
        indicators.add(indicator3);

        setActionListeners();

    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.size(); i++) {
            indicators.get(i).setBackgroundResource(
                    i == position ? R.drawable.orange_circle : R.drawable.white_circle
            );
        }
    }

    private void setActionListeners(){
        //for changing background color smoothly
        final ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(),
                ContextCompat.getColor(this, R.color.colorPrimaryDark),
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorAccent)
        );
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                topView.setBackgroundColor((Integer)animator.getAnimatedValue());
            }

        });
        // (3 - 1) = number of pages minus 1
        animator.setDuration((3 - 1) * 10000000000L);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                animator.setCurrentPlayTime((long)((positionOffset + position)* 10000000000L));
            }
            @Override
            public void onPageSelected(int position) {
                pageIndex = position;
                updateIndicators(pageIndex);

                nextButton.setVisibility(position == maxPageIndex ? View.GONE : View.VISIBLE);
                finishButton.setVisibility(position == maxPageIndex ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPage();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextFragment();
            }
        });

    }

    private void nextFragment(){
        pageIndex = pageIndex + 1;
        //transition is too fast so slow it down
        viewPager.setScrollDurationFactor(5);
        viewPager.setCurrentItem(pageIndex);
        //this also affects the speed of manual swipe (which is normal)
        //so set it back to default
        viewPager.setScrollDurationFactor(1);
    }

    private void nextPage(){
        Intent intent = new Intent(this, LanguageChooser.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        //need to make sure this is popped before the application starts
        //but during the tutorial, the user should be able to go back to this screen
    }

}

