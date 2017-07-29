package com.linnca.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterEndListener;

public class FinalAnswer extends Fragment {
    private final String SAVED_STATE_USER_INPUT = "userInput";

    private ActionAfterEndListener actionAfterEndListener;
    private Button confirmButton;
    private EditText userInputEditText;
    private TextView errorTextview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_final_answer, container, false);
        confirmButton =(Button) view.findViewById(R.id.final_answer_confirm_button);
        userInputEditText = (EditText) view.findViewById(R.id.final_answer_edit_text);
        errorTextview = (TextView) view.findViewById(R.id.final_answer_error_message);

        if (savedInstanceState != null &&
                savedInstanceState.getString(SAVED_STATE_USER_INPUT) != null){
            userInputEditText.setText(savedInstanceState.getString(SAVED_STATE_USER_INPUT));
        }

        addActionListeners();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment parentFragment = getParentFragment();
        implementListeners(parentFragment);
    }

    //must implement to account for lower APIs
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Fragment parentFragment = getParentFragment();
        implementListeners(parentFragment);
    }

    private void implementListeners(Fragment fragment){
        try {
            actionAfterEndListener = (ActionAfterEndListener) fragment;
        } catch (Exception e){
            throw new ClassCastException(fragment.toString() + " must implement listener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        if (userInputEditText.getText().length() != 0){
            outState.putString(
                    SAVED_STATE_USER_INPUT, userInputEditText.getText().toString()
            );
        }

    }


    private void addActionListeners(){
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onConfirm();
            }
        });
    }

    private void onConfirm(){
        String userInput = userInputEditText.getText().toString();
        if (userInput.length() != 0) {
            //no recording (null) because this is the final answer
            actionAfterEndListener.saveData(null, userInput);
            actionAfterEndListener.redirectUser();
        } else {
            errorTextview.setText(R.string.final_answer_error_message);
        }
    }
}

