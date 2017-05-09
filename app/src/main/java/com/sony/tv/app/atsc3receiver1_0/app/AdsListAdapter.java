package com.sony.tv.app.atsc3receiver1_0.app;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.sony.tv.app.atsc3receiver1_0.R;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

/**
 * Created by valokafor on 4/12/17.
 */

public class AdsListAdapter extends RecyclerView.Adapter<AdsListAdapter.ViewHolder>{
    private int selectedPosition = -1;

    public AdsListAdapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.custom_row_layout_ads, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final String selectedCategory = AdCategory.adCategoryPosition.get(position);

        if (selectedPosition == position){
            holder.itemView.setBackgroundColor(Color.parseColor("#000000"));
        }else {
            holder.itemView.setBackgroundColor(Color.parseColor("#ffffffff"));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedPosition = position;
                notifyDataSetChanged();
            }
        });


        if (selectedCategory != null ) {
            holder.adNameTextView.setText(selectedCategory);
        }
        if (selectedCategory != null ) {
            if (AdCategory.isEnabled(selectedCategory)){
                holder.adEnableCheckbox.setChecked(true);
            } else {
                holder.adEnableCheckbox.setChecked(false);
            }
        }


        if (selectedCategory!= null){

            String listOfAdTitle = "";
            ArrayList<AdContent> ads= AdCategory.adMap.get(selectedCategory).getAds();
            for (AdContent ad:ads){
                String title=ad.title.concat ("\n");
                listOfAdTitle+=title;
            }

            holder.adUrlTextView.setText(listOfAdTitle);

        }

    }

    @Override
    public int getItemCount() {

        return AdCategory.adCategoryNames.size();

    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView adCounterTextView;
        public TextView adNameTextView;
        public TextView adUrlTextView;
        public CheckBox adEnableCheckbox;


        public ViewHolder(View itemView) {
            super(itemView);
            adCounterTextView = (TextView) itemView.findViewById(R.id.display_count_textview);
            adNameTextView = (TextView) itemView.findViewById(R.id.ad_name_textview);
            adUrlTextView = (TextView) itemView.findViewById(R.id.ad_url_textview);
            adEnableCheckbox = (CheckBox) itemView.findViewById(R.id.ad_enable_checkbox);
            adEnableCheckbox.setEnabled(true);
            adEnableCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final String category = AdCategory.adCategoryPosition.get(getLayoutPosition());
                    if (isChecked){
                        AdCategory.enable(category);
                    }else{
                        AdCategory.disable(category);

                    }

                }
            });



        }
    }


}
