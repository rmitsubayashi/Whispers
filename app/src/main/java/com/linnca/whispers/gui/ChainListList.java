package com.linnca.whispers.gui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ChainListAdapter;

public class ChainListList extends Fragment
{
    private final String TAG = "chainListList";
    private FirebaseDatabase database;
    private RecyclerView chainList;
    private ChainListAdapter adapter = null;
    private Snackbar snackbar;
    private ViewGroup mainLayout;
    private ViewGroup noChainsLayout;
    private ImageView arrow1;
    private ImageView arrow2;
    private Animation arrowAnimation;
    private View bottomSheet;

    private ChainListListListener listener;

    String userID;

    public interface ChainListListListener {
        void chainListToChainInfo(MinimalChain minimalChain);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = FirebaseDatabase.getInstance();
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        implementListeners(getParentFragment());
        View view = inflater.inflate(R.layout.fragment_chain_list, container, false);
        mainLayout = (ViewGroup)view.findViewById(R.id.chain_list_layout);
        noChainsLayout = (ViewGroup)view.findViewById(R.id.chain_list_no_chains_layout);
        chainList = (RecyclerView) view.findViewById(R.id.chain_list_list);
        arrow1 = (ImageView)noChainsLayout.findViewById(R.id.chain_list_no_chain_arrow1);
        arrow2 = (ImageView)noChainsLayout.findViewById(R.id.chain_list_no_chain_arrow2);
        setupArrowAnimation();
        bottomSheet = view.findViewById(R.id.chain_list_bottom_sheet);
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        populateChainList();
    }


    private void implementListeners(Fragment parentFragment){
        try {
            listener = (ChainListListListener) parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement listener");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        if (snackbar != null && snackbar.isShown()){
            snackbar.dismiss();
        }
        final int position = adapter.getPos();
        DatabaseReference chainRef = adapter.getRef(position).child(FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_CHAIN_ID);
        switch (item.getItemId()) {
            case ChainListAdapter.HIDE:
                adapter.getRef(position).child(FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_VISIBILITY)
                        .setValue(ChainManager.MINIMUM_CHAIN_VISIBILITY_HIDDEN);
                chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String chainID = dataSnapshot.getValue(String.class);
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(
                                FirebaseDBHeaders.CHAINS + "/" +
                                        chainID + "/" +
                                        FirebaseDBHeaders.CHAINS_ID_USERS
                        );
                        Query userChainQuery = userRef.orderByChild(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID)
                                .equalTo(userID);

                        userChainQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //might be more than one if the same user is on the same chain?
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                                    String userKey = snapshot.getKey();

                                    FirebaseDatabase.getInstance().getReference(
                                            FirebaseDBHeaders.CHAINS + "/" +
                                            chainID + "/" +
                                            FirebaseDBHeaders.CHAINS_ID_USERS + "/" +
                                            userKey + "/" +
                                            FirebaseDBHeaders.CHAINS_ID_USERS_VISIBILITY
                                    ).setValue(ChainManager.MINIMUM_CHAIN_VISIBILITY_HIDDEN);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                break;
            case ChainListAdapter.NO_PUSH_NOTIFICATIONS:
                //we don't need this??
                adapter.getRef(position).child(FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_NOTIFICATION_TYPE)
                        .setValue(ChainManager.NOTIFICATION_TYPE_NONE);

                chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String chainID = dataSnapshot.getValue(String.class);
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(
                                FirebaseDBHeaders.CHAINS + "/" +
                                        chainID + "/" +
                                        FirebaseDBHeaders.CHAINS_ID_USERS
                        );
                        Query userChainQuery = userRef.orderByChild(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID)
                                .equalTo(userID);

                        userChainQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //might be more than one if the same user is on the same chain?
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                                    String userKey = snapshot.getKey();

                                    FirebaseDatabase.getInstance().getReference(
                                            FirebaseDBHeaders.CHAINS + "/" +
                                                    chainID + "/" +
                                                    FirebaseDBHeaders.CHAINS_ID_USERS + "/" +
                                                    userKey + "/" +
                                                    FirebaseDBHeaders.CHAINS_ID_USERS_NOTIFICATION_TYPE
                                    ).setValue(ChainManager.NOTIFICATION_TYPE_NONE);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


                break;
            case ChainListAdapter.REMOVE:
                final DatabaseReference minimalChainVisibilityRef = adapter.getRef(position)
                        .child(FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_VISIBILITY);
                minimalChainVisibilityRef
                        .setValue(ChainManager.MINIMUM_CHAIN_VISIBILITY_REMOVED);

                chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String chainID = dataSnapshot.getValue(String.class);
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(
                                FirebaseDBHeaders.CHAINS + "/" +
                                        chainID + "/" +
                                        FirebaseDBHeaders.CHAINS_ID_USERS
                        );
                        Query userChainQuery = userRef.orderByChild(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID)
                                .equalTo(userID);

                        userChainQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //might be more than one if the same user is on the same chain?
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                                    String userKey = snapshot.getKey();
                                    DatabaseReference chainUserVisibilityRef =
                                            FirebaseDatabase.getInstance().getReference(
                                                    FirebaseDBHeaders.CHAINS + "/" +
                                                            chainID + "/" +
                                                            FirebaseDBHeaders.CHAINS_ID_USERS + "/" +
                                                            userKey + "/" +
                                                            FirebaseDBHeaders.CHAINS_ID_USERS_VISIBILITY
                                            );
                                    chainUserVisibilityRef.setValue(ChainManager.MINIMUM_CHAIN_VISIBILITY_REMOVED);

                                    //let the user undo the action if it's a mis-click
                                    showUndoSnackBar(minimalChainVisibilityRef, chainUserVisibilityRef);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        }
        return super.onContextItemSelected(item);
    }

