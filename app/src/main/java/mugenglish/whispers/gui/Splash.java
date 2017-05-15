package mugenglish.whispers.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import data.ContentManager;
import data.FirebaseDBHeaders;
import mugenglish.whispers.R;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //testing population of content
        //ContentManager.run();

        if (firstTime()){
            setup();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    private boolean firstTime(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return preferences.getBoolean(getResources().getString(R.string.preferences_first_time_key), true);
    }

    private void setup() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        startActivity(intent);
    }

    //just copy/pasted code. probably doesn't make sense yet
    private void getLanguageFromDatabase(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference languageCodeRef = database.getReference(
                FirebaseDBHeaders.USER + "/" +
                        userID + "/" +
                        FirebaseDBHeaders.USER_ID_TO_LEARN_LANGUAGE_CODE
        );

        languageCodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //languageCode = (int)dataSnapshot.getValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
