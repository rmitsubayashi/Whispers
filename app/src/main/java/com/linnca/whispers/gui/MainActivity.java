package com.linnca.whispers.gui;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.RecordingManager;
import com.linnca.whispers.data.UserManager;
import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.R;

public class MainActivity extends AppCompatActivity
implements
        SituationList.SituationListListener,
        NoLinks.NoLinksListener,
        Learn_Start.Learn_StartListener,
        Learn_End.Learn_EndListener,
        Teach_Start.Teach_StartListener,
        Teach_End.Teach_EndListener,
        ChainList.ChainListListener
{
    private final String TAG = "MainActivity";
    private String toLearnLanguage;
    private String toTeachLanguage;
    
    FragmentManager fragmentManager;

    private BottomNavigationView nav;
    private TextView linksAmountTextview;
    private long currentLinkAmount = 0;
    private MenuItem linksMenuItem;

    private String userID;
    private FirebaseDatabase database;

    private String SAVED_STATE_TOPMOST_FRAGMENT = "topmostFragment";
    private String topmostFragmentTag = "";
    private String FRAGMENT_SITUATION_LIST = "situationList";
    private String FRAGMENT_NO_LINKS = "noLinks";
    private String FRAGMENT_LEARN_START = "learnStart";
    private String FRAGMENT_LEARN_END = "learnEnd";
    private String FRAGMENT_TEACH_START = "teachStart";
    private String FRAGMENT_TEACH_END = "teachEnd";
    private String FRAGMENT_CHAIN_LIST = "chainList";
    private String FRAGMENT_CHAIN_INFO = "chainInfo";
    private String FRAGMENT_LINK_HISTORY = "linkHistory";
    private String FRAGMENT_OFFLINE_MODE = "offlineMode";

    //used when we press the back button from learn end and initialize a new instance of learn start
    private String situationID;
    private String SAVED_STATE_SITUATION_ID = "savedSituationID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setLanguages();
        fragmentManager = getSupportFragmentManager();

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        database = FirebaseDatabase.getInstance();

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        nav = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        nav.setItemIconTintList(null);
        setNavigationListener();

        setChatNotificationListener();

        
        Fragment fragment;

        //not restored (first time user opens app)
        if (savedInstanceState == null) {
            //first page should be learn?
            fragment = new SituationList();
            Bundle bundle = new Bundle();
            bundle.putString(SituationList.BUNDLE_DISPLAY_LANGUAGE, toTeachLanguage);
            bundle.putBoolean(SituationList.BUNDLE_INITIAL_RUN, true);
            fragment.setArguments(bundle);
            topmostFragmentTag = FRAGMENT_SITUATION_LIST;
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment, topmostFragmentTag);
            fragmentTransaction.commit();

            //check if the user should get a daily reward
            UserManager.OnLoginRewardListener onLoginRewardListener = new UserManager.OnLoginRewardListener() {
                @Override
                public void onLoginReward() {
                    showDailyLoginRewardDialog();
                }
            };
            UserManager.checkLastLogin(userID, onLoginRewardListener);


        } else {
            topmostFragmentTag = savedInstanceState.getString(SAVED_STATE_TOPMOST_FRAGMENT);
            situationID = savedInstanceState.getString(SAVED_STATE_SITUATION_ID);
            //prevents animations
            currentLinkAmount = -1;
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        linksMenuItem = menu.findItem(R.id.toolbar_links);
        ViewGroup menuLayout = (ViewGroup)linksMenuItem.getActionView();
        linksAmountTextview = (TextView)menuLayout.findViewById(R.id.toolbar_links_text);

        menuLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOptionsItemSelected(linksMenuItem);
            }
        });

        //listen for changes in credit
        DatabaseReference creditsRef = database.getReference(
                FirebaseDBHeaders.LINKS + "/" +
                userID
        );
        creditsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long links = dataSnapshot.getValue(long.class);
                //checking if this is a saved instance state
                if (currentLinkAmount == -1){
                    linksAmountTextview.setText(Long.toString(links));
                } else {
                    changeLinks(currentLinkAmount, links);
                }
                currentLinkAmount = links;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.toolbar_links :
                toLinkHistory();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putString(SAVED_STATE_TOPMOST_FRAGMENT, topmostFragmentTag);
        outState.putString(SAVED_STATE_SITUATION_ID, situationID);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed(){
        //also can do
        //String fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();

        //when the user presses the back from learn start or learn end,
        Fragment learnEndFragment = fragmentManager.findFragmentByTag(FRAGMENT_LEARN_END);
        if (learnEndFragment != null && learnEndFragment.isVisible()){
            if (situationID == null){
                super.onBackPressed();
            } else {
                promptBackFromLearnEnd();
            }
            return;
        }

        Fragment teachEndFragment = fragmentManager.findFragmentByTag(FRAGMENT_TEACH_END);
        if (teachEndFragment != null && teachEndFragment.isVisible()){
            teachEndToTeachStart(false);
            return;
        }

        Fragment offlineFragment = fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE_MODE);
        if (offlineFragment != null && offlineFragment.isVisible()){
            if (((OfflineMode)offlineFragment).shouldHandleBackPress()) {
                ((OfflineMode) offlineFragment).handleBackPress();
                return;
            }
        }


        super.onBackPressed();
    }

    private void promptBackFromLearnEnd(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.learn_end_go_back);
        alertDialogBuilder.setPositiveButton(R.string.learn_end_go_back_continue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                learnEndToLearnStart(situationID);
            }
        });
        alertDialogBuilder.setNegativeButton(R.string.link_purchase_cancel, null);
        alertDialogBuilder.show();
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

    private void setNavigationListener(){
        nav.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Fragment fragment = null;
                        String currentFragmentTag = "";
                        Bundle bundle = new Bundle();
                        switch (item.getItemId()) {
                            case R.id.bottom_navigation_learn:
                                fragment = new SituationList();
                                bundle.putString(SituationList.BUNDLE_DISPLAY_LANGUAGE, toTeachLanguage);
                                currentFragmentTag = FRAGMENT_SITUATION_LIST;

                                break;
                            case R.id.bottom_navigation_list:
                                fragment = new ChainList();
                                currentFragmentTag = FRAGMENT_CHAIN_LIST;

                                //whenever the user selects this,
                                //the notification icon(if there is a new notification)
                                //should disappear
                                switchToNormalIcon();
                                resetNotification();
                                break;
                            case R.id.bottom_navigation_teach:
                                fragment = new Teach_Start();
                                bundle.putString(Teach_Start.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
                                currentFragmentTag = FRAGMENT_TEACH_START;
                                break;
                        }

                        FragmentManager fragmentManager = getSupportFragmentManager();
                        clearBackStack(fragmentManager);
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                        if (fragment != null) {
                            fragment.setArguments(bundle);
                            fragmentTransaction.replace(R.id.fragment_container, fragment, currentFragmentTag);
                            topmostFragmentTag = currentFragmentTag;
                        }
                        fragmentTransaction.commit();

                        return true;
                    }
                }
        );
    }

    //for the bottom navigation.
    //if the user has a new notification,
    //we change the chat icon to the same icon with a red circle
    //on the top right
    private void setChatNotificationListener(){
        DatabaseReference notificationRef = database.getReference(
            FirebaseDBHeaders.NEW_NOTIFICATION + "/" +
            userID
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
                FirebaseDBHeaders.NEW_NOTIFICATION + "/" +
                userID
        );
        notificationRef.removeValue();
    }

    //longs because FireBase stores long values
    private void changeLinks(long from, long to){
        ValueAnimator animator = ValueAnimator.ofInt((int)from, (int)to);
        animator.setDuration(1500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                linksAmountTextview.setText(animation.getAnimatedValue().toString());
            }
        });
        animator.start();
    }

    //all the interfaces that control the flow of the fragments
    //from learn_start to learn_end
    @Override
    public void learnStartToLearnEnd(String chainQueueKey, String situationID, String chainID){
        Fragment fragment = new Learn_End();
        Bundle bundle = new Bundle();
        bundle.putString(Learn_End.BUNDLE_LANGUAGE_CODE, toLearnLanguage);
        bundle.putString(Learn_End.BUNDLE_SITUATION_ID, situationID);
        bundle.putString(Learn_End.BUNDLE_CHAIN_QUEUE_KEY, chainQueueKey);
        bundle.putString(Learn_End.BUNDLE_CHAIN_ID, chainID);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );
        fragmentTransaction.addToBackStack(FRAGMENT_LEARN_START);
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_LEARN_END);
        fragmentTransaction.commit();
    }

    public void learnEndToLearnStart(String situationID){
        if (situationID == null){
            return;
        }
        fragmentManager.popBackStack();
        fragmentManager.popBackStack();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(FRAGMENT_SITUATION_LIST);
        Fragment fragment = new Learn_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Learn_Start.BUNDLE_TO_LEARN_LANGUAGE_CODE, toLearnLanguage);
        bundle.putString(Learn_Start.BUNDLE_TO_DISPLAY_LANGUAGE_CODE, toTeachLanguage);
        bundle.putString(Learn_Start.BUNDLE_SITUATION_ID, situationID);
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_LEARN_START);
        fragmentTransaction.commit();
    }

    @Override
    public void learnStartToSituationList(){
        Fragment fragment = new SituationList();
        Bundle bundle = new Bundle();
        bundle.putString(SituationList.BUNDLE_DISPLAY_LANGUAGE, toTeachLanguage);
        fragment.setArguments(bundle);
        clearBackStack(fragmentManager);
        fragmentManager.executePendingTransactions();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_SITUATION_LIST);
        fragmentTransaction.commit();
    }

    //when the user can't find a chain
    //and we want to redirect the user to another chain
    @Override
    public void learnStartToLearnStart(String situationID){
        Fragment fragment = new Learn_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Learn_Start.BUNDLE_TO_LEARN_LANGUAGE_CODE, toLearnLanguage);
        bundle.putString(Learn_Start.BUNDLE_TO_DISPLAY_LANGUAGE_CODE, toTeachLanguage);
        bundle.putString(Learn_Start.BUNDLE_SITUATION_ID, situationID);
        fragment.setArguments(bundle);
        fragmentManager.popBackStack();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(FRAGMENT_SITUATION_LIST);
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_LEARN_START);
        fragmentTransaction.commit();
    }

    //save for when the user presses the back button from learn end
    @Override
    public void setSituationID(String situationID){
        this.situationID = situationID;
    }

    @Override
    public void learnEndSaveData(final String fileName, final String answer, final String chainID, String chainQueueKey, String situationID){
        if (fileName != null){
            RecordingManager.OnSaveRecordingListener listener = new RecordingManager.OnSaveRecordingListener() {
                @Override
                public void onSaveRecording() {
                    RecordingManager.removeRecordingFromInternalStorage(getApplicationContext(), fileName);
                }
            };
            RecordingManager.uploadFromInternalStorage(getApplicationContext(), fileName, listener);
        }

        final DatabaseReference chainQueueRef = database.getReference(
                FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                        toLearnLanguage + "/" +
                        situationID + "/" +
                        chainQueueKey
        );
        ChainManager.updateChain(chainID, chainQueueRef, fileName, answer);
        UserManager.removeCredit(userID, UserManager.CREDIT_NEEDED_FOR_LEARNING, com.linnca.whispers.data.datawrappers.LinkHistory.TRANSACTION_TYPE_LEARN);
    }

    //from teach_start to teach_end
    @Override
    public void teachStartToTeachEnd(String chainQueueKey, String chainID, String phrase){
        Fragment fragment = new Teach_End();
        Bundle bundle = new Bundle();
        bundle.putString(Teach_End.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
        bundle.putString(Teach_End.BUNDLE_CHAIN_QUEUE_KEY, chainQueueKey);
        bundle.putString(Teach_End.BUNDLE_CHAIN_ID, chainID);
        if (phrase != null)
            bundle.putString(Teach_End.BUNDLE_PHRASE, phrase);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );
        fragmentTransaction.addToBackStack(FRAGMENT_TEACH_START);
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_TEACH_END);
        fragmentTransaction.commit();
    }

    //from situation list to learn_start
    @Override
    public void situationListToLearnStart(String situationID){
        Fragment fragment = new Learn_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Learn_Start.BUNDLE_TO_LEARN_LANGUAGE_CODE, toLearnLanguage);
        bundle.putString(Learn_Start.BUNDLE_TO_DISPLAY_LANGUAGE_CODE, toTeachLanguage);
        bundle.putString(Learn_Start.BUNDLE_SITUATION_ID, situationID);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
                );
        fragmentTransaction.addToBackStack(FRAGMENT_SITUATION_LIST);
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_LEARN_START);
        fragmentTransaction.commit();
    }
    @Override
    public void situationListToNoLinks(){
        Fragment fragment = new NoLinks();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(FRAGMENT_SITUATION_LIST);
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_NO_LINKS);
        fragmentTransaction.commit();
    }

    @Override
    public void NoLinksToTeachStart(){
        Fragment fragment = new Teach_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Teach_Start.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        clearBackStack(fragmentManager);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_TEACH_START);
        fragmentTransaction.commit();
        topmostFragmentTag = FRAGMENT_TEACH_START;
        //since we are switching hierarchies we need to set the navigation manually
        nav.setSelectedItemId(R.id.bottom_navigation_teach);
    }

    //from learn_end to situation_list
    @Override
    public void learnEndToSituationList(boolean completed){
        Fragment fragment = new SituationList();
        Bundle bundle = new Bundle();
        bundle.putString(SituationList.BUNDLE_DISPLAY_LANGUAGE, toTeachLanguage);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        clearBackStack(fragmentManager);
        fragmentManager.executePendingTransactions();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_SITUATION_LIST);
        fragmentTransaction.commit();

        if (completed){
            showCompleteToast();
        }
    }

    //from teach_end to teach_start
    @Override
    public void teachEndToTeachStart(boolean completed){
        Fragment fragment = new Teach_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Teach_Start.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        clearBackStack(fragmentManager);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_TEACH_START);
        fragmentTransaction.commit();

        if (completed){
            showCompleteToast();
        }
    }

    //the fragment closes before the FireBase transaction finishes, so save the com.linnca.whispers.data in the main activity
    @Override
    public void teachEndSaveData(final String fileName, final String answer, final String chainID, final String chainQueueKey){
        //can be null if it's the final answer
        if (fileName != null){
            RecordingManager.OnSaveRecordingListener listener = new RecordingManager.OnSaveRecordingListener() {
                @Override
                public void onSaveRecording() {
                    RecordingManager.removeRecordingFromInternalStorage(getApplicationContext(), fileName);
                }
            };
            RecordingManager.uploadFromInternalStorage(getApplicationContext(), fileName, listener);
        }
        //is null when we create a new chain
        if (chainQueueKey == null){
            //increment here so we know the user completed the session and have added a chain
            ChainManager.incrementSituationToCreateChainCt(chainID);
        }

        DatabaseReference chainQueueRef = null;
        if (chainQueueKey != null){
            chainQueueRef= database.getReference(
                    FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                            toTeachLanguage + "/" +
                            chainQueueKey
            );
        }
        ChainManager.updateChain(chainID, chainQueueRef, fileName, answer);

        UserManager.addCredit(userID, UserManager.CREDIT_REWARD_FOR_TEACHING, com.linnca.whispers.data.datawrappers.LinkHistory.TRANSACTION_TYPE_TEACH);
    }

    //from teach_start(the user stopped so he has to go back) to teach_start
    @Override
    public void teachStartToTeachStart(){
        Fragment fragment = new Teach_Start();
        Bundle bundle = new Bundle();
        bundle.putString(Teach_Start.BUNDLE_LANGUAGE_CODE, toTeachLanguage);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        clearBackStack(fragmentManager);
        fragmentManager.executePendingTransactions();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_TEACH_START);
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
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );
        fragmentTransaction.addToBackStack(FRAGMENT_CHAIN_LIST);
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_CHAIN_INFO);
        fragmentTransaction.commit();
    }

    public void toLinkHistory(){
        //don't do anything if the user is already in link history
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(FRAGMENT_LINK_HISTORY) != null)
            return;

        Fragment fragment = new LinkHistory();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.stay,
                0, R.anim.slide_out_bottom
        );
        //we can call this from any fragment,
        //so we can't hard-code the previous fragment to add on to the back stack.
        //so get it programmatically
        if (fragmentManager.getBackStackEntryCount() != 0) {
            String fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();
            fragmentTransaction.addToBackStack(fragmentTag);
        } else {
            fragmentTransaction.addToBackStack(topmostFragmentTag);
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_LINK_HISTORY);
        fragmentTransaction.commit();
    }

    @Override
    public void toOfflineMode(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = new OfflineMode();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.stay,
                0, R.anim.slide_out_bottom
        );
        //we can call this from most fragments,
        //so we can't hard-code the previous fragment to add on to the back stack.
        //so get it programmatically
        if (fragmentManager.getBackStackEntryCount() != 0) {
            String fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();
            fragmentTransaction.addToBackStack(fragmentTag);
        } else {
            fragmentTransaction.addToBackStack(topmostFragmentTag);
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_OFFLINE_MODE);
        fragmentTransaction.commit();
    }

    private void clearBackStack(FragmentManager manager){
        manager.popBackStack(topmostFragmentTag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    //for when we complete a chain
    private void showCompleteToast(){
        Toast.makeText(this, getString(R.string.main_activity_completed), Toast.LENGTH_SHORT).show();
    }

    private void showDailyLoginRewardDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.main_activity_login_reward_title);
        alertDialogBuilder.setMessage(R.string.main_activity_login_reward_message);
        alertDialogBuilder.setPositiveButton(R.string.main_activity_login_confirm, null);
        alertDialogBuilder.show();
    }

}
