package com.linnca.whispers.gui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.datawrappers.ChainQueue;
import com.linnca.whispers.R;

public class Learn_StartNoChain extends Fragment {
    private final String TAG = "learn_start_no_chain";
    public final static String LANGUAGE_TO_LEARN_BUNDLE = "toLearnBundle";
    public final static String LANGUAGE_TO_DISPLAY_BUNDLE  = "toDisplayBundle";
    private LearnStartNoChainListener listener;
    private final int maxLinkCount = 3;
    private int linkCount = 0;
    private AtomicInteger linksLoaded = new AtomicInteger(0);
    //needed for grabbing the situations
    private String languageToLearn;
    //needed for displaying the situations
    private String languageToDisplay;

    private FirebaseDatabase database;
    private DatabaseReference availableSituationRef;
    private ValueEventListener availableSituationListener;
    private List<Link> links = new ArrayList<>();
    private TextView availableSituationTextView;
    private FloatingActionButton refreshButton;
    private Animation refreshButtonAnimation;

    private class Link {
        private final ViewGroup layout;
        private final TextView textView;

        public Link(ViewGroup layout, TextView textView){
            this.layout = layout;
            this.textView = textView;
        }
    }

    public interface LearnStartNoChainListener {
        void newSituation(String situationID);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        Bundle bundle = getArguments();
        languageToDisplay = bundle.getString(LANGUAGE_TO_DISPLAY_BUNDLE);
        languageToLearn = bundle.getString(LANGUAGE_TO_LEARN_BUNDLE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_learn_start_no_chain, container, false);
        ViewGroup linkLayout1 = (ViewGroup)view.findViewById(R.id.learn_start_no_chain_link1);
        TextView linkTextView1 = (TextView)view.findViewById(R.id.learn_start_no_chain_link1_textview);
        ViewGroup linkLayout2 = (ViewGroup)view.findViewById(R.id.learn_start_no_chain_link2);
        TextView linkTextView2 = (TextView)view.findViewById(R.id.learn_start_no_chain_link2_textview);
        ViewGroup linkLayout3 = (ViewGroup)view.findViewById(R.id.learn_start_no_chain_link3);
        TextView linkTextView3 = (TextView)view.findViewById(R.id.learn_start_no_chain_link3_textview);
        links.add(new Link(linkLayout1,linkTextView1));
        links.add(new Link(linkLayout2, linkTextView2));
        links.add(new Link(linkLayout3, linkTextView3));

        availableSituationTextView = (TextView)view.findViewById(R.id.learn_start_no_chain_available_situation_textview);

        refreshButton = (FloatingActionButton)view.findViewById(R.id.learn_start_no_chain_refresh);
        //can't do this in the xml file..
        disableRefreshButton();
        refreshButtonAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        refreshButtonAnimation.setRepeatCount(Animation.INFINITE);
        refreshButton.setAnimation(refreshButtonAnimation);
        refreshButtonAnimation.start();

        implementListener(getParentFragment());

        addListeners();

        getAvailableSituations();

        return view;
    }

    private void implementListener(Fragment parentFragment){
        try {
            listener = (LearnStartNoChainListener)parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement listener");
        }
    }

    private void getAvailableSituations(){
        availableSituationRef = database.getReference(
                FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                languageToLearn
        );
        availableSituationRef.keepSynced(true);

        availableSituationListener = fetchSituationsListener();
        availableSituationRef.addListenerForSingleValueEvent(
                availableSituationListener
        );
    }

    //inefficient because we will have to check whether the chain queue is available
    // by checking a nested value.
    //but, since this page will not be called a lot +
    //the animation (as of 7/2/17) will take a while to finish
    private ValueEventListener fetchSituationsListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //we want to shuffle this so the user gets random situations every time
                List<String> situationKeys = new ArrayList<>();
                for (DataSnapshot situationSnapshot : dataSnapshot.getChildren()){
                    String situationKey = situationSnapshot.getKey();
                    //check if any of the chain queues in this is available
                    for (DataSnapshot chainQueueSnapshot : situationSnapshot.getChildren()) {
                        long inQuery = chainQueueSnapshot.child(FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE)
                                .getValue(long.class);
                        if (inQuery == ChainQueue.IN_QUEUE) {
                            situationKeys.add(situationKey);
                            break;
                        }
                    }
                }

