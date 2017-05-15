package mugenglish.whispers.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OneSignal;

import java.util.Locale;

import data.ChainManager;
import data.FirebaseDBHeaders;
import data.LanguageIDs;
import mugenglish.whispers.R;

public class LanguageChooser extends AppCompatActivity {
    private Spinner spinner;
    private Button submitButton;
    private ProgressBar progressBar;
    private String languageToTeach;

    //how many credits the user starts off with.
    //only this is depleted, the user has to teach to get more credits
    public static long defaultCredits = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_chooser);

        spinner = (Spinner) findViewById(R.id.language_chooser_spinner);
        submitButton = (Button) findViewById(R.id.language_chooser_submit_button);
        progressBar = (ProgressBar) findViewById(R.id.language_chooser_progress_bar);

        languageToTeach = getToTeachLanguage();

        if (languageToTeach == null){
            Log.d(getClass().getCanonicalName(), "Language not supported");
        }

        addActionListeners();
    }

    private String getToTeachLanguage(){
        String language = Locale.getDefault().getLanguage();
        Log.d(getClass().getCanonicalName(), language);
        switch (language){
            case "en" :
                return LanguageIDs.ENGLISH;
            case "ja" :
            return LanguageIDs.JAPANESE;
            default:
                //language not supported
                return null;
        }
    }

    private void registerUser(final String toLearnLanguage, final String toTeachLanguage){
        Log.d("LanguageChooser","Registering user...");
        if (FirebaseAuth.getInstance().getCurrentUser() == null){
            FirebaseAuth.getInstance().signInAnonymously().addOnSuccessListener(
                    new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            String newUserID = authResult.getUser().getUid();
                            final DatabaseReference chatIDRef = FirebaseDatabase.getInstance().getReference(
                                    FirebaseDBHeaders.USER + "/" +
                                            newUserID + "/" +
                                            FirebaseDBHeaders.USER_ID_CHAT_ID
                            );

                            OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                                @Override
                                public void idsAvailable(String userId, String registrationId) {
                                    chatIDRef.setValue(userId);
                                }
                            });

                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(
                                    FirebaseDBHeaders.USER + "/" +
                                            newUserID
                            );

                            DatabaseReference toLearnRef = userRef.child(
                                            FirebaseDBHeaders.USER_ID_TO_LEARN_LANGUAGE_CODE
                            );
                            toLearnRef.setValue(toLearnLanguage);

                            DatabaseReference toTeachRef = userRef.child(
                                            FirebaseDBHeaders.USER_ID_TO_TEACH_LANGUAGE_CODE
                            );
                            toTeachRef.setValue(toTeachLanguage);

                            DatabaseReference creditsRef = userRef.child(
                                    FirebaseDBHeaders.USER_ID_CREDITS
                            );
                            creditsRef.setValue(defaultCredits);

                            DatabaseReference notificationTypeRef = userRef.child(
                                    FirebaseDBHeaders.USER_ID_NOTIFICATION_TYPE
                            );
                            //1 for now. might change later
                            notificationTypeRef.setValue(ChainManager.NOTIFICATION_TYPE_ONLY_NEXT);

                            //also set the preferences
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(
                                    getResources().getString(R.string.preferences_to_learn_language_key), toLearnLanguage
                            );
                            editor.putString(
                                    getResources().getString(R.string.preferences_to_teach_language_key), toTeachLanguage
                            );
                            //make sure this won't get called again
                            editor.putBoolean(getResources().getString(R.string.preferences_first_time_key), false);
                            //we commit instead of applying so
                            //the next page will be able to read it
                            boolean suppressError = editor.commit();
                            if (suppressError)
                                goToMainActivity();
                            else
                                Log.d("LanguageChooser","Could not commit the language preferences");

                        }
                    }
            ).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            Log.d("LanguageChooser","Could not register user because he was already registered");
        }
    }

    private void addActionListeners(){
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                int languagePosition = spinner.getSelectedItemPosition();
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

                registerUser(languageToLearn, languageToTeach);

                //this is done in the register user
                //because we want the user to go to the next page
                //only when the registration is reflected on FireBase
                //goToMainActivity();


            }
        });
    }

    private void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
