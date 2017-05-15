package mugenglish.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import data.FirebaseDBHeaders;
import data.datawrappers.MinimalChain;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ChainListAdapter;

public class ChainList extends Fragment {
    private FirebaseDatabase database;
    private RecyclerView chainList;
    private FirebaseRecyclerAdapter adapter = null;

    private ChainListListener listener;

    public interface ChainListListener {
        void chainListToChainInfo(MinimalChain minimalChain);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = FirebaseDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_list, container, false);
        chainList = (RecyclerView) view.findViewById(R.id.chain_list_list);
        chainList.setLayoutManager(new LinearLayoutManager(getContext()));
        populateChainList();

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
            listener = (ChainListListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    private void populateChainList(){
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference minimalChainRef = database.getReference(
                FirebaseDBHeaders.USER + "/" +
                userID + "/" +
                FirebaseDBHeaders.USER_ID_MINIMUM_CHAINS
        );
        adapter = new ChainListAdapter(minimalChainRef, listener);
        chainList.setAdapter(adapter);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (adapter != null)
            adapter.cleanup();
    }
}