                Collections.shuffle(situationKeys);

                linkCount = situationKeys.size() < maxLinkCount ?
                        situationKeys.size() : maxLinkCount;

                //if empty, we should notify the user
                if (linkCount == 0){
                    //wait until the refresh button stops spinning or it will flicker
                    refreshButtonAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            availableSituationTextView.setText(R.string.learn_start_no_chain_no_suggestions);
                            enableRefreshButton();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    //the lst call of populateTextview() does this,
                    //but if there are no links the method doesn't get called
                    refreshButtonAnimation.setRepeatCount(0);
                }

                for (int i=0; i<linkCount; i++){
                    String situationKey = situationKeys.get(i);
                    populateTextView(situationKey, i);
                }

                //hide any unused TextViews
                for (int i=situationKeys.size(); i<maxLinkCount; i++){
                    hideTextView(i);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void enableRefreshButton(){
        refreshButton.setEnabled(true);
        refreshButton.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(getContext(),R.color.colorAccent))
        );
    }

    private void disableRefreshButton(){
        refreshButton.setEnabled(false);
        refreshButton.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(getContext(),R.color.colorAccentDark))
        );
    }

    private void populateTextView(final String situationKey, final int index){
        if (index >= maxLinkCount){
            Log.d(TAG, "indexOutOfBoundsException in attachLinkListener()");
            return;
        }

        DatabaseReference situationNameRef = database.getReference(
                FirebaseDBHeaders.SITUATIONS + "/" +
                situationKey + "/" +
                FirebaseDBHeaders.SITUATIONS_ID_TITLE + "/" +
                languageToDisplay
        );

        situationNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String title = dataSnapshot.getValue(String.class);
                TextView linkTextView = links.get(index).textView;
                linkTextView.setText(title);

                ViewGroup linkLayout = links.get(index).layout;
                linkLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.newSituation(situationKey);
                    }
                });

                //populateTextView() will be called multiple times,
                //but we only want to enable the refresh button once the
                //last call of populateTextView() finishes
                if (linksLoaded.incrementAndGet() == linkCount){
                    refreshButtonAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            for (int i=0; i<linkCount; i++){
                                final Link link = links.get(i);
                                //show them one by one for aesthetic reasons
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        link.layout.setVisibility(View.VISIBLE);
                                    }
                                }, 400 * i);
                            }
                            enableRefreshButton();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    //makes sure spinning refresh button completes the spin
                    //before going back to default position
                    refreshButtonAnimation.setRepeatCount(0);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void hideTextView(int index){
        ViewGroup linkLayout = links.get(index).layout;
        linkLayout.setVisibility(View.GONE);
        TextView linkTextView = links.get(index).textView;
        linkTextView.setText("");
    }

    private void addListeners(){
        for (int i=0; i<links.size(); i++){
            hideTextView(i);
        }
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //reset everything
                linksLoaded.set(0);
                linkCount = 0;
                if (availableSituationListener != null && availableSituationRef != null){
                    availableSituationRef.removeEventListener(availableSituationListener);
                }
                for (Link link : links){
                    link.layout.setVisibility(View.GONE);
                }
                availableSituationTextView.setText(R.string.learn_start_no_chain_suggestion);

                //don't let the user re-click on the refresh button before it loads
                disableRefreshButton();
                refreshButton.clearAnimation();
                refreshButtonAnimation.setRepeatCount(Animation.INFINITE);
                refreshButton.setAnimation(refreshButtonAnimation);
                refreshButtonAnimation.start();

                availableSituationListener = fetchSituationsListener();
                availableSituationRef.addListenerForSingleValueEvent(
                        availableSituationListener
                );
            }
        });
    }
}
