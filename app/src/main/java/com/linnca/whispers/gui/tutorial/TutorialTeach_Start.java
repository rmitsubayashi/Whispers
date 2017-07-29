package com.linnca.whispers.gui.tutorial;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.PhraseDisplay;
import com.linnca.whispers.gui.widgets.ActionAfterStartListener;

public class TutorialTeach_Start extends Fragment implements ActionAfterStartListener {


    public interface TutorialTeach_StartListener {
        void tutorialTeachStartToTutorialLinks();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_wrapper, container, false);
        ProgressBar progressBar = (ProgressBar)view.findViewById(R.id.fragment_progress_bar);
        progressBar.setVisibility(View.GONE);
        addPronounceNewPhraseUI();
        return view;
    }

    @Override
    public void continueToEnd(){
        //do nothing because the user will not be clicking this
    }


    private void addPronounceNewPhraseUI(){
        String phrase = getString(R.string.tutorial_teach_start_phrase);

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment phraseDisplay = new PhraseDisplay();
        Bundle bundle = new Bundle();
        bundle.putString(PhraseDisplay.BUNDLE_PHRASE, phrase);
        phraseDisplay.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, phraseDisplay);
        fragmentTransaction.commit();
    }
}
