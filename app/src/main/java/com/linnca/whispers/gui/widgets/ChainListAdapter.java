package com.linnca.whispers.gui.widgets;

import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.ChainListList;
import com.linnca.whispers.gui.widgets.viewholder.ChainListViewHolder;


public class ChainListAdapter extends FirebaseRecyclerAdapter<MinimalChain, ChainListViewHolder> {
    private DateTimeFormatter formatter;
    private ChainListList.ChainListListListener chainListListener;
    private String userID;
    private NoChainListener noChainListener;
    private int pos = -1;
    public static final int HIDE = 0;
    public static final int NO_PUSH_NOTIFICATIONS = 1;
    public static final int REMOVE = 2;

    public interface NoChainListener{
        void onNoChain();
        void onChain();
    }

    public ChainListAdapter(Query query, ChainListList.ChainListListListener chainListListener, String userID, NoChainListener noChainListener){
        super(MinimalChain.class, R.layout.inflatable_chain_list_item, ChainListViewHolder.class, query);
        formatter = DateTimeFormat.forPattern("MM/dd/yy");
        this.chainListListener = chainListListener;
        this.noChainListener = noChainListener;
        this.userID = userID;
    }

    public int getPos(){
        return pos;
    }

    @Override
    public void populateViewHolder(ChainListViewHolder holder, final MinimalChain minimalChain, int position){
        String situationName = minimalChain.getSituation();
        holder.setSituationName(situationName);
        boolean newNotification = minimalChain.getNewNotification();
        holder.setNewNotificationImageView(newNotification);
        String dateTimeString = minimalChain.getDateTimeLinked();
        DateTime tempDateTime = DateTime.parse(dateTimeString);
        String formattedDateTime = formatter.print(tempDateTime);
        holder.setDateLinked(formattedDateTime);
        long nextChainNumber = minimalChain.getNextLinkNumber();
        List<Long> linkedLinkNumbers = minimalChain.getLinkedLinkNumbers();
        holder.setLinks(nextChainNumber, linkedLinkNumbers);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChainManager.removeMinimalChainNewNotification(minimalChain.getId(), userID);
                chainListListener.chainListToChainInfo(minimalChain);
            }
        });
    }

    @Override
    public void onBindViewHolder(final ChainListViewHolder holder, int position){
        super.onBindViewHolder(holder, position);
        final DatabaseReference ref = getRef(position);
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                pos = holder.getAdapterPosition();
                return false;
            }
        });
    }

    @Override
    public void onDataChanged(){
        if (getItemCount() == 0){
            noChainListener.onNoChain();
        } else {
            noChainListener.onChain();
        }
    }


}

