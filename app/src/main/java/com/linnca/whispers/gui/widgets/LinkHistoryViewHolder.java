package com.linnca.whispers.gui.widgets;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.linnca.whispers.data.datawrappers.LinkHistory;
import com.linnca.whispers.R;

public class LinkHistoryViewHolder extends RecyclerView.ViewHolder {
    private final TextView changeInLinksTextView;
    private final TextView transactionDateTextView;
    private final TextView transactionTimeTextView;
    private final TextView transactionTypeTextView;

    public LinkHistoryViewHolder(View view){
        super(view);
        changeInLinksTextView = (TextView)view.findViewById(R.id.link_history_change_in_links);
        transactionDateTextView = (TextView)view.findViewById(R.id.link_history_transaction_date);
        transactionTimeTextView = (TextView)view.findViewById(R.id.link_history_transaction_time);
        transactionTypeTextView = (TextView)view.findViewById(R.id.link_history_transaction_type);

    }

    void setChangeInLinks(long number){
        String changeString;
        if (number >= 0){
            changeString = "+" + Long.toString(number);
        } else {
            //negative sign already attached since it's a negative number
            changeString = Long.toString(number);
        }
        changeInLinksTextView.setText(changeString);
    }

    void setTransactionDate(String dateTimeString){
        DateTime dateTime = DateTime.parse(dateTimeString);
        DateTime todayDateTime = DateTime.now();
        int daysBetween = Days.daysBetween(
                dateTime.toLocalDate(),
                todayDateTime.toLocalDate()
        ).getDays();
        String dateString;
        if (daysBetween == 0){
            dateString = itemView.getContext().getString(
                    R.string.link_history_today
            );
        } else if (daysBetween == 1){
            dateString = itemView.getContext().getString(
                    R.string.link_history_yesterday
            );
        } else {
            DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("MM/dd");
            dateString = dateFormatter.print(dateTime);
        }

        DateTimeFormatter timeFormatter = DateTimeFormat.shortTime();
        String timeString = timeFormatter.print(dateTime);
        transactionDateTextView.setText(dateString);
        transactionTimeTextView.setText(timeString);
    }

    void setTransactionType(int transactionType){
        String transactionString;
        Context context = itemView.getContext();
        switch (transactionType){
            case LinkHistory.TRANSACTION_TYPE_PURCHASE :
                transactionString = context.getString(R.string.link_history_transaction_type_purchase);
                break;
            case LinkHistory.TRANSACTION_TYPE_LEARN:
                transactionString = context.getString(R.string.link_history_transaction_type_learn);
                break;
            case LinkHistory.TRANSACTION_TYPE_CANCEL_LEARN:
                transactionString = context.getString(R.string.link_history_transaction_type_cancel_learn);
                break;
            case LinkHistory.TRANSACTION_TYPE_SUCCESSFUL_CHAIN:
                transactionString = context.getString(R.string.link_history_transaction_type_successful_chain);
                break;
            case LinkHistory.TRANSACTION_TYPE_TEACH:
                transactionString = context.getString(R.string.link_history_transaction_type_teach);
                break;
            case LinkHistory.TRANSACTION_TYPE_LOGIN_REWARD:
                transactionString = context.getString(R.string.link_history_transaction_type_login_reward);
                break;
            default:
                return;
        }

        transactionTypeTextView.setText(transactionString);
    }
}
