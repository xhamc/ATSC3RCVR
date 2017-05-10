package com.sony.tv.app.atsc3receiver1_0.app.Ad;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by valokafor on 4/17/17.
 */
//
//public class AdCategory extends RealmObject{
//    @PrimaryKey
//    private long id;
//    private String name;
//    private RealmList<AdContent> ads;
//
//    public long getId() {
//        return id;
//    }
//
//    public void setId(long id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public RealmList<AdContent> getAds() {
//        return ads;
//    }
//
//    public void setAds(RealmList<AdContent> ads) {
//        this.ads = ads;
//    }
//}

public class AdCategory {
    public static HashMap<Integer,String> adCategoryPosition=new HashMap<>();
    public static ArrayList<String> adEnabledMap=new ArrayList<>();
    public static ArrayList<String> adCategoryNames=new ArrayList<>();
    public static HashMap<String, Ads> adMap=new HashMap<>();

    private static Context context;
    private static int adCategoryEnabledCount;
    private static int adCategoryCount;

    public  AdCategory(Context context){
        this.context=context;
    }

    public static void addAdCategory(String adCategoryName){
        adCategoryPosition.put(adCategoryCount++,adCategoryName);
        adCategoryNames.add(adCategoryName);
        adMap.put(adCategoryName, new Ads(context));
        adEnabledMap.add(adCategoryName);
    }

    public static void addAd(String adCategoryName, String url){
        adMap.get(adCategoryName).addAd(url);
    }

    public static boolean isEnabled(String ad){

        for (int i=0; i<adEnabledMap.size(); i++){
            if (adEnabledMap.get(i).equals(ad)){
                return true;
            }

        }
        return false;
    }

    public static void enable(String ad){

        for (int i=0; i<adEnabledMap.size(); i++){
            if (adEnabledMap.get(i).equals(ad)){
                return;
            }
        }
        adEnabledMap.add(ad);
    }

    public static void disable(String ad){

        for (int i=0; i<adEnabledMap.size(); i++){
            if (adEnabledMap.get(i).equals(ad)){
                adEnabledMap.remove(i);
                return;
            }
        }
    }

    public static AdContent getNextAd(boolean random) {

        if (random){
            if (adEnabledMap.size()==0) return null;
            int randomAd=(int) Math.floor(adEnabledMap.size()*Math.random());
            Ads ads=adMap.get(adEnabledMap.get(randomAd));
            return ads.getNextAd(true);
        }
        else{
            if (adEnabledMap.size()==0) return null;
            adCategoryEnabledCount++;
            if (adCategoryEnabledCount >= adEnabledMap.size()) {
                adCategoryEnabledCount = 0;
            }
            Ads ads=adMap.get(adEnabledMap.get(adCategoryEnabledCount));
            return ads.getNextAd(true);

        }
    }



}
