package com.linnca.whispers.gui.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

import com.linnca.whispers.data.datawrappers.LinkHistory;
import com.linnca.whispers.R;

public class LinkHistoryAdapter extends FirebaseRecyclerAdapter<LinkHistory, LinkHistoryViewHolder> {
    String userID;
    NoLinksListener listener;

    private final int PLUS = 0;
    private final int MINUS = 1;

    public interface NoLinksListener{
        void onNoLinks();
        void onLinks();
    }

    public LinkHistoryAdapter(String userID, DatabaseReference ref, NoLinksListener linksListener){
        super(LinkHistory.class, R.layout.inflatable_link_history_item_minus, LinkHistoryViewHolder.class, ref);
        this.userID = userID;
        this.listener = linksListener;
    }

    @Override
    public int getItemViewType(int position){
        LinkHistory history = getItem(position);
        return (history.getAmount() >= 0) ? PLUS : MINUS;
    }

    @Override
    public LinkHistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView;
        if (viewType == PLUS){
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.inflatable_link_history_item_plus, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.inflatable_link_history_item_minus, parent, false);
        }

        return new LinkHistoryViewHolder(itemView);
    }

    @Override
    public void populateViewHolder(LinkHistoryViewHolder holder, LinkHistory linkHistory, int position){
        holder.setChangeInLinks(linkHistory.getAmount());
        holder.setTransactionDate(linkHistory.getTransactionDateTime());
        holder.setTransactionType(linkHistory.getTransactionType());
    }

    //add method to clear individual history items?

    @Override
    public void onDataChanged(){
        if (getItemCount() == 0){
            listener.onNoLinks();
        } else {
            listener.onLinks();
        }
    }
}
