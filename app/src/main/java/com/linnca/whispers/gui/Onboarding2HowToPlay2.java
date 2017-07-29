package com.linnca.whispers.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.linnca.whispers.R;

public class Onboarding2HowToPlay2 extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding2, container, false);
        TextView titleTextView = (TextView)view.findViewById(R.id.onboarding2_title);
        titleTextView.setText(R.string.onboarding2_how_to_play2_title);

        ImageView imageView = (ImageView)view.findViewById(R.id.onboarding2_image);
        imageView.setImageResource(R.drawable.hand);

        TextView descriptionTextView = (TextView)view.findViewById(R.id.onboarding2_description);
        descriptionTextView.setText(R.string.onboarding2_how_to_play2_description);

        return view;
    }
}
