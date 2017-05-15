package mugenglish.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ActionAfterStartListener;

public class PhraseDisplay extends Fragment{
    public static String BUNDLE_PHRASE;
    private ActionAfterStartListener actionAfterStartListener;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_phrase_display, container, false);
        Bundle dataBundle = getArguments();
        String phrase = dataBundle.getString(BUNDLE_PHRASE);

        TextView phraseTextView = (TextView)view.findViewById(R.id.phrase_display_phrase);
        phraseTextView.setText(phrase);

        Button confirmButton = (Button)view.findViewById(R.id.phrase_display_confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionAfterStartListener.continueToEnd();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment parentFragment = getParentFragment();
        implementListeners(parentFragment);
    }

    //must implement to account for lower APIs
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Fragment parentFragment = getParentFragment();
        implementListeners(parentFragment);
    }

    private void implementListeners(Fragment parentFragment){
        try {
            actionAfterStartListener = (ActionAfterStartListener) parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement actionAfterStart listener");
        }
    }
}
