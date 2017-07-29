package com.linnca.whispers.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.linnca.whispers.R;

public class OnboardingIntroduction1 extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);
        TextView titleTextView = (TextView)view.findViewById(R.id.onboarding_title);
        titleTextView.setText(R.string.onboarding1_title);

        ImageView imageView = (ImageView)view.findViewById(R.id.onboarding_image);
        imageView.setImageResource(R.drawable.onboarding_chat);

        TextView descriptionTextView = (TextView)view.findViewById(R.id.onboarding_description);
        descriptionTextView.setText(R.string.onboarding1_explanation);

        return view;
    }
}
