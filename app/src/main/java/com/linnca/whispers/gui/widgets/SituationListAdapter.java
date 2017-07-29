package com.linnca.whispers.gui.widgets;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.UserManager;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.SituationListList;


public class SituationListAdapter extends RecyclerView.Adapter<SituationListViewHolder> {
    private List<SituationListItemWrapper> items;
    private final SituationListList.SituationListListListener listener;
    private final String userID;

    public SituationListAdapter(List<SituationListItemWrapper> items, SituationListList.SituationListListListener listener, String userID) {
        this.items = items;
        this.listener = listener;
        this.userID = userID;
    }

    @Override
    public int getItemViewType(int position){
        return position % 2;
    }

    @Override
    public SituationListViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView;
        if (viewType == 0) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.inflatable_situation_list_item_even, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.inflatable_situation_list_item_odd, parent, false);
        }
        return new SituationListViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(SituationListViewHolder holder, int position){
        SituationListItemWrapper item = items.get(position);
        holder.getTitleTextView().setText(item.getTitle());
        final String situationID = item.getSituationID();
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectToLearn_Start(situationID);
            }
        });
    }

    @Override
    public int getItemCount(){
        return items.size();
    }

    private void redirectToLearn_Start(final String situationID){
        //first check if the user has enough credits.
        //if he does, continue.
        DatabaseReference userCreditRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.LINKS + "/" +
                userID
        );

        userCreditRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long credits = dataSnapshot.getValue(long.class);
                if (credits >= UserManager.CREDIT_NEEDED_FOR_LEARNING) {
                    listener.situationListToLearnStart(situationID);
                }
                else {
                    listener.situationListToNoLinks();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}

