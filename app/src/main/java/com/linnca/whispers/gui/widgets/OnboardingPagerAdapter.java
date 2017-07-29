package com.linnca.whispers.gui.widgets;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.linnca.whispers.gui.OnboardingIntroduction1;
import com.linnca.whispers.gui.OnboardingIntroduction2;
import com.linnca.whispers.gui.OnboardingIntroduction3;

public class OnboardingPagerAdapter extends FragmentPagerAdapter {

    public OnboardingPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0 :
                return new OnboardingIntroduction1();
            case 1 :
                return new OnboardingIntroduction2();
            case 2 :
                return new OnboardingIntroduction3();

            default :
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }


}