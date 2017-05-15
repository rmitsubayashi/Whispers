package mugenglish.whispers.gui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import mugenglish.whispers.R;
import mugenglish.whispers.gui.ChainList;
import mugenglish.whispers.gui.SituationList;
import mugenglish.whispers.gui.Teach_Start;

public class GUIUtils {
    private static int BOTTOM_NAVIGATION_BAR_STATE = 0;
    private static String BOTTOM_NAVIGATION_LEARN_TITLE = null;
    private static String BOTTOM_NAVIGATION_TEACH_TITLE = null;

    public static int getDp(int num, Context context){
        return (int)(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, num, context.getResources().getDisplayMetrics()));

    }

    public static void prepareBottomNavigationBar(Activity activity, BottomNavigationView nav) {
        final Activity fActivity = activity;

        Menu menu = nav.getMenu();
        if (BOTTOM_NAVIGATION_LEARN_TITLE == null || BOTTOM_NAVIGATION_TEACH_TITLE == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
            BOTTOM_NAVIGATION_LEARN_TITLE = preferences.getString(
                    activity.getResources().getString(R.string.preferences_to_learn_language_key), null
            );

            BOTTOM_NAVIGATION_TEACH_TITLE = preferences.getString(
                    activity.getResources().getString(R.string.preferences_to_teach_language_key), null
            );
        }

        menu.findItem(R.id.bottom_navigation_learn).setTitle(BOTTOM_NAVIGATION_LEARN_TITLE);
        menu.findItem(R.id.bottom_navigation_teach).setTitle(BOTTOM_NAVIGATION_TEACH_TITLE);

        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
        menu.getItem(BOTTOM_NAVIGATION_BAR_STATE).setChecked(true);

        nav.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Intent intent = null;
                    switch (item.getItemId()) {

                        case R.id.bottom_navigation_learn:
                            BOTTOM_NAVIGATION_BAR_STATE = 0;
                            intent = new Intent(fActivity, SituationList.class);
                            break;
                        case R.id.bottom_navigation_list:
                            BOTTOM_NAVIGATION_BAR_STATE = 1;
                            intent = new Intent(fActivity, ChainList.class);
                            break;
                        case R.id.bottom_navigation_teach:
                            BOTTOM_NAVIGATION_BAR_STATE = 2;
                            intent = new Intent(fActivity, Teach_Start.class);
                            break;
                    }

                    fActivity.startActivity(intent);
                    fActivity.finish();
                    return true;
                }
        });
    }
}
