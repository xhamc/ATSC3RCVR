package com.sony.tv.app.atsc3receiver1_0.app.Ad;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.upstream.AssetDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.util.Util;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Created by xhamc on 4/11/17.
 */

public class Ads {

//    private static String title;
//    private static String Period;
//    private static String duration;
//    private static String scheme;
//    private static String parsedPeriod;
//    private static String category;

    private  DataSource dataSource;
    private  DataSource fileDataSource;
    private  DataSource assetDataSource;
    private  DataSource httpDataDataSource;
    private  XmlPullParserFactory factory;
    private  XmlPullParser xpp;

    private static final String TAG="Ads";
    public static final String SCHEME_FILE = "file";

    public static final String SCHEME_ASSET = "asset";
    public static final String SCHEME_FLUTE = "flute";
    public static final String SCHEME_HTTP  = "http";
    public final static String XLINK_HREF  ="/@xlink:href ";
    private final static int MANIFEST_BUFFER_SIZE=5000;

    private int adCount=0;
    private String scheme;


    private byte[] buffer=new byte[MANIFEST_BUFFER_SIZE];
    private ArrayList<AdContent> adArrayList=new ArrayList<>();

    public Ads(Context context){
        DataSource fileDataSource=new FileDataSource(null);
        assetDataSource=new AssetDataSource(context);
        httpDataDataSource= new DefaultHttpDataSource(ATSC3.userAgent, null, null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);
    }

    public boolean addAd(String url){
        String title="",period="",duration="",replaceStartString="",manifest="", category="";
        Uri uri=Uri.parse(url);
        if (Util.isLocalFileUri(uri)) {
            dataSource = fileDataSource;
            scheme=SCHEME_FILE;
        } else if (SCHEME_ASSET.equals(uri.getScheme())) {
            dataSource = assetDataSource;
            scheme=SCHEME_ASSET;
        } else if (SCHEME_HTTP.equals(uri.getScheme())) {
            dataSource = httpDataDataSource;
            scheme=SCHEME_HTTP;
        }else{
            Log.e(TAG,"URI scheme not recognized");
            dataSource = null;
            return false;
        }
        DataSpec dataSpec=new DataSpec(uri);

        try {
            dataSource.open(dataSpec);
            int len=dataSource.read(buffer,0,MANIFEST_BUFFER_SIZE);
            manifest=new String (buffer,0,len);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        XmlPullParserFactory factory = null;
        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(manifest);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            boolean titleTag=false;
            boolean categoryTag=false;
            loop:while (eventType!=XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {

                } else if(eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("Title")){
                        titleTag=true;
                    }else{
                        titleTag=false;
                    }
                    if (xpp.getName().equals("Category")){
                        categoryTag=true;
                    }else{
                        categoryTag=false;
                    }
                    if (xpp.getName().equals("Period")){

                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).equals("start")){
                                replaceStartString=xpp.getAttributeValue(i);
                            }
                            if (xpp.getAttributeName(i).equals("duration")){
                                duration=xpp.getAttributeValue(i);
                            }
//                            if (xpp.getAttributeName(i).equals("category")){
//                                category=xpp.getAttributeValue(i);
//                                Log.d(TAG, "Category: " + category);
//                            }

                    }
                        break loop;
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                } else if(eventType == XmlPullParser.TEXT) {
                    if (titleTag) {
                        title = xpp.getText();
                        titleTag = false;
                    }
                    if (categoryTag) {
                        category=xpp.getText();
                        categoryTag=false;
                    }
                }
                eventType = xpp.next();
            }
            period=manifest.substring(manifest.indexOf("<Period"),manifest.indexOf("</Period>")+9);
            int baseUrlStart=period.indexOf("<Period");
            baseUrlStart=period.indexOf(">",baseUrlStart+7)+1;

            String newPeriod=period.substring(0,baseUrlStart).concat("<BaseURL>").concat(uri.toString().substring(0,uri.toString().lastIndexOf("/")+1)).concat("</BaseURL>").concat(period.substring(baseUrlStart,period.length()));

