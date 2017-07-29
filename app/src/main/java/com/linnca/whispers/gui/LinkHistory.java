package com.linnca.whispers.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.LinkHistoryAdapter;

public class LinkHistory extends Fragment {
    String userID;
    private RecyclerView list;
    private FirebaseRecyclerAdapter adapter;
    //private FloatingActionButton addButton;
    private ViewGroup noLinksLayout;
    private ImageView arrow1;
    private ImageView arrow2;
    private Animation arrowAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_link_history);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_link_history ,container, false);
        list = (RecyclerView)view.findViewById(R.id.link_history_list);
        //addButton = (FloatingActionButton) view.findViewById(R.id.link_history_add_button);
        noLinksLayout = (ViewGroup)view.findViewById(R.id.link_history_no_history_layout);
        arrow1 = (ImageView)noLinksLayout.findViewById(R.id.chain_list_no_chain_arrow1);
        arrow2 = (ImageView)noLinksLayout.findViewById(R.id.chain_list_no_chain_arrow2);
        setupArrowAnimation();

        populateList();
        //addListeners();

        return view;
    }

    private void populateList(){
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        //we can also reverse the order by using a query on the database ref,
        //but this is computationally simpler?
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        list.setLayoutManager(layoutManager);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.LINK_HISTORY + "/" +
                        userID
        );
        LinkHistoryAdapter.NoLinksListener linksListener = new LinkHistoryAdapter.NoLinksListener() {
            @Override
            public void onNoLinks() {
                noLinksLayout.setVisibility(View.VISIBLE);

                arrow1.startAnimation(arrowAnimation);
                arrow2.startAnimation(arrowAnimation);
            }

            @Override
            public void onLinks() {
                noLinksLayout.setVisibility(View.GONE);
                arrow1.clearAnimation();
                arrow2.clearAnimation();
            }
        };
        adapter = new LinkHistoryAdapter(userID, ref, linksListener);
        list.setAdapter(adapter);
    }

    /*
    private void addListeners(){
        //show a dialog that let's the user select how much links they want to purchase.
        //after that, redirect them to google pay to purchase
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinkPurchaseDialogFragment dialogFragment = new LinkPurchaseDialogFragment();
                dialogFragment.show(getChildFragmentManager(), "");
            }
        });
    }*/

    private void setupArrowAnimation(){
        arrowAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, -0.5f);
        arrowAnimation.setDuration(500);
        arrowAnimation.setRepeatCount(-1);
        arrowAnimation.setRepeatMode(Animation.REVERSE);
        arrowAnimation.setInterpolator(new LinearInterpolator());
    }
}
