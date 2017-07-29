package com.linnca.whispers.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.R;

public class Splash extends AppCompatActivity {
    DatabaseReference onlineRef;
    ValueEventListener onlineListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (firstTime()){
            setup();
            return;
        }

        /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String toLearnLanguage = preferences.getString(
                getResources().getString(R.string.preferences_to_learn_language_key), null
        );
        OfflineModeManager.repopulateOfflineRecordings(getApplicationContext(), toLearnLanguage);
*/
        goToMainActivity();

    }

    private boolean firstTime(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return preferences.getBoolean(getResources().getString(R.string.preferences_first_time_key), true);
    }

    private void setup() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
