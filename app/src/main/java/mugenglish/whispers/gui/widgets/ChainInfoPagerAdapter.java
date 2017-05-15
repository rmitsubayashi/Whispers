package mugenglish.whispers.gui.widgets;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import data.datawrappers.MinimalChain;
import mugenglish.whispers.gui.ChainInfoChat;
import mugenglish.whispers.gui.ChainInfoRecordings;

public class ChainInfoPagerAdapter extends FragmentPagerAdapter {
    private MinimalChain minimalChain;

    public ChainInfoPagerAdapter(FragmentManager manager, MinimalChain minimalChain){
        super(manager);
        this.minimalChain = minimalChain;
    }

    @Override
    public Fragment getItem(int position){
        Fragment fragment = null;
        Bundle bundle = new Bundle();
        switch (position){
            case 0:
                fragment = new ChainInfoRecordings();
                bundle.putSerializable(ChainInfoRecordings.CHAIN_INFO_RECORDINGS_MINIMAL_CHAIN, minimalChain);
                fragment.setArguments(bundle);
                return fragment;
            case 1:
                fragment = new ChainInfoChat();
                bundle.putSerializable(ChainInfoChat.CHAIN_INFO_CHAT_MINIMAL_CHAIN, minimalChain);
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
