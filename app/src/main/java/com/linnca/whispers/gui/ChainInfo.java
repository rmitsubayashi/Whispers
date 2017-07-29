package com.linnca.whispers.gui;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ChainInfoPagerAdapter;

public class ChainInfo extends Fragment {
    private MinimalChain minimalChain;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FragmentPagerAdapter adapter;

    public static String BUNDLE_MINIMAL_CHAIN = "minimalChain";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        minimalChain = (MinimalChain)bundle.getSerializable(BUNDLE_MINIMAL_CHAIN);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_info, container, false);
        viewPager = (ViewPager)view.findViewById(R.id.chain_info_view_pager);
        tabLayout = (TabLayout)view.findViewById(R.id.chain_info_tab_layout);
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_chain_info);
        populatePager();
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

        adapter = new ChainInfoPagerAdapter(getChildFragmentManager(), minimalChain);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    }


}
