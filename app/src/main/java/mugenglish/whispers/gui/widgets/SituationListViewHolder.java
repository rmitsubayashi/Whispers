package mugenglish.whispers.gui.widgets;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mugenglish.whispers.R;


class SituationListViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleTextView;
    SituationListViewHolder(View view){
        super(view);
        titleTextView = (TextView)view.findViewById(R.id.inflatable_situation_list_item_title);
    }

    TextView getTitleTextView() {
        return titleTextView;
    }

    void setBoxColor(int position){
        this.itemView.setBackgroundResource(
                R.drawable.box_primary_color_border
        );
    }
}
