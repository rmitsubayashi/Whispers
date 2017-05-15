package mugenglish.whispers.gui.widgets;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

import data.datawrappers.ChatMessage;
import mugenglish.whispers.R;

public class ChainInfoChatAdapter extends FirebaseRecyclerAdapter<ChatMessage, ChainInfoChatViewHolder> {
    //to identify which layout to inflate
    String userID;

    public ChainInfoChatAdapter(DatabaseReference ref, String userID){
        super(ChatMessage.class, R.layout.inflatable_chain_info_chat_item_me, ChainInfoChatViewHolder.class, ref);
        this.userID = userID;
    }

    @Override
    public void populateViewHolder(ChainInfoChatViewHolder holder, ChatMessage chatMessage, int position){
        holder.setMessage(chatMessage.getMessage());
    }

    @Override
    public int getItemViewType(int position){
        ChatMessage chatMessage = getItem(position);
        if (userID.equals(chatMessage.getUserID()))
            return R.layout.inflatable_chain_info_chat_item_me;
        else
            return R.layout.inflatable_chain_info_chat_item_other;
    }
}