    private void showUndoSnackBar(final DatabaseReference minimalChainVisibilityRef, final DatabaseReference chainUserVisibilityRef){
        //somehow we can't save the snack bar as a local variable
        snackbar = Snackbar.make(mainLayout, R.string.chain_list_item_remove_completed,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.chain_list_item_remove_undo,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //undo
                        minimalChainVisibilityRef.setValue(ChainManager.MINIMUM_CHAIN_VISIBILITY_VISIBLE);
                        chainUserVisibilityRef.setValue(ChainManager.MINIMUM_CHAIN_VISIBILITY_VISIBLE);
                    }
                }
        );

        chainList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (snackbar.isShown())
                    snackbar.dismiss();
            }
        });

        snackbar.show();
    }

    private void setupArrowAnimation(){
        arrowAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, -0.5f);
        arrowAnimation.setDuration(500);
        arrowAnimation.setRepeatCount(-1);
        arrowAnimation.setRepeatMode(Animation.REVERSE);
        arrowAnimation.setInterpolator(new LinearInterpolator());
    }

    private void populateChainList(){
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        //we want the new chains on top
        manager.setReverseLayout(true);
        manager.setStackFromEnd(true);
        chainList.setLayoutManager(manager);
        DatabaseReference minimalChainRef = database.getReference(
                FirebaseDBHeaders.MINIMUM_CHAINS + "/" +
                userID
        );

        Query minimalChainQuery = minimalChainRef.orderByChild(FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_VISIBILITY)
                .equalTo(ChainManager.MINIMUM_CHAIN_VISIBILITY_VISIBLE);
        ChainListAdapter.NoChainListener noChainListener = new ChainListAdapter.NoChainListener() {
            @Override
            public void onNoChain() {
                noChainsLayout.setVisibility(View.VISIBLE);

                arrow1.startAnimation(arrowAnimation);
                arrow2.startAnimation(arrowAnimation);
            }

            @Override
            public void onChain() {
                noChainsLayout.setVisibility(View.GONE);
                arrow1.clearAnimation();
                arrow2.clearAnimation();

                onFirstTime();
            }
        };
        adapter = new ChainListAdapter(minimalChainQuery, listener, userID, noChainListener);
        chainList.setAdapter(adapter);
    }

    //a quick guide if this is the user's first time
    //(just an explanation of the different check marks)
    private void onFirstTime(){
        if (isFirstTime()){
            BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(getString(R.string.preferences_chain_list_first_time_key), false);
            editor.apply();
        }
    }

    private boolean isFirstTime(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return preferences.getBoolean(
                getString(R.string.preferences_chain_list_first_time_key),true);
    }

    @Override
    public void onStop(){
        super.onStop();
        if (adapter != null)
            adapter.cleanup();
    }
}
