package com.linnca.whispers.gui.tutorial.widgets;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ChainInfoChatViewHolder;

public class TutorialChainInfoChatAdapter extends RecyclerView.Adapter<ChainInfoChatViewHolder> {
    private List<TutorialChainInfoChatItemWrapper> chatMessages;

    public TutorialChainInfoChatAdapter(List<TutorialChainInfoChatItemWrapper> chatMessages){
        super();
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position){
        TutorialChainInfoChatItemWrapper message = chatMessages.get(position);
        return message.isUser() ?
                R.layout.inflatable_chain_info_chat_item_me :
                R.layout.inflatable_chain_info_chat_item_other;
    }

    @Override
    public ChainInfoChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ChainInfoChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChainInfoChatViewHolder holder, int position) {
        TutorialChainInfoChatItemWrapper message = chatMessages.get(position);
        holder.setMessage(message.getMessage());

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
}
