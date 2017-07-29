package com.linnca.whispers.gui.widgets;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import com.linnca.whispers.R;

public class ChainInfoChatViewHolder extends RecyclerView.ViewHolder {
    private final TextView messageTextView;
    private final TextView playerNumberTextView;
    public ChainInfoChatViewHolder(View view){
        super(view);
        messageTextView = (TextView) view.findViewById(R.id.chain_info_chat_item_message);
        playerNumberTextView = (TextView) view.findViewById(R.id.chain_info_chat_item_player_number);
    }

    public void setMessage(String message){
        messageTextView.setText(message);
    }

    public void setPlayerNumber(String chatUserID, List<String> userIDs){
        String playerNumber = "";
        for (int i=0; i<userIDs.size(); i++){
            if (chatUserID.equals(userIDs.get(i))){
                String toAdd = itemView.getContext().getString(
                        R.string.chain_info_chat_item_player_number, (i+1)
                );
                playerNumber += toAdd + ", ";
            }
        }
        if (playerNumber.length() != 0){
            //remove last comma
            playerNumber = playerNumber.substring(0, playerNumber.length()-2);
        }
        playerNumberTextView.setText(playerNumber);
    }
}
