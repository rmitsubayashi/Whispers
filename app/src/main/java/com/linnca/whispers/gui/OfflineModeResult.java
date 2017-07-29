package com.linnca.whispers.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.OfflineModeManager;
import com.linnca.whispers.R;

public class OfflineModeResult extends Fragment {
    public static final String BUNDLE_RESPONSE = "response";
    private String response;
    private Button nextButton;
    private TextView correctTextview;
    private TextView feedbackTextview;
    private OfflineModeResultListener listener;

    interface OfflineModeResultListener{
        void toNextRecording();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        response = bundle.getString(BUNDLE_RESPONSE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_offline_mode_result, container, false);
        correctTextview = (TextView)view.findViewById(R.id.offline_mode_result_correct);
        feedbackTextview = (TextView)view.findViewById(R.id.offline_mode_result_feedback);
        nextButton = (Button)view.findViewById(R.id.offline_mode_result_next_button);
        populateLayout();
        return view;
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        try {
            listener = (OfflineModeResultListener)getParentFragment();
        } catch (ClassCastException e){
            e.printStackTrace();
        }
    }


    private void populateLayout(){
        String correctAnswer = OfflineModeManager.getNextAnswer(getContext());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String toLearnLanguage = preferences.getString(
                getResources().getString(R.string.preferences_to_learn_language_key), null
        );
        if (ChainManager.checkAnswers(correctAnswer, response, toLearnLanguage)){
            correctTextview.setText(R.string.offline_mode_result_correct);
        } else {
            correctTextview.setText(R.string.offline_mode_result_incorrect);
            feedbackTextview.setText(getString(R.string.offline_mode_result_incorrect_feedback, correctAnswer));
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.toNextRecording();
            }
        });
    }
}
