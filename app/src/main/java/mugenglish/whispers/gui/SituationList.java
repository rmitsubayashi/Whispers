package mugenglish.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import data.FirebaseDBHeaders;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.SituationListAdapter;
import mugenglish.whispers.gui.widgets.SituationListItemWrapper;

public class SituationList extends Fragment {
    private String displayLanguage;
    private FirebaseDatabase database;
    private RecyclerView listView;
    private SituationListAdapter listAdapter;
    private List<SituationListItemWrapper> listItems;

    private SituationListListener listener;

    public static String BUNDLE_DISPLAY_LANGUAGE = "displayLanguage";

    public interface SituationListListener {
        void situationListToLearnStart(String situationID);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();

        Bundle bundle = getArguments();
        displayLanguage = bundle.getString(BUNDLE_DISPLAY_LANGUAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_situation_list, container, false);
        if (displayLanguage == null){
            Log.d("SituationList","Language code is empty");
            return view;
        }
        listView = (RecyclerView)view.findViewById(R.id.situation_list_list);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listItems = new ArrayList<>();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listAdapter = new SituationListAdapter(listItems, listener, userID);
        listView.setAdapter(listAdapter);
        populateList();
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
            listener = (SituationListListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    //this is expensive now.
    //flatten out/make it local later
    private void populateList(){
        DatabaseReference situationsReference = database.getReference(
                FirebaseDBHeaders.SITUATIONS
        );

        situationsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()){
                    String situationID = child.getKey();
                    //we want the list to be in the user's native language
                    String title = (String)child.child(FirebaseDBHeaders.SITUATIONS_ID_TITLE)
                            .child(displayLanguage).getValue();
                    String image = (String)child.child(FirebaseDBHeaders.SITUATIONS_ID_IMAGE)
                            .getValue();
                    SituationListItemWrapper wrapper = new SituationListItemWrapper(
                            title, situationID, image
                    );
                    listItems.add(wrapper);
                }

                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
