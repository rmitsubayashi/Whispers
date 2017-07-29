package com.linnca.whispers.gui.tutorial.widgets;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.linnca.whispers.gui.tutorial.TutorialChainInfoChat;
import com.linnca.whispers.gui.tutorial.TutorialChainInfoRecordings;

public class TutorialChainInfoPagerAdapter extends FragmentPagerAdapter {
    private String audioPath;
    private String answer;
    private String languageCode;

    public TutorialChainInfoPagerAdapter(FragmentManager manager, String audioPath, String answer, String languageCode){
        super(manager);
        this.audioPath = audioPath;
        this.answer = answer;
        this.languageCode = languageCode;
    }

    @Override
    public Fragment getItem(int position){
        Fragment fragment;
        Bundle bundle = new Bundle();
        switch (position){
            case 0:
                fragment = new TutorialChainInfoRecordings();
                bundle.putString(TutorialChainInfoRecordings.BUNDLE_AUDIO_PATH, audioPath);
                bundle.putString(TutorialChainInfoRecordings.BUNDLE_ANSWER, answer);
                bundle.putString(TutorialChainInfoRecordings.BUNDLE_LANGUAGE_CODE, languageCode);
                fragment.setArguments(bundle);
                return fragment;
            case 1:
                fragment = new TutorialChainInfoChat();
                bundle.putString(TutorialChainInfoChat.BUNDLE_LANGUAGE_CODE, languageCode);
                fragment.setArguments(bundle);
                return fragment;
            default:
                return null;
        }

    }

    @Override
    public int getCount(){
        return 2;
    }
}
