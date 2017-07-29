package com.linnca.whispers.gui.tutorial;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.tutorial.widgets.TutorialChainInfoPagerAdapter;

public class TutorialChainInfo extends Fragment {
    public static final String BUNDLE_AUDIO_PATH = "audioPath";
    public static final String BUNDLE_ANSWER = "answer";
    public static final String BUNDLE_LANGUAGE_CODE = "languageCode";
    private String audioPath;
    private String answer;
    private String languageCode;
    ViewPager viewPager;
    TabLayout tabLayout;
    FragmentPagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle.getString(BUNDLE_AUDIO_PATH) != null){
            audioPath = bundle.getString(BUNDLE_AUDIO_PATH);
        }

        if (bundle.getString(BUNDLE_ANSWER) != null){
            answer = bundle.getString(BUNDLE_ANSWER);
        }

        languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_info, container, false);
        viewPager = (ViewPager)view.findViewById(R.id.chain_info_view_pager);
        tabLayout = (TabLayout)view.findViewById(R.id.chain_info_tab_layout);
        populatePager();
        return view;
    }

    private void populatePager(){
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        adapter = new TutorialChainInfoPagerAdapter(getChildFragmentManager(), audioPath, answer, languageCode);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    }

}
