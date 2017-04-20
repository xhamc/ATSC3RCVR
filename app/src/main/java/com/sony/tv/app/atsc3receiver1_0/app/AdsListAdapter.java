package com.sony.tv.app.atsc3receiver1_0.app;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.sony.tv.app.atsc3receiver1_0.R;
import com.sony.tv.app.atsc3receiver1_0.app.events.OnAdCategoryCheckedListener;

import java.util.List;

/**
 * Created by valokafor on 4/12/17.
 */

public class AdsListAdapter extends RecyclerView.Adapter<AdsListAdapter.ViewHolder>{

    private List<AdCategory> adsList;
    private OnAdCategoryCheckedListener listener;


    public AdsListAdapter(List<AdCategory> adsList) {
        this.adsList = adsList;
    }

    public void setListener(OnAdCategoryCheckedListener listener) {
        this.listener = listener;
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
        holder.adEnableCheckbox.setOnCheckedChangeListener(null);

        if (selectedCategory != null && !TextUtils.isEmpty(selectedCategory.getName())) {
            holder.adNameTextView.setText(selectedCategory.getName());
        }


        if (selectedCategory != null && selectedCategory.getAds().size() > 0) {
            boolean isChecked = selectedCategory.isChecked();
            holder.adEnableCheckbox.setChecked(isChecked);
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

    public void updateAdList(List<AdCategory> adCategoryList){
        this.adsList = adCategoryList;
        notifyDataSetChanged();

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
            //this.setIsRecyclable(false);

            adEnableCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = getLayoutPosition();
                    AdCategory selectedCategory = adsList.get(position);
                    if (isChecked){
                        listener.onAdCategoryChecked(selectedCategory.getId());
                    }else {
                        listener.onAdCategoryUnChecked(selectedCategory.getId());
                    }

                }
            });
        }


    }


}
