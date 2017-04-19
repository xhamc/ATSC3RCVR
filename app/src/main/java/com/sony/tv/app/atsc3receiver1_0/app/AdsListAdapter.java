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

import java.util.List;

import io.realm.Realm;

/**
 * Created by valokafor on 4/12/17.
 */

public class AdsListAdapter extends RecyclerView.Adapter<AdsListAdapter.ViewHolder>{
    private int selectedPosition = -1;
    private final List<AdCategory> adsList;
    private final Realm realm;

    public AdsListAdapter(List<AdCategory> adsList, Realm realm) {
        this.adsList = adsList;
        this.realm = realm;
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
        final AdCategory selectedCategory = adsList.get(position);
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

        if (selectedCategory != null && !TextUtils.isEmpty(selectedCategory.getName())) {
            holder.adNameTextView.setText(selectedCategory.getName());
        }

        if (selectedCategory != null && selectedCategory.getAds().size() > 0) {
            if (selectedCategory.getAds().get(0).enabled == true){
                holder.adEnableCheckbox.setChecked(true);
            } else {
                holder.adEnableCheckbox.setChecked(false);
            }
        }

        if (selectedCategory.getAds() != null && selectedCategory.getAds().size() > 0){
            String listOfAdTitle = "";
            int count = 0;

            for (AdContent selectedAd: selectedCategory.getAds()){
                if (selectedAd != null && !TextUtils.isEmpty(selectedAd.title)) {
                    String title = selectedAd.title;
                    title = title + "\n";
                    listOfAdTitle += title;
                }
                if (selectedAd != null && selectedAd.displayCount > 0) {
                    count += selectedAd.displayCount;
                }
            }
            holder.adUrlTextView.setText(listOfAdTitle);
            if (count > 0) {
                holder.adCounterTextView.setText(String.valueOf(count));
            }


        }





    }

    @Override
    public int getItemCount() {
        if (adsList != null){
            return adsList.size();
        }else {
            return 0;
        }
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
                    final AdCategory category = adsList.get(getLayoutPosition());
                    if (isChecked){
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                for (AdContent selectedAd: category.getAds())
                                    selectedAd.enabled = true;

                            }
                        });
                    }else {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                for (AdContent selectedAd: category.getAds())
                                    selectedAd.enabled = false;
                            }
                        });

                    }
                }
            });



        }
    }


}
