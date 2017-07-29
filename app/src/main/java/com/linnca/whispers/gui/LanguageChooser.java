package com.linnca.whispers.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.linnca.whispers.data.LanguageIDs;
import com.linnca.whispers.data.UserManager;
import com.linnca.whispers.R;

public class LanguageChooser extends AppCompatActivity {
    private Spinner spinner;
    private Button submitButton;
    private String languageToTeach;
    //there has to be a better way..
    private int shiftIndex = -1;

    //how many credits the user starts off with.
    //once this is depleted, the user has to teach to get more credits
    public static long defaultCredits = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_chooser);

        spinner = (Spinner) findViewById(R.id.language_chooser_spinner);
        submitButton = (Button) findViewById(R.id.language_chooser_submit_button);

        languageToTeach = UserManager.getToTeachLanguage();
        populateSpinner();

        if (languageToTeach == null){
            Log.d(getClass().getCanonicalName(), "Language not supported");
        }

        addActionListeners();
    }

    private void populateSpinner(){
        List<String> languages = new ArrayList<>(
                Arrays.asList(getResources().getStringArray(R.array.language_chooser_languages))
        );
        switch (languageToTeach){
            case LanguageIDs.JAPANESE :
                languages.remove(0);
                shiftIndex = 0;
                break;
            case LanguageIDs.ENGLISH :
                languages.remove(1);
                shiftIndex = 1;
                break;
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinner.setAdapter(spinnerArrayAdapter);
    }

    private void addActionListeners(){
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int languagePosition = spinner.getSelectedItemPosition();
                languagePosition = languagePosition >= shiftIndex ? languagePosition+1 : languagePosition;
                String languageToLearn = "";
                switch (languagePosition){
                    case 0 :
                        languageToLearn = LanguageIDs.JAPANESE;
                        break;
                    case 1:
                        languageToLearn = LanguageIDs.ENGLISH;
                        break;
                    default:
                        //throw error
                        break;
                }
                goToOnboarding2(languageToLearn);
            }
        });
    }

    private void goToOnboarding2(String languageToLearn){
        Intent intent = new Intent(this, OnboardingActivity2.class);
        intent.putExtra(OnboardingActivity2.KEY_LANGUAGE_TO_LEARN, languageToLearn);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        //need to make sure this is popped before the application starts
        //but during the tutorial, the user should be able to go back to this screen
    }
}
