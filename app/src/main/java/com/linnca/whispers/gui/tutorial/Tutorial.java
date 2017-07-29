package com.linnca.whispers.gui.tutorial;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.linnca.whispers.data.UserManager;
import com.onesignal.OneSignal;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.LanguageIDs;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.MainActivity;

public class Tutorial extends AppCompatActivity
implements TutorialSituationList.TutorialSituationListListener,
        TutorialLearn_Start.TutorialLearn_StartListener,
        TutorialLearn_End.TutorialLearn_EndListener,
        TutorialChainList.TutorialChainListListener,
        TutorialTeach_Start.TutorialTeach_StartListener
{
    public static final String TO_LEARN_LANGUAGE = "toLearnLanguage";
    private final String FRAGMENT_START = "start";
    private final String FRAGMENT_SITUATION_LIST = "situationList";
    private final String FRAGMENT_LEARN_START = "learnStart";
    private final String FRAGMENT_LEARN_END = "learnEnd";
    private final String FRAGMENT_AFTER_LEARN_END = "AfterLearnEnd";
    private final String FRAGMENT_CHAIN_LIST = "chainList";
    private final String FRAGMENT_CHAIN_INFO = "chainInfo";
    private final String FRAGMENT_TEACH_START = "teachStart";
    private final String FRAGMENT_LINK_HISTORY = "linkHistory";

    private List<SavedState> savedStateList = new ArrayList<>();
    private final String SAVED_STATE_ARRAY = "array";

    private final String NAVIGATION_DISABLED = "disabled";
    private final String NAVIGATION_LEARN_ENABLED = "learnEnabled";
    private final String NAVIGATION_TEACH_ENABLED = "teachEnabled";
    private final String NAVIGATION_LIST_ENABLED = "listEnabled";
    //we grab it in onCreate() but we since the toolbar is initialized later,
    //save it in a local variable
    boolean toolbarEnabled = false;
    int currentLinkAmount;

    private boolean finished = false;

    private String toLearnLanguage;
    private String toTeachLanguage;

    private Toolbar toolbar;
    private BottomNavigationView nav;
    private ProgressBar progressBar;
    private TextView progressBarTitleTextView;
    private FrameLayout container;
    private TextView linksNumberTextview;
    private MenuItem linksMenuItem;
    private FrameLayout linksMenuLayout;
    private View infoMenuLayout;
    private BottomSheetDialog bottomSheetDialog;
    private TextView bottomSheetDialogTitle;
    private TextView bottomSheetDialogDescription;

    //we want to pass on user input from learn end to chain list
    private String audioPath;
    private String answer;

    private float dim = 0.3f;

    //how many credits the user starts off with.
    //once this is depleted, the user has to teach to get more credits
    public static long defaultCredits = 1000;

    //saved state of the main activity.
    //used when the user presses the back button.
    //we have this class as static because a normal inner class would
    //have a reference to the activity (which isn't serializable)
    private static class SavedState implements Serializable {
        String navigationState;
        boolean toolbarEnabled;
        boolean mainLayoutEnabled;
        String bottomSheetTitle;
        String bottomSheetDescription;
        //not long because we don't need to store in FireBase
        int currentLinkAmount;
        boolean finished;

        SavedState(){
            super();
        }

        SavedState(SavedState copy){
            super();
            this.navigationState = copy.navigationState;
            this.toolbarEnabled = copy.toolbarEnabled;
            this.mainLayoutEnabled = copy.mainLayoutEnabled;
            this.bottomSheetTitle = copy.bottomSheetTitle;
            this.bottomSheetDescription = copy.bottomSheetDescription;
            this.currentLinkAmount = copy.currentLinkAmount;
            this.finished = copy.finished;
        }
    }

    /*
    Tutorial flow

    Let's learn ->
    One choice for situation list (something simple like "hello") ->
    Have user record something ->
    oh look! someone already connected your chain! let's check it out ->
    chain list (one item) ->
    Looks like you successfully completed a chain! Go to chat and send them a friendly congratulations! ->
    Preset chat, preset response ->
    ok, now check the log of the links you have ->
    you spent @ to learn, but since the whole chain was successfully, you got @ links! ->
    You need @ more to learn again. Go teach to get more! ->
    teach ->
    You got @ links for teaching! ->
    You're all set to go! Here's 500 links to get you started.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        if (getIntent().hasExtra(TO_LEARN_LANGUAGE)){
            toLearnLanguage = getIntent().getStringExtra(TO_LEARN_LANGUAGE);
        } else {
            //should always have a language selected
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.tutorial_toolbar);
        nav = (BottomNavigationView) findViewById(R.id.tutorial_bottom_navigation);
        progressBar = (ProgressBar) findViewById(R.id.tutorial_progress_bar);
        progressBarTitleTextView = (TextView) findViewById(R.id.tutorial_progress_bar_title);
        container = (FrameLayout) findViewById(R.id.tutorial_fragment_container);

        nav.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.bottom_navigation_learn:
                                if (!finished) {
                                    startToSituationList();
                                } else {
                                    //prevent user from registering twice
                                    disableAllNavigation(new SavedState());
                                    UserManager.registerUser(new UserManager.OnRegisterUserListener() {
                                        @Override
                                        public void onRegisterUser() {
                                            goToMainActivity();
                                        }
                                    }, toLearnLanguage, getApplicationContext());
                                }
                                return true;
                            case R.id.bottom_navigation_list:
                                afterLearnEndToChainList();
                                return true;
                            case R.id.bottom_navigation_teach:
                                tutorialChainInfoToTutorialTeachStart();
                        }

                        return true;
                    }
                }
        );

        bottomSheetDialog = new BottomSheetDialog(Tutorial.this);
        View sheetView = Tutorial.this.getLayoutInflater().inflate(R.layout.inflatable_bottom_sheet, null);
        bottomSheetDialogTitle = (TextView)sheetView.findViewById(R.id.tutorial_bottom_sheet_title);
        bottomSheetDialogDescription = (TextView)sheetView.findViewById(R.id.tutorial_bottom_sheet_text);
        bottomSheetDialog.setContentView(sheetView);

        toTeachLanguage = getToTeachLanguage();

        setSupportActionBar(toolbar);
        setTitle(R.string.tutorial_toolbar_title);
        nav.setItemIconTintList(null);

        if (savedInstanceState == null) {
            startTutorialFlow();
        } else {
            //no need to check since we know what's passed in here
            savedStateList = (List<SavedState>)savedInstanceState.getSerializable(SAVED_STATE_ARRAY);
            if (savedStateList == null) {
                startTutorialFlow();
                return;
            }

            SavedState currentState = savedStateList.get(savedStateList.size()-1);
            String navigationState = currentState.navigationState;
            switch (navigationState){
                case NAVIGATION_DISABLED :
                    disableAllNavigation(currentState);
                    break;
                case NAVIGATION_LEARN_ENABLED :
                    enableLearnNavigation(currentState);
                    break;
                case NAVIGATION_LIST_ENABLED :
                    enableChainListNavigation(currentState);
                    break;
                case NAVIGATION_TEACH_ENABLED :
                    enableTeachNavigation(currentState);
                    break;
                default:
                    disableAllNavigation(currentState);
            }

            toolbarEnabled = currentState.toolbarEnabled;
            /*have to do this in onPrepareOptionsMenu since it's not initialized yet
            if (savedToolbarEnabled){
                enableToolbar();
            } else {
                disableToolbar();
            }*/
            currentLinkAmount = currentState.currentLinkAmount;

            boolean mainLayoutEnabled = currentState.mainLayoutEnabled;
            if (mainLayoutEnabled){
                enableMainLayout(currentState);
            } else {
                disableMainLayout(currentState);
            }

            finished = currentState.finished;

            bottomSheetDialogTitle.setText(
                    currentState.bottomSheetTitle
            );

            bottomSheetDialogDescription.setText(
                    currentState.bottomSheetDescription
            );
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        linksMenuItem = menu.findItem(R.id.tutorial_toolbar_links);
        linksMenuLayout = (FrameLayout) linksMenuItem.getActionView();
        linksNumberTextview = (TextView)linksMenuLayout.findViewById(R.id.tutorial_toolbar_links_text);
        linksMenuLayout = (FrameLayout) linksMenuLayout.findViewById(R.id.tutorial_toolbar_links_layout);
        linksNumberTextview.setText(Integer.toString(currentLinkAmount));

        linksMenuLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOptionsItemSelected(linksMenuItem);
            }
        });

        final MenuItem infoMenuItem = menu.findItem(R.id.tutorial_toolbar_info);
        infoMenuLayout = infoMenuItem.getActionView();
        infoMenuLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOptionsItemSelected(infoMenuItem);
            }
        });

        //handles both initial creation and saved instance
        if (!toolbarEnabled){
            disableToolbar(new SavedState());
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tutorial_toolbar, menu);
        //linksNumberMenuItem = menu.findItem(R.id.toolbar_links_number);
        //linksNumberMenuItem.setTitle("100");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.tutorial_toolbar_links:
                tutorialTeachStartToTutorialLinks();
                return true;
            case R.id.tutorial_toolbar_info:
                //if a bottom sheet has some text, show
                if (bottomSheetDialogTitle.getText().length() != 0 &&
                        bottomSheetDialogDescription.getText().length() != 0){
                    bottomSheetDialog.show();
                }
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        //arrayList is serializable, but not list
        outState.putSerializable(SAVED_STATE_ARRAY, (ArrayList) savedStateList);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        savedStateList.remove(savedStateList.size()-1);

        if (savedStateList.size() == 0)
            return;
        //different from when we grab the state from onCreate because
        //some of the layout not initialized in onCreate is initiated here.
        SavedState currentState = savedStateList.get(savedStateList.size()-1);
        String navigationState = currentState.navigationState;
        switch (navigationState){
            case NAVIGATION_DISABLED :
                disableAllNavigation(currentState);
                break;
            case NAVIGATION_LEARN_ENABLED :
                enableLearnNavigation(currentState);
                break;
            case NAVIGATION_LIST_ENABLED :
                enableChainListNavigation(currentState);
                break;
            case NAVIGATION_TEACH_ENABLED :
                enableTeachNavigation(currentState);
                break;
            default:
                disableAllNavigation(currentState);
        }

        toolbarEnabled = currentState.toolbarEnabled;
        if (toolbarEnabled){
            enableToolbar(currentState);
        } else {
            disableToolbar(currentState);
        }
        currentLinkAmount = currentState.currentLinkAmount;
        linksNumberTextview.setText(Integer.toString(currentLinkAmount));

        boolean mainLayoutEnabled = currentState.mainLayoutEnabled;
        if (mainLayoutEnabled){
            enableMainLayout(currentState);
        } else {
            disableMainLayout(currentState);
        }

        finished = currentState.finished;

        bottomSheetDialogTitle.setText(
                currentState.bottomSheetTitle
        );

        bottomSheetDialogDescription.setText(
                currentState.bottomSheetDescription
        );
        bottomSheetDialog.show();

    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        //this may cause crash if not handled
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()){
            bottomSheetDialog.dismiss();
        }

        //we should remove the audio file only after the activity (tutorial) finishes.
        //the user may want to go back to the audio recording page at any point.
        //do not remove it on configuration change
        if (isFinishing() && audioPath != null) {
            File file = new File(audioPath);
            if (!file.delete()) {
                Log.d(getClass().getCanonicalName(), "Could not remove tutorial audio file");
            }
        }
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

    private void disableToolbar(SavedState savedState){
        toolbar.setBackgroundColor(
                ContextCompat.getColor(getApplicationContext(), R.color.dimmedColorPrimary)
        );
        linksMenuLayout.setClickable(false);
        //transparent because overlaying the same dimmed color makes it darker
        /*linksMenuLayout.setBackgroundColor(
                ContextCompat.getColor(
                        getApplicationContext(), R.color.transparent
                )
        );*/
        //never disable the info menu
        if (Build.VERSION.SDK_INT >= 21)
            infoMenuLayout.setElevation(10);

        savedState.toolbarEnabled = false;

    }

    private void enableToolbar(SavedState savedState){
        toolbar.setBackgroundColor(
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary)
        );
        linksMenuLayout.setClickable(true);
        //push the info menu back down
        if (Build.VERSION.SDK_INT >= 21)
            infoMenuLayout.setElevation(0);

        savedState.toolbarEnabled = true;
    }

    private void disableMainLayout(SavedState savedState){
        //make sure no one can touch the main layout
        container.setClickable(false);
        container.setAlpha(dim);

        savedState.mainLayoutEnabled = false;
    }

    private void enableMainLayout(SavedState savedState){
        container.setClickable(true);
        container.setAlpha(1);

        savedState.mainLayoutEnabled = true;
    }

    private void disableAllNavigation(SavedState savedState){
        int loopSize = nav.getMenu().size();
        for (int i=0; i<loopSize; i++){
            MenuItem item = nav.getMenu().getItem(i);
            item.setEnabled(false);
        }

        nav.findViewById(R.id.bottom_navigation_learn).setAlpha(dim);
        nav.findViewById(R.id.bottom_navigation_list).setAlpha(dim);
        nav.findViewById(R.id.bottom_navigation_teach).setAlpha(dim);

        savedState.navigationState = NAVIGATION_DISABLED;
    }

    private void enableLearnNavigation(SavedState savedState){
        disableAllNavigation(savedState);
        nav.getMenu().findItem(R.id.bottom_navigation_learn).setEnabled(true);
        nav.findViewById(R.id.bottom_navigation_learn).setAlpha(1);
        savedState.navigationState = NAVIGATION_LEARN_ENABLED;
    }

    private void enableTeachNavigation(SavedState savedState){
        disableAllNavigation(savedState);
        nav.getMenu().findItem(R.id.bottom_navigation_teach).setEnabled(true);
        nav.findViewById(R.id.bottom_navigation_teach).setAlpha(1);
        savedState.navigationState = NAVIGATION_TEACH_ENABLED;
    }

    private void enableChainListNavigation(SavedState savedState){
        disableAllNavigation(savedState);
        nav.getMenu().findItem(R.id.bottom_navigation_list).setEnabled(true);
        nav.findViewById(R.id.bottom_navigation_list).setAlpha(1);
        savedState.navigationState = NAVIGATION_LIST_ENABLED;
    }

    private void switchToNotificationIcon(){
        MenuItem item = nav.getMenu().findItem(R.id.bottom_navigation_list);
        item.setIcon(R.drawable.chat_notification);
    }

    private void switchToNormalIcon(){
        MenuItem item = nav.getMenu().findItem(R.id.bottom_navigation_list);
        item.setIcon(R.drawable.chat);
    }

    private void startTutorialFlow(){
        SavedState savedState = new SavedState();
        //first is having the user go to the situation list
        enableLearnNavigation(savedState);
        //have to do this in onPrepareOptionsMenu since it's not initialized yet
        //disableToolbar();
        savedState.toolbarEnabled = false;

        savedState.mainLayoutEnabled = true;
        savedState.currentLinkAmount = 100;
        savedState.finished = false;

        bottomSheetDialogTitle.setText(
                R.string.tutorial_bottom_sheet_start_title
        );
        bottomSheetDialogDescription.setText(
                R.string.tutorial_bottom_sheet_start_description
        );


        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_start_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_start_description);

        bottomSheetDialog.show();

        savedStateList.add(savedState);
    }

    private void startToSituationList(){
        //copy and overwrite any changes
        SavedState savedState = copyTopSavedState();
        //should not allow user to click on learn again
        disableAllNavigation(savedState);

        Fragment situationListFragment = new TutorialSituationList();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.addToBackStack(FRAGMENT_START);
        transaction.replace(R.id.tutorial_fragment_container, situationListFragment, FRAGMENT_SITUATION_LIST);
        transaction.commit();

        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_situation_list_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_situation_list_description);

        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_situation_list_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_situation_list_description);

        bottomSheetDialog.show();

        savedStateList.add(savedState);
    }


    @Override
    public void tutorialSituationListToTutorialLearnStart(){
        SavedState savedState = copyTopSavedState();
        //navigation all disabled already
        Fragment learnStartFragment = new TutorialLearn_Start();
        Bundle bundle = new Bundle();
        bundle.putString(TutorialLearn_Start.BUNDLE_LANGUAGE_CODE, toLearnLanguage);
        learnStartFragment.setArguments(bundle);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );
        transaction.addToBackStack(FRAGMENT_SITUATION_LIST);
        transaction.replace(R.id.tutorial_fragment_container, learnStartFragment, FRAGMENT_LEARN_START);
        transaction.commit();

        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_learn_start_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_learn_start_description);
        bottomSheetDialog.show();

        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_learn_start_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_learn_start_description);

        //remove links
        changeLinks(100, 0, savedState);

        savedStateList.add(savedState);
    }

    @Override
    public void tutorialLearnStartToTutorialLearnEnd(){
        SavedState savedState = copyTopSavedState();
        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_learn_end_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_learn_end_description);

        Fragment learnEndFragment = new TutorialLearn_End();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );
        transaction.addToBackStack(FRAGMENT_LEARN_START);
        //we save a tag so we can remove the fragment and return a blank screen afterwards
        transaction.replace(R.id.tutorial_fragment_container, learnEndFragment, FRAGMENT_LEARN_END);
        transaction.commit();

        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_learn_end_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_learn_end_description);
        bottomSheetDialog.show();

        savedStateList.add(savedState);
    }

    @Override
    public void tutorialLearnEndToStart(String audioPath, String answer){
        SavedState savedState = copyTopSavedState();
        //save as local variables
        this.audioPath = audioPath;
        this.answer = answer;

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(
                manager.findFragmentByTag(FRAGMENT_LEARN_END)
        );
        transaction.addToBackStack(FRAGMENT_LEARN_END);
        transaction.commit();

        enableChainListNavigation(savedState);
        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_after_learn_end_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_after_learn_end_description);


        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_after_learn_end_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_after_learn_end_description);
        bottomSheetDialog.show();

        switchToNotificationIcon();

        savedStateList.add(savedState);
    }

    private void afterLearnEndToChainList(){
        switchToNormalIcon();
        SavedState savedState = copyTopSavedState();
        disableAllNavigation(savedState);
        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_chain_list_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_chain_list_description);

        Fragment chainListFragment = new TutorialChainList();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );
        transaction.addToBackStack(FRAGMENT_AFTER_LEARN_END);
        transaction.replace(R.id.tutorial_fragment_container, chainListFragment);
        transaction.commit();

        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_chain_list_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_chain_list_description);
        bottomSheetDialog.show();

        changeLinks(0, 25, savedState);

        savedStateList.add(savedState);
    }

    @Override
    public void tutorialChainListToTutorialChainInfo(){
        SavedState savedState = copyTopSavedState();
        enableTeachNavigation(savedState);
        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_chain_info_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_chain_info_description);
        Fragment chainInfoFragment = new TutorialChainInfo();
        Bundle bundle = new Bundle();
        bundle.putString(TutorialChainInfo.BUNDLE_AUDIO_PATH, audioPath);
        bundle.putString(TutorialChainInfo.BUNDLE_ANSWER, answer);
        bundle.putString(TutorialChainInfo.BUNDLE_LANGUAGE_CODE, toLearnLanguage);
        chainInfoFragment.setArguments(bundle);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );
        transaction.addToBackStack(FRAGMENT_CHAIN_LIST);
        transaction.replace(R.id.tutorial_fragment_container, chainInfoFragment);
        transaction.commit();

        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_chain_info_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_chain_info_description);
        bottomSheetDialog.show();

        savedStateList.add(savedState);
    }

    private void tutorialChainInfoToTutorialTeachStart(){
        SavedState savedState = copyTopSavedState();
        disableAllNavigation(savedState);
        enableToolbar(savedState);
        disableMainLayout(savedState);
        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_teach_start_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_teach_start_description);

        Fragment teachStartFragment = new TutorialTeach_Start();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.addToBackStack(FRAGMENT_CHAIN_INFO);
        transaction.replace(R.id.tutorial_fragment_container, teachStartFragment);
        transaction.commit();

        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_teach_start_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_teach_start_description);
        bottomSheetDialog.show();

        changeLinks(25, 100, savedState);
        savedStateList.add(savedState);
    }

    public void tutorialTeachStartToTutorialLinks(){
        SavedState savedState = copyTopSavedState();
        enableLearnNavigation(savedState);
        disableToolbar(savedState);
        enableMainLayout(savedState);

        savedState.bottomSheetTitle = getString(R.string.tutorial_bottom_sheet_link_history_title);
        savedState.bottomSheetDescription = getString(R.string.tutorial_bottom_sheet_link_history_description);

        Fragment linkHistoryFragment = new TutorialLinkHistory();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.addToBackStack(FRAGMENT_TEACH_START);
        transaction.replace(R.id.tutorial_fragment_container, linkHistoryFragment, FRAGMENT_LINK_HISTORY);
        transaction.commit();

        bottomSheetDialogTitle.setText(R.string.tutorial_bottom_sheet_link_history_title);
        bottomSheetDialogDescription.setText(R.string.tutorial_bottom_sheet_link_history_description);
        bottomSheetDialog.show();

        finished = true;
        savedState.finished = true;

        savedStateList.add(savedState);
    }

    private void showLoadingUI(){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(
                manager.findFragmentByTag(FRAGMENT_LINK_HISTORY)
        );
        transaction.commit();

        progressBar.setVisibility(View.VISIBLE);
        progressBarTitleTextView.setVisibility(View.VISIBLE);

    }

    public void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void changeLinks(int from, int to, SavedState savedState){
        savedState.currentLinkAmount = to;
        currentLinkAmount = to;
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(1500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                linksNumberTextview.setText(animation.getAnimatedValue().toString());
            }
        });
        animator.start();
    }

    private SavedState copyTopSavedState(){
        return new SavedState(savedStateList.get(savedStateList.size()-1));
    }


}
