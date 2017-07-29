package com.linnca.whispers.gui.tutorial;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import com.linnca.whispers.data.LanguageIDs;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.tutorial.widgets.TutorialChainInfoChatAdapter;
import com.linnca.whispers.gui.tutorial.widgets.TutorialChainInfoChatItemWrapper;

public class TutorialChainInfoChat extends Fragment {
    public final static String BUNDLE_LANGUAGE_CODE = "languageCode";
    private String languageCode;
    private RecyclerView list;
    private RecyclerView.Adapter adapter;
    private List<TutorialChainInfoChatItemWrapper> messages = new ArrayList<>();
    private EditText chatBox;
    private Button submitMessageButton;
    private MediaPlayer player;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_info_chat ,container, false);
        chatBox = (EditText)view.findViewById(R.id.chain_info_chat_chat_box);
        submitMessageButton = (Button)view.findViewById(R.id.chain_info_chat_submit);
        list = (RecyclerView)view.findViewById(R.id.chain_info_chat_list);
        populateList();
        addButtonListener();
        return view;
    }

    private void populateList(){
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        //now only have one initial message
        String firstMessage = "";
        switch (languageCode){
            case LanguageIDs.ENGLISH :
                firstMessage = getString(R.string.tutorial_chain_info_chat_message1_en);
                break;
            case LanguageIDs.JAPANESE :
                firstMessage = getString(R.string.tutorial_chain_info_chat_message1_jp);
                break;
        }

        messages.add(new TutorialChainInfoChatItemWrapper(firstMessage, false));
        adapter = new TutorialChainInfoChatAdapter(messages);
        list.setAdapter(adapter);

    }

    private void addButtonListener(){
        player = MediaPlayer.create(getContext(), R.raw.popping);
        submitMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = chatBox.getText().toString();
                //no need to send an empty message
                if (message.equals(""))
                    return;
                //clear message and play a sound so the user knows it has been sent
                chatBox.setText("");
                player.start();
                messages.add(new TutorialChainInfoChatItemWrapper(message, true));
                adapter.notifyDataSetChanged();
            }
        });
    }
}
