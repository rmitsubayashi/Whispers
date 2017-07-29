package com.linnca.whispers.gui.widgets;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.linnca.whispers.gui.Onboarding2HowToPlay1;
import com.linnca.whispers.gui.Onboarding2HowToPlay2;
import com.linnca.whispers.gui.Onboarding2HowToPlay3;

public class Onboarding2PagerAdapter extends FragmentPagerAdapter {

    public Onboarding2PagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0 :
                return new Onboarding2HowToPlay1();
            case 1 :
                return new Onboarding2HowToPlay2();
            case 2 :
                return new Onboarding2HowToPlay3();

            default :
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}