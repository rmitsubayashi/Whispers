package com.linnca.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.linnca.whispers.R;

public class YouAreOffline extends Fragment {
    private YouAreOfflineListener listener;

    interface YouAreOfflineListener {
        void toOfflineMode();
    }

    private Button offlineModeButton;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_you_are_offline, container, false);
        offlineModeButton = (Button)view.findViewById(R.id.offline_suggestion_button);
        addActionListeners();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        implementListeners(getParentFragment());
    }

    //must implement to account for lower APIs
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        implementListeners(getParentFragment());
    }

    private void implementListeners(Fragment parentFragment){
        try {
            listener = (YouAreOfflineListener) parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement listener");
        }
    }

    private void addActionListeners(){
        offlineModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.toOfflineMode();
            }
        });
    }
}
