package mugenglish.whispers.gui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import data.FirebaseDBHeaders;
import data.datawrappers.ChainQueue;
import data.datawrappers.MinimalChain;
import mugenglish.whispers.R;

public class MainActivity extends AppCompatActivity
implements
        SituationList.SituationListListener,
        Learn_Start.Learn_StartListener,
        Learn_End.Learn_EndListener,
        Teach_Start.Teach_StartListener,
        Teach_End.Teach_EndListener,
        ChainList.ChainListListener
{

    private String toLearnLanguage;
    private String toTeachLanguage;

    private BottomNavigationView nav;

    private String userID;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setLanguages();

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        database = FirebaseDatabase.getInstance();

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        nav = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        //can't figure
        nav.setItemIconTintList(null);
        nav.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment fragment = null;
                    Bundle bundle = new Bundle();
                    switch (item.getItemId()) {
                        case R.id.bottom_navigation_learn:
                            fragment = new SituationList();
                            bundle.putString(SituationList.BUNDLE_DISPLAY_LANGUAGE, toTeachLanguage);
                            break;
                        case R.id.bottom_navigation_list:
                            fragment = new ChainList();
                            //whenever the user selects this,
                            //the notification icon should disappear
                            switchToNormalIcon();
                            resetNotification();
                            break;
                        case R.id.bottom_navigation_teach:
                            fragment = new Teach_Start();
                            bundle.putString(Teach_Start.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
                            break;
                    }

                    if (fragment != null) {
                        fragment.setArguments(bundle);
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.replace(R.id.fragment_container, fragment);
                        fragmentTransaction.commit();
                    }

                    return true;
                }
            }
        );

        setChatNotificationListener();

        //first page should be learn?
        Fragment fragment = new SituationList();
        Bundle bundle = new Bundle();
        bundle.putString(SituationList.BUNDLE_DISPLAY_LANGUAGE, toTeachLanguage);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar, menu);
        final MenuItem creditsMenuItem = menu.findItem(R.id.toolbar_credits);
        //listen for changes in credit
        DatabaseReference creditsRef = database.getReference(
                FirebaseDBHeaders.USER + "/" +
                        userID + "/" +
                        FirebaseDBHeaders.USER_ID_CREDITS
        );
        creditsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long credits = dataSnapshot.getValue(long.class);
                creditsMenuItem.setTitle(Long.toString(credits));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return true;
    }

    private void setLanguages(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        toLearnLanguage = preferences.getString(
                getResources().getString(R.string.preferences_to_learn_language_key), null
        );

        toTeachLanguage = preferences.getString(
                getResources().getString(R.string.preferences_to_teach_language_key), null
        );
    }

    //for the bottom navigation.
    //if the user has a new notification,
    //we change the chat icon to the same icon with a red circle
    //on the top right
    private void setChatNotificationListener(){
        DatabaseReference notificationRef = database.getReference(
            FirebaseDBHeaders.USER + "/" +
            userID + "/" +
            FirebaseDBHeaders.USER_ID_NEW_NOTIFICATION
        );

        notificationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    switchToNotificationIcon();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void switchToNotificationIcon(){
        MenuItem item = nav.getMenu().findItem(R.id.bottom_navigation_list);
        item.setIcon(R.drawable.chat_notification);
    }

    private void switchToNormalIcon(){
        MenuItem item = nav.getMenu().findItem(R.id.bottom_navigation_list);
        item.setIcon(R.drawable.chat);
    }

    private void resetNotification(){
        DatabaseReference notificationRef = database.getReference(
                FirebaseDBHeaders.USER + "/" +
                        userID + "/" +
                        FirebaseDBHeaders.USER_ID_NEW_NOTIFICATION
        );
        notificationRef.removeValue();
    }

    //from learn_start to learn_end
    @Override
    public void learnStartToLearnEnd(ChainQueue chainQueue, String situationID){
        Fragment fragment = new Learn_End();
        Bundle bundle = new Bundle();
        bundle.putString(Learn_End.BUNDLE_LANGUAGE_CODE, toLearnLanguage);
        bundle.putString(Learn_End.BUNDLE_SITUATION_ID, situationID);
        bundle.putSerializable(Learn_End.BUNDLE_CHAIN_QUEUE, chainQueue);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    //from teach_start to teach_end
    @Override
    public void teachStartToTeachEnd(ChainQueue chainQueue, String chainID){
        Fragment fragment = new Teach_End();
        Bundle bundle = new Bundle();
        bundle.putString(Teach_End.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
        bundle.putSerializable(Teach_End.BUNDLE_CHAIN_QUEUE, chainQueue);
        bundle.putString(Teach_End.BUNDLE_CHAIN_ID, chainID);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    //from situation list to learn_start
    @Override
    public void situationListToLearnStart(String situationID){
        Fragment fragment = new Learn_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Learn_Start.BUNDLE_LANGUAGE_CODE, toLearnLanguage);
        bundle.putString(Learn_Start.BUNDLE_SITUATION_ID, situationID);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    //from learn_end to situation_list
    @Override
    public void learnEndToSituationList(){
        Fragment fragment = new SituationList();
        Bundle bundle = new Bundle();
        bundle.putString(SituationList.BUNDLE_DISPLAY_LANGUAGE, toTeachLanguage);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    //from teach_end to teach_start
    @Override
    public void teachEndToTeachStart(){
        Fragment fragment = new Teach_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Teach_Start.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    //from chain list to chain info
    @Override
    public void chainListToChainInfo(MinimalChain minimalChain){
        Fragment fragment = new ChainInfo();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ChainInfo.BUNDLE_MINIMAL_CHAIN, minimalChain);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

}
