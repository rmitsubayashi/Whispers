package com.linnca.whispers.gui.tutorial;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.linnca.whispers.R;

public class TutorialLinkHistory extends Fragment {
    private TextView learnTimeTextView;
    private TextView teachTimeTextView;
    private TextView successfulChainTimeTextView;
    private TextView learnDateTextView;
    private TextView teachDateTextView;
    private TextView successfulChainDateTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //we can make this the same layout and add a list,
        //but easier to make a linear layout and manually add items
        // (we need to change this if we change the layout for the items)
        View view = inflater.inflate(R.layout.fragment_tutorial_link_history, container, false);
        learnTimeTextView = (TextView)view.findViewById(R.id.tutorial_link_history_transaction_time_learn);
        teachTimeTextView = (TextView)view.findViewById(R.id.tutorial_link_history_transaction_time_teach);
        successfulChainTimeTextView = (TextView)view.findViewById(R.id.tutorial_link_history_transaction_time_chain_success);

        learnDateTextView = (TextView)view.findViewById(R.id.tutorial_link_history_transaction_date_learn);
        teachDateTextView = (TextView)view.findViewById(R.id.tutorial_link_history_transaction_date_teach);
        successfulChainDateTextView = (TextView)view.findViewById(R.id.tutorial_link_history_transaction_date_chain_success);

        setDateTime();

        return view;
    }

    //just set them one minute apart for simplicity
    private void setDateTime(){
        DateTime now = DateTime.now();
        DateTimeFormatter formatter = DateTimeFormat.shortTime();
        teachTimeTextView.setText(
                formatter.print(now)
        );
        //guaranteed to be today
        teachDateTextView.setText(R.string.link_history_today);
        
        DateTime oneMinuteBefore = now.minusMinutes(1);
        successfulChainTimeTextView.setText(
                formatter.print(oneMinuteBefore)
        );
        int daysBetweenOneMinute = Days.daysBetween(
                oneMinuteBefore.toLocalDate(),
                now.toLocalDate()
        ).getDays();
        String oneMinuteDateString;
        if (daysBetweenOneMinute == 0){
            oneMinuteDateString = getContext().getString(
                    R.string.link_history_today
            );
        } else {
            oneMinuteDateString = getContext().getString(
                    R.string.link_history_yesterday
            );
        }
        successfulChainDateTextView.setText(oneMinuteDateString);
        
        DateTime twoMinutesBefore = now.minusMinutes(2);
        learnTimeTextView.setText(
                formatter.print(twoMinutesBefore)
        );
        int daysBetweenTwoMinutes = Days.daysBetween(
                twoMinutesBefore.toLocalDate(),
                now.toLocalDate()
        ).getDays();
        String twoMinutesDateString;
        if (daysBetweenTwoMinutes == 0){
            twoMinutesDateString = getContext().getString(
                    R.string.link_history_today
            );
        } else {
            twoMinutesDateString = getContext().getString(
                    R.string.link_history_yesterday
            );
        }
        learnDateTextView.setText(twoMinutesDateString);
    }
}
