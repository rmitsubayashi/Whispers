package mugenglish.whispers.gui.widgets;

import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import data.datawrappers.MinimalChain;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.ChainList;


public class ChainListAdapter extends FirebaseRecyclerAdapter<MinimalChain, ChainListViewHolder> {
    private DateTimeFormatter formatter;
    private ChainList.ChainListListener listener;

    public ChainListAdapter(DatabaseReference ref, ChainList.ChainListListener listener){
        super(MinimalChain.class, R.layout.inflatable_chain_list_item, ChainListViewHolder.class, ref);
        formatter = DateTimeFormat.forPattern("MM/dd/yy");
        this.listener = listener;
    }

    @Override
    public void populateViewHolder(ChainListViewHolder holder, final MinimalChain minimalChain, int position){
        String situationName = minimalChain.getSituation();
        holder.setSituationName(situationName);
        String dateTimeString = minimalChain.getDateTimeLinked();
        DateTime tempDateTime = DateTime.parse(dateTimeString);
        String formattedDateTime = formatter.print(tempDateTime);
        holder.setDateLinked(formattedDateTime);
        long nextChainNumber = minimalChain.getNextLinkNumber();
        long linkedLinkNumber = minimalChain.getLinkedLinkNumber();
        holder.setLinks(nextChainNumber, linkedLinkNumber);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.chainListToChainInfo(minimalChain);
            }
        });
    }


}

