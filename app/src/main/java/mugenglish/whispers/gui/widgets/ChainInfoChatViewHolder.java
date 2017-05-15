package mugenglish.whispers.gui.widgets;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import mugenglish.whispers.R;

public class ChainInfoChatViewHolder extends RecyclerView.ViewHolder {
    private final TextView messageTextView;
    public ChainInfoChatViewHolder(View view){
        super(view);
        messageTextView = (TextView) view.findViewById(R.id.chain_info_chat_item_message);
    }

    public void setMessage(String message){
        messageTextView.setText(message);
    }
}