//            String newPeriod=period.substring(0,baseUrlStart).concat("<BaseURL>").concat("/asset").concat(uri.getPath().substring(0,uri.getPath().lastIndexOf("/")+1)).concat("</BaseURL>").concat(period.substring(baseUrlStart,period.length()));


            if (!TextUtils.isEmpty(title)) {
                final AdContent ad = new AdContent(title,newPeriod,duration,scheme,replaceStartString,uri);
//                insertAdIntoDatabase(ad, category);
                adArrayList.add(ad);
            }
            return  true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

//    private static void insertAdIntoDatabase(final AdContent ad, final String category) {
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Realm realm = Realm.getDefaultInstance();
//                try {
//                    Log.d(TAG, "Realm created");
//                    AdContent adContent = realm.where(AdContent.class).equalTo("title", ad.title).findFirst();
//                    final String finalCategory;
//                    if (TextUtils.isEmpty(category)){
//                        finalCategory = "general";
//                    }else {
//                        finalCategory = category;
//                    }
//                    if (adContent == null){
//                        realm.beginTransaction();
//                        long adId = ATSC3.adPrimaryKey.getAndIncrement();
//                        adContent = realm.createObject(AdContent.class, adId);
//                        adContent.updateToRealm(ad);
//
//                        AdCategory adCategory = realm.where(AdCategory.class).equalTo("name", finalCategory).findFirst();
//
//                        if (adCategory == null){
//                            long catId = ATSC3.catPrimaryKey.getAndIncrement();
//                            adCategory = realm.createObject(AdCategory.class, catId);
//                            String upperCaseCategory = finalCategory.substring(0,1).toUpperCase() + finalCategory.substring(1);
//                            adCategory.setName(upperCaseCategory);
//                        }
//
//                        adCategory.getAds().add(adContent);
//                        realm.commitTransaction();
//                    }
//                }finally {
//                    realm.close();
//                }
//
//            }
//        });
//        thread.start();
//
//    }

    public ArrayList<AdContent> getAds(){
            return adArrayList;
    }
//
//    public static AdContent getAdByTitle(String title){
//
//        for (AdContent ad:adArrayList)
//        {
//            if (ad.title.equals(title)) {
//                return ad;
//            }
//        }
//        return null;
//
//    }


    public AdContent getNextAd(boolean random){
//        ArrayList<AdContent> enabledArrayAds=new ArrayList<>();
//        Realm realm = Realm.getDefaultInstance();
//        RealmResults<AdContent> adContents = realm.where(AdContent.class).findAll();
//
//        for (AdContent ad:adArrayList)
//        {
//            if (ad.enabled) {
//                enabledArrayAds.add(ad);
//            }
//        }
        if (adArrayList.size()==0) return null;
        if (random){
            if (adArrayList.size()==0) return null;
            int randomAd=(int) Math.floor(adArrayList.size()*Math.random());
            AdContent randomlySelectedAd = adArrayList.get(randomAd);
            //randomlySelectedAd.displayCount++;
            Uri savedUri = Uri.parse(randomlySelectedAd.uriString);
            Log.d(TAG, "savedUri: " + savedUri);
            randomlySelectedAd.uri = savedUri;
//            AdContent ad = realm.copyFromRealm(randomlySelectedAd);
//            realm.close();
            return randomlySelectedAd;
//            return ad ;

        }else {
            adCount++;
            if (adCount >= adArrayList.size()) {
                adCount = 0;
            }
            AdContent selectedAd = adArrayList.get(adCount);
            // selectedAd.displayCount++;
            selectedAd.uri = Uri.parse(selectedAd.uriString);
//            AdContent ad = realm.copyFromRealm(selectedAd);
//            realm.close();
            return selectedAd;
//            return ad;
        }

    }


}
