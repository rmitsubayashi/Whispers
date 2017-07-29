package com.linnca.whispers.gui.tutorial;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.linnca.whispers.R;

public class TutorialSituationList extends Fragment {
    private TutorialSituationListListener listener;

    public interface TutorialSituationListListener {
        void tutorialSituationListToTutorialLearnStart();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        //can be the same as the original, but a recycler view with only one item?
        View view = inflater.inflate(R.layout.fragment_tutorial_situation_list, container, false);
        LinearLayout situationItem = (LinearLayout)view.findViewById(R.id.tutorial_situation_list_item);

        situationItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.tutorialSituationListToTutorialLearnStart();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        implementListeners(context);
    }

    //must implement to account for lower APIs
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        implementListeners(activity);
    }

    private void implementListeners(Context context){
        try {
            listener = (TutorialSituationListListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

}
