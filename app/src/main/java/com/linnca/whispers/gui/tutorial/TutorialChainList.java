package com.linnca.whispers.gui.tutorial;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.GUIUtils;

public class TutorialChainList extends Fragment {
    private TutorialChainListListener listener;

    public interface TutorialChainListListener{
        void tutorialChainListToTutorialChainInfo();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        //can be the same as the original, but a recycler view with only one item?
        View view = inflater.inflate(R.layout.fragment_tutorial_chain_list, container, false);
        View chainListItem = inflater.inflate(R.layout.inflatable_chain_list_item, (ViewGroup)view, false);
        //populate
        TextView situationLabel = (TextView)chainListItem.findViewById(R.id.chain_list_item_situation);
        situationLabel.setText(R.string.tutorial_situation_list_situation);

        DateTime tempDateTime = DateTime.now();
        String formattedDateTime = DateTimeFormat.forPattern("MM/dd/yy").print(tempDateTime);
        TextView dateTimeLabel = (TextView)chainListItem.findViewById(R.id.chain_list_item_date_linked);
        dateTimeLabel.setText(formattedDateTime);

        //the fourth link is the user
        LinearLayout chainLinkLayout = (LinearLayout)chainListItem.findViewById(R.id.chain_list_item_links);
        Context context = getContext();
        int imageDimension = GUIUtils.getDp(40, context);
        for (int i=0; i<3; i++){
            ImageView checkImageView = new ImageView(context);
            checkImageView.setImageResource(R.drawable.check);
            checkImageView.setLayoutParams(new LinearLayout.LayoutParams(0, imageDimension, 1f));
            chainLinkLayout.addView(checkImageView);
        }

        ImageView checkMeImageView = new ImageView(context);
        checkMeImageView.setImageResource(R.drawable.check_me);
        checkMeImageView.setLayoutParams(new LinearLayout.LayoutParams(0, imageDimension, 1f));
        chainLinkLayout.addView(checkMeImageView);

        ImageView checkImageView = new ImageView(context);
        checkImageView.setImageResource(R.drawable.check);
        checkImageView.setLayoutParams(new LinearLayout.LayoutParams(0, imageDimension, 1f));
        chainLinkLayout.addView(checkImageView);

        chainListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.tutorialChainListToTutorialChainInfo();
            }
        });

        //new notification should be on
        ImageView notificationIcon = (ImageView)chainListItem.findViewById(R.id.chain_list_item_new_notification);
        notificationIcon.setVisibility(View.VISIBLE);

        ((ViewGroup) view).addView(chainListItem);


        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        implementListeners(context);
    }

    //must implement to account for lower APIs
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        implementListeners(activity);
    }

    private void implementListeners(Context context){
        try {
            listener = (TutorialChainListListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }
}
