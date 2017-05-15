package mugenglish.whispers.gui;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import data.ChainManager;
import data.ChatManager;
import data.FirebaseDBHeaders;
import data.datawrappers.ChatMessage;
import data.datawrappers.MinimalChain;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ChainInfoChatAdapter;

public class ChainInfoChat extends Fragment {
    public static String CHAIN_INFO_CHAT_MINIMAL_CHAIN;
    private MinimalChain minimalChain;
    private FirebaseDatabase database;
    private RecyclerView list;
    private FirebaseRecyclerAdapter adapter = null;
    private LinearLayoutManager manager = null;
    private EditText chatBox;
    private Button submitMessageButton;
    private MediaPlayer player;

    String userID;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        Bundle bundle = getArguments();
        minimalChain = (MinimalChain)bundle.getSerializable(CHAIN_INFO_CHAT_MINIMAL_CHAIN);
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_info_chat ,container, false);
        list = (RecyclerView)view.findViewById(R.id.chain_info_chat_list);
        chatBox = (EditText)view.findViewById(R.id.chain_info_chat_chat_box);
        submitMessageButton = (Button)view.findViewById(R.id.chain_info_chat_submit);

        populateList();
        addListeners();

        return view;
    }

    private void populateList(){
        DatabaseReference ref = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                        minimalChain.getChainID() + "/" +
                        FirebaseDBHeaders.CHAINS_ID_CHAT_MESSAGES
        );
        adapter = new ChainInfoChatAdapter(ref, userID);
        manager = new LinearLayoutManager(getContext());
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
        list.setLayoutManager(manager);
        list.setAdapter(adapter);
    }

    private void addListeners(){
        player = MediaPlayer.create(ChainInfoChat.this.getContext(), R.raw.popping);
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
                //send the chat message
                String chainID = minimalChain.getChainID();
                ChatMessage chatMessage = new ChatMessage(message, userID);
                ChatManager.sendChatMessage(chainID, chatMessage);
            }
        });
    }

    @Override
    public void onDestroy(){
        if (adapter != null)
            adapter.cleanup();

        super.onDestroy();
    }
}
