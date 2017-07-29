package com.linnca.whispers.gui;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import com.linnca.whispers.data.ChatManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.datawrappers.ChatMessage;
import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ChainInfoChatAdapter;

public class ChainInfoChat extends Fragment {
    public static String CHAIN_INFO_CHAT_MINIMAL_CHAIN;
    private MinimalChain minimalChain;
    private FirebaseDatabase database;
    private ViewGroup layout;
    private RecyclerView list;
    private ChainInfoChatAdapter adapter = null;
    private LinearLayoutManager manager = null;
    private boolean lastIndexShown;
    private EditText chatBox;
    private Button submitMessageButton;
    private MediaPlayer player;
    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;
    private DatabaseReference userRef;
    private ValueEventListener userListener;
    private boolean connected;
    private final String SAVED_STATE_IN_QUEUE = "inQueue";
    private final String SAVED_STATE_PENDING_QUEUE_INDEX = "pendingQueueIndex";
    //TODO: retain chat message 'pending' state until the connection is restored
    private boolean inQueue = false;
    private int pendingQueueIndex = -1;

    String userID;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        Bundle bundle = getArguments();
        minimalChain = (MinimalChain)bundle.getSerializable(CHAIN_INFO_CHAT_MINIMAL_CHAIN);
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (savedInstanceState != null){
            inQueue = savedInstanceState.getBoolean(SAVED_STATE_IN_QUEUE);
            pendingQueueIndex = savedInstanceState.getInt(SAVED_STATE_PENDING_QUEUE_INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_info_chat ,container, false);
        layout = (ViewGroup)view.findViewById(R.id.chain_info_chat_layout);
        list = (RecyclerView)view.findViewById(R.id.chain_info_chat_list);
        chatBox = (EditText)view.findViewById(R.id.chain_info_chat_chat_box);
        submitMessageButton = (Button)view.findViewById(R.id.chain_info_chat_submit);

        populateList();
        addListeners();

        return view;
    }
    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_STATE_IN_QUEUE, inQueue);
        outState.putInt(SAVED_STATE_PENDING_QUEUE_INDEX, pendingQueueIndex);
    }

    private void populateList(){
        manager = new LinearLayoutManager(getContext());
        list.setLayoutManager(manager);
        //grab the list of users on the chain first so we can name the users by number
        // ie Player 1, Player 2, etc.
        //we don't store the names on the chat messages because
        // if the user joins the chain more than once, we have to check every chat message
        // and change the name
        userRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                        minimalChain.getChainID() + "/" +
                        FirebaseDBHeaders.CHAINS_ID_USERS
        );
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> userIDs = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    String userID = snapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID
                    ).getValue(String.class);
                    userIDs.add(userID);
                }
                DatabaseReference chatRef = database.getReference(
                        FirebaseDBHeaders.CHAINS + "/" +
                                minimalChain.getChainID() + "/" +
                                FirebaseDBHeaders.CHAINS_ID_CHAT_MESSAGES
                );
                if (adapter == null) {

                    adapter = new ChainInfoChatAdapter(chatRef, userID, userIDs);
                    adapter.setPendingMessagePosition(pendingQueueIndex);
                    //scroll to the bottom of the chat (so we can see newest messages)
                    adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            super.onItemRangeInserted(positionStart, itemCount);
                            int friendlyMessageCount = adapter.getItemCount();
                            int lastVisiblePosition =
                                    manager.findLastCompletelyVisibleItemPosition();
                            // If the recycler view is initially being loaded or the
                            // user is at the bottom of the list, scroll to the bottom
                            // of the list to show the newly added message.
                            if (lastVisiblePosition == -1 ||
                                    (positionStart >= (friendlyMessageCount - 1) &&
                                            lastVisiblePosition == (positionStart - 1))) {
                                list.scrollToPosition(positionStart);
                            }
                        }
                    });
                    list.setAdapter(adapter);
                } else {
                    adapter.setUserIDs(userIDs);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        userRef.addValueEventListener(userListener);

    }

    private void addListeners(){
        player = MediaPlayer.create(getContext(), R.raw.popping);
        submitMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = chatBox.getText().toString();
                //no need to send an empty message
                if (message.equals(""))
                    return;
                //clear message and play a sound so the user knows it has been sent
                chatBox.setText("");
                player.start();
                //if the user is offline, let him know
                if (!connected){
                    showNoConnectionWarning();
                    //if thi is the first message in queue
                    if (!inQueue) {
                        pendingQueueIndex = adapter.getItemCount();
                        adapter.setPendingMessagePosition(pendingQueueIndex);
                        inQueue = true;
                    }
                }
                //send the chat message
                String chainID = minimalChain.getChainID();
                ChatMessage chatMessage = new ChatMessage(message, userID);
                ChatManager.sendChatMessage(chainID, chatMessage);
            }
        });

        //let users know visually that they can't send messages when disconnected
        onlineRef = database.getReference(".info/connected");
        onlineListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                connected = dataSnapshot.getValue(Boolean.class);
                if (connected){
                    if (inQueue){
                        inQueue = false;
                        pendingQueueIndex = -1;
                        adapter.setPendingMessagePosition(pendingQueueIndex);
                        adapter.notifyDataSetChanged();
                        showQueueMessagesSent();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        onlineRef.addValueEventListener(onlineListener);
        //the chat interaction replicates what Line does.
        //if the last item is visible and the user opens the chat box,
        //scroll up so the lst item is visible.
        //if the user has scrolled somewhere and then opens the chat box,
        //keep the chat at that position and don't scroll up.
        //TODO: more elegant, "adjustPan"-like animation when the user opens the chat box while at the bottom
        //Line doesn't do this elegantly, but Facebook messenger does.
        chatBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                lastIndexShown = manager.findLastVisibleItemPosition() == adapter.getItemCount()-1;
                return false;
            }
        });
        list.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom && lastIndexShown) {

                    list.post(new Runnable() {
                        @Override
                        public void run() {
                            list.scrollToPosition(
                                    adapter.getItemCount() - 1);
                        }
                    });
                }
            }
        });
    }

    private void showQueueMessagesSent(){
        Snackbar snackbar = Snackbar.make(layout, R.string.chain_info_chat_no_connection_sent, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }
    private void showNoConnectionWarning(){
        Snackbar snackbar = Snackbar.make(layout, R.string.chain_info_chat_no_connection_submit, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    public void onDestroy(){
        if (adapter != null)
            adapter.cleanup();
        if (onlineRef != null && onlineListener != null)
            onlineRef.removeEventListener(onlineListener);
        if (userRef != null && userListener != null)
            userRef.removeEventListener(userListener);

        super.onDestroy();
    }
}
