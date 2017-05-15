package mugenglish.whispers.gui;

import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.OnboardingPagerAdapter;
import mugenglish.whispers.gui.widgets.ViewPagerCustomDuration;

public class OnboardingActivity extends AppCompatActivity {
    private int pageIndex = 0;
    private int maxPageIndex = 1;
    private Button nextButton;
    private Button finishButton;
    private List<ImageView> indicators = new ArrayList<>(2);

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
        indicators.add(indicator1);
        indicators.add(indicator2);

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
        final ArgbEvaluator evaluator = new ArgbEvaluator();
        final int color1 = ContextCompat.getColor(this, R.color.colorPrimary);
        final int color2 = ContextCompat.getColor(this, R.color.colorAccent);
        final int[] colorList = new int[]{color1, color2};

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                int colorUpdate = (Integer) evaluator.evaluate(positionOffset, colorList[position], colorList[position == maxPageIndex ? position : position + 1]);
                topView.setBackgroundColor(colorUpdate);
            }
            @Override
            public void onPageSelected(int position) {
                pageIndex = position;
                updateIndicators(pageIndex);
                topView.setBackgroundColor(colorList[position]);

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
        this.finish();
    }

}

