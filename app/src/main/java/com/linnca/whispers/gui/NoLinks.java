package com.linnca.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.linnca.whispers.R;

public class NoLinks extends Fragment {
    private NoLinksListener listener;
    public interface NoLinksListener{
        void NoLinksToTeachStart();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        if (firstTime()){
            view = inflater.inflate(R.layout.fragment_no_links_first_time, container, false);
            TextView textViewWithLink = (TextView)view.findViewById(R.id.no_links_first_time_option1);
            addLink(textViewWithLink);
            //uncheckFirstTime();
        } else {
            view = inflater.inflate(R.layout.fragment_no_links, container, false);
            Button callToActionButton = (Button)view.findViewById(R.id.no_links_call_to_action);
            addCallToActionButton(callToActionButton);
        }

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
            listener = (NoLinksListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    private boolean firstTime(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        return preferences.getBoolean(
                getString(R.string.preferences_no_links_first_time_key),true);
    }

    private void uncheckFirstTime(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(getString(R.string.preferences_no_links_first_time_key), false);
        editor.apply();
    }

    private void addLink(TextView textView){
        String wholeString = getString(R.string.no_links_first_time_option1);
        String linkPart = getString(R.string.no_links_first_time_option1_link);
        int startingIndex = wholeString.indexOf(linkPart);
        int endingIndex = startingIndex + linkPart.length();
        SpannableString spannableString = new SpannableString(wholeString);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                listener.NoLinksToTeachStart();
            }

            //underline makes it more identifiable as a link
            /*@Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }*/
        };
        spannableString.setSpan(clickableSpan, startingIndex, endingIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString);
        //can't click without this??
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT);

    }

    private void addCallToActionButton(Button button){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.NoLinksToTeachStart();
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (getActivity().isFinishing()){
            uncheckFirstTime();
        }
    }
}
