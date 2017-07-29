package com.linnca.whispers.gui.widgets;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

import java.util.List;

import com.linnca.whispers.data.datawrappers.ChatMessage;
import com.linnca.whispers.R;

public class ChainInfoChatAdapter extends FirebaseRecyclerAdapter<ChatMessage, ChainInfoChatViewHolder> {
    //to identify which layout to inflate
    private String userID;
    private List<String> userIDs;
    private int pendingMessagePosition = -1;

    public ChainInfoChatAdapter(DatabaseReference ref, String userID, List<String> userIDs){
        super(ChatMessage.class, R.layout.inflatable_chain_info_chat_item_me, ChainInfoChatViewHolder.class, ref);
        this.userID = userID;
        this.userIDs = userIDs;
    }

    @Override
    public void populateViewHolder(ChainInfoChatViewHolder holder, ChatMessage chatMessage, int position){
        holder.setMessage(chatMessage.getMessage());
        String chatUserID = chatMessage.getUserID();
        if (chatUserID.equals(userID))
            return;
        holder.setPlayerNumber(chatUserID, userIDs);
    }

    @Override
    public int getItemViewType(int position){
        if (pendingMessagePosition == -1){
            ChatMessage chatMessage = getItem(position);
            if (userID.equals(chatMessage.getUserID()))
                return R.layout.inflatable_chain_info_chat_item_me;
            else
                return R.layout.inflatable_chain_info_chat_item_other;
        }

        //this serves as a divider between online messages and offline messages
        if (position == pendingMessagePosition){
            return R.layout.inflatable_chain_info_chat_item_me_offline_first;
        }
        if (position > pendingMessagePosition){
            return R.layout.inflatable_chain_info_chat_item_me_offline;
        }
        ChatMessage chatMessage = getItem(position);
        if (userID.equals(chatMessage.getUserID()))
            return R.layout.inflatable_chain_info_chat_item_me;
        else
            return R.layout.inflatable_chain_info_chat_item_other;
    }

    public void setPendingMessagePosition(int position){
        pendingMessagePosition = position;
    }

    public void setUserIDs(List<String> userIDs){
        this.userIDs = userIDs;
    }
}
