package mugenglish.whispers.gui.widgets;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import mugenglish.whispers.gui.OnboardingIntroduction1;
import mugenglish.whispers.gui.OnboardingIntroduction2;

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

            default :
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }


}