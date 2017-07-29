package com.linnca.whispers.gui.widgets.viewholder;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ChainListAdapter;
import com.linnca.whispers.gui.widgets.GUIUtils;

public class ChainListViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener{
    private final TextView situationTextView;
    private final ImageView newNotificationImageView;
    private final TextView dateLinkedTextView;
    private final LinearLayout linksLayout;

    public ChainListViewHolder(View view){
        super(view);
        situationTextView = (TextView)view.findViewById(R.id.chain_list_item_situation);
        newNotificationImageView = (ImageView)view.findViewById(R.id.chain_list_item_new_notification);
        linksLayout = (LinearLayout)view.findViewById(R.id.chain_list_item_links);
        dateLinkedTextView = (TextView)view.findViewById(R.id.chain_list_item_date_linked);

        view.setOnCreateContextMenuListener(this);
    }

    public void setSituationName(String name) {
        situationTextView.setText(name);
    }

    public void setDateLinked(String dateTime){
        dateLinkedTextView.setText(dateTime);
    }

    public void setLinks(long nextLinkNumber, List<Long> linkedLinkNumbers){
        //reset all links
        linksLayout.removeAllViews();
        Context context = itemView.getContext();
        int imageDimension = GUIUtils.getDp(40,context);

        for (long i=0; i<nextLinkNumber; i++){
            if (linkedLinkNumbers.contains(i)){
                ImageView linkedLink = new ImageView(context);
                linkedLink.setImageResource(R.drawable.check_me);
                linkedLink.setLayoutParams(
                        new LinearLayout.LayoutParams(0, imageDimension, 1f)
                );
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
            LinearLayout emptyLayout = new LinearLayout(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, imageDimension, 1f);
            emptyLayout.setLayoutParams(
                    params
            );
            emptyLayout.setGravity(Gravity.CENTER);
            View imageView = new View(context);
            imageView.setBackgroundResource(R.drawable.x);
            imageView.setLayoutParams(
                    new LinearLayout.LayoutParams(imageDimension, imageDimension)
            );
            emptyLayout.addView(imageView);
            linksLayout.addView(emptyLayout);
        }
    }

    public void setNewNotificationImageView(boolean active){
        if (active){
            newNotificationImageView.setVisibility(View.VISIBLE);
        } else {
            newNotificationImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info){
        //(group, id, order, text)
        menu.add(Menu.NONE, ChainListAdapter.HIDE, 0, R.string.chain_list_item_hide);
        menu.add(Menu.NONE, ChainListAdapter.NO_PUSH_NOTIFICATIONS, 1, R.string.chain_list_item_prevent_push);
        menu.add(Menu.NONE, ChainListAdapter.REMOVE, 2, R.string.chain_list_item_remove);
    }

}