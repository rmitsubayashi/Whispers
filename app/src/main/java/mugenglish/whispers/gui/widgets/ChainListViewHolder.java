package mugenglish.whispers.gui.widgets;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mugenglish.whispers.R;

class ChainListViewHolder extends RecyclerView.ViewHolder {
    private final TextView situationTextView;
    private final TextView dateLinkedTextView;
    private final LinearLayout linksLayout;

    ChainListViewHolder(View view){
        super(view);
        situationTextView = (TextView)view.findViewById(R.id.chain_list_item_situation);
        linksLayout = (LinearLayout)view.findViewById(R.id.chain_list_item_links);
        dateLinkedTextView = (TextView)view.findViewById(R.id.chain_list_item_date_linked);
    }

    void setSituationName(String name) {
        situationTextView.setText(name);
    }

    void setDateLinked(String dateTime){
        dateLinkedTextView.setText(dateTime);
    }

    void setLinks(long nextLinkNumber, long linkedLinkNumber){
        //reset all links
        linksLayout.removeAllViews();
        Context context = itemView.getContext();
        int imageDimension = GUIUtils.getDp(40,context);

        ImageView linkedLink = new ImageView(context);
        linkedLink.setImageResource(R.drawable.check_me);
        linkedLink.setLayoutParams(
                new LinearLayout.LayoutParams(0, imageDimension, 1f)
        );

        for (long i=0; i<nextLinkNumber; i++){
            if (i == linkedLinkNumber){
                linksLayout.addView(linkedLink);
                continue;
            }
            ImageView checkView = new ImageView(context);
            checkView.setImageResource(R.drawable.check);
            checkView.setLayoutParams(
                    new LinearLayout.LayoutParams(0, imageDimension, 1f)
            );
            linksLayout.addView(checkView);
        }

        for (long i=nextLinkNumber; i<5; i++){
            if (i == linkedLinkNumber){
                linksLayout.addView(linkedLink);
                continue;
            }
            LinearLayout emptyLayout = new LinearLayout(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, imageDimension, 1f);
            emptyLayout.setLayoutParams(
                params
            );
            emptyLayout.setGravity(Gravity.CENTER);
            View imageView = new View(context);
            imageView.setBackgroundResource(R.drawable.empty_circle);
            imageView.setLayoutParams(
                    new LinearLayout.LayoutParams(imageDimension, imageDimension)
            );
            emptyLayout.addView(imageView);
            linksLayout.addView(emptyLayout);
        }
    }

}
