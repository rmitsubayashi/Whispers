package com.linnca.whispers.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.linnca.whispers.data.UserManager;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.Onboarding2PagerAdapter;
import com.linnca.whispers.gui.widgets.ViewPagerCustomDuration;

public class OnboardingActivity2 extends AppCompatActivity {
    private int pageIndex = 0;
    private int maxPageIndex = 2;
    private Button nextButton;
    private Button finishButton;
    private List<ImageView> indicators = new ArrayList<>(3);
    private ProgressBar progressBar;
    private TextView progressBarTitle;
    private ViewGroup bottomNavigationView;

    //keep the language the user chose in the previous activity
    private String languageToLearn;
    public static final String KEY_LANGUAGE_TO_LEARN = "languageToLearn";

    private ViewPagerCustomDuration viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding2);

        if (getIntent().hasExtra(KEY_LANGUAGE_TO_LEARN)){
            languageToLearn = getIntent().getStringExtra(KEY_LANGUAGE_TO_LEARN);
        } else {
            finish();
        }

        viewPager = (ViewPagerCustomDuration) findViewById(R.id.onboarding2_viewpager);
        nextButton = (Button) findViewById(R.id.onboarding2_next);
        finishButton = (Button) findViewById(R.id.onboarding2_finish);
        progressBar = (ProgressBar) findViewById(R.id.onboarding2_progress_bar);
        progressBarTitle = (TextView) findViewById(R.id.onboarding2_progress_bar_textview);
        bottomNavigationView = (ViewGroup) findViewById(R.id.onboarding2_bottom_navigation);

        // Set up the ViewPager with the sections adapter.
        Onboarding2PagerAdapter adapter = new Onboarding2PagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        ImageView indicator1 = (ImageView) findViewById(R.id.onboarding2_indicator1);
        ImageView indicator2 = (ImageView) findViewById(R.id.onboarding2_indicator2);
        ImageView indicator3 = (ImageView) findViewById(R.id.onboarding2_indicator3);
        indicators.add(indicator1);
        indicators.add(indicator2);
        indicators.add(indicator3);

        setActionListeners();

    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LANGUAGE_TO_LEARN, languageToLearn);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        languageToLearn = savedInstanceState.getString(KEY_LANGUAGE_TO_LEARN);
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.size(); i++) {
            indicators.get(i).setBackgroundResource(
                    i == position ? R.drawable.orange_circle : R.drawable.gray_circle
            );
        }
    }

    private void setActionListeners(){
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
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
                //just making sure the user can't double click
                finishButton.setEnabled(false);
                showLoadingUI();
                UserManager.registerUser(
                        new UserManager.OnRegisterUserListener() {
                            @Override
                            public void onRegisterUser() {
                                goToMainActivity();
                            }
                        },
                        languageToLearn,
                        getApplicationContext()
                );
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

    public void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        //we don't want the user to be able to go back to the tutorial
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showLoadingUI(){
        bottomNavigationView.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        progressBarTitle.setVisibility(View.VISIBLE);
    }

}

