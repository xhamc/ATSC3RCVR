package com.sony.tv.app.atsc3receiver1_0;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSCSample;
import com.sony.tv.app.atsc3receiver1_0.app.Ad.AdCategory;
import com.sony.tv.app.atsc3receiver1_0.app.Ad.Ads;
import com.sony.tv.app.atsc3receiver1_0.app.Flute.ContentFileLocation;
import com.sony.tv.app.atsc3receiver1_0.app.Flute.FluteFileManager;
import com.sony.tv.app.atsc3receiver1_0.app.Flute.FluteReceiver;
import com.sony.tv.app.atsc3receiver1_0.app.LLS.LLSReceiver;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

public class MainPhoneActivity extends Activity {


    LLSReceiver mLLSReceiver;
    FluteReceiver mFluteReceiver;
    private static final String TAG="MainActivity";
    private static boolean fragmentsInitialized=false;
    private static boolean sltComplete=false;
    private static boolean stComplete=false;
    private static boolean firstLLS =true;
    public static boolean ExoPlayerStarted=false;
    private static boolean isReady[];
    private Ads ads;

    ATSC3.CallBackInterface callBackInterface;

    public static Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Initializing ATSC MainActivity");
        setContentView(R.layout.activity_main);
        ExoPlayerStarted=false;
        mLLSReceiver=LLSReceiver.getInstance();
        mFluteReceiver=FluteReceiver.getInstance();
        mFluteReceiver.stop();
        mLLSReceiver.stop();
        sltComplete=false;
        stComplete=false;
        ExoPlayerStarted=false;
        fragmentsInitialized=false;
        activity=this;
        //first=true;

        Intent intent=getIntent();
        if (null!=intent){
            if (null!=intent.getExtras()){
                if (intent.getExtras().containsKey("gzip")){
                    boolean value=Boolean.parseBoolean(intent.getStringExtra("gzip"));
                    ATSC3.GZIP=value;
                }
                if (intent.getExtras().containsKey("ADS")){
                    boolean value=Boolean.parseBoolean(intent.getStringExtra("ADS"));
                    ATSC3.ADS_ENABLED=value;
                }


            }
        }
        new AdCategory(this);
        try {
            String[] dirCatList=activity.getApplicationContext().getAssets().list("ADS");
            for (int i=0; i<dirCatList.length; i++) {
                AdCategory.addAdCategory(dirCatList[i]);
                String[] dirlist=activity.getApplicationContext().getAssets().list("ADS/".concat(dirCatList[i]));

                for (int j=0; j<dirlist.length; j++) {
                    String[] adList = activity.getApplicationContext().getAssets().list("ADS/".concat(dirCatList[i]).concat("/").concat(dirlist[j]));
                    for (int k = 0; k < adList.length; k++) {
                        if (adList[k].endsWith(".mpd")) {
                            AdCategory.addAd(dirCatList[i], Ads.SCHEME_ASSET.concat(":///ADS/").concat(dirCatList[i]).concat("/").concat(dirlist[j]).concat("/").concat(adList[k]));
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        callBackInterface=new ATSC3.CallBackInterface() {
            @Override
            public void callBackSLTFound() {
                sltComplete = true;
                if (stComplete && firstLLS) {
                    int type;
                    if (LLSReceiver.getInstance().systemTime.getPtpPrepend() != 0) {
                        type = ATSC3.QUALCOMM;
                    } else {
                        type = ATSC3.NAB;
                    }
                    startSignalingFluteSession(type);
                    firstLLS = false;
                }
            }

            @Override
            public void callBackSTFound() {
                stComplete=true;
                if (sltComplete && firstLLS) {
                    int type;
                    if (LLSReceiver.getInstance().systemTime.getPtpPrepend() != 0) {
                        type = ATSC3.QUALCOMM;
                    } else {
                        type = ATSC3.NAB;
                    }
                    startSignalingFluteSession(type);
                    firstLLS = false;

                }
            }

            @Override
            public void callBackUSBDFound(int index) {

            }

            @Override
            public void callBackSTSIDFound(int index) {
            }

            @Override
            public void callBackManifestFound(int index) {

            }


            @Override
            public void callBackVideoFileSize(int index, HashMap<String, ContentFileLocation> files){
                if (ExoPlayerStarted) return;
                long now= (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()).getTime() - FluteFileManager.AVAILABILITY_TIME_OFFSET - 2000;
                boolean sufficientBuffer=false;
                Iterator<Map.Entry<String, ContentFileLocation>> it=files.entrySet().iterator();

                while (it.hasNext()){
                    Map.Entry<String, ContentFileLocation> entry=it.next();
                    if (entry.getValue().time<now){
                        sufficientBuffer=true;
                    }
                }

                if (sufficientBuffer  && mFluteReceiver.mFluteTaskManager[index].isManifestFound()
                        && mFluteReceiver.mFluteTaskManager[index].isSTSIDFound()
                        && mFluteReceiver.mFluteTaskManager[index].isUsbdFound()
                        )
                {
                    isReady[index]=true;
                }
                for (int i=0; i<isReady.length; i++){
                    if (!isReady[i]){
                        return;
                    }
                }
                launchPlayer();
            }


            @Override
            public void callBackFluteStopped(int index){

            }


        };

        initLLSReceiver();

    }


    private void launchPlayer() {

        ExoPlayerStarted=true;
        ATSC3.dataSourceIndex=0;
        ATSCSample sample=ATSC3.getSampleFromIndex(ATSC3.dataSourceIndex);
        activity.startActivity(sample.buildIntent(activity));

    }

    private void launchPlayerDelayed() {
//        ((MainActivity)activity).stopLLSReceiver();
        ExoPlayerStarted=true;
        ATSC3.dataSourceIndex=0;
        ATSCSample sample=ATSC3.getSampleFromIndex(ATSC3.dataSourceIndex);
        activity.startActivity(sample.buildIntent(activity));
    }

    @Override
    public void onStop(){
        super.onStop();
        if (!ExoPlayerStarted) {
            FluteReceiver.getInstance().stop();
            LLSReceiver.getInstance().stop();
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        FluteReceiver.getInstance().stop();
        LLSReceiver.getInstance().stop();
    }

    @Override
    public void onStart(){
        super.onStart();
        mFluteReceiver.resetTimeStamp();
//        LLSReceiver.getInstance().start(this);
    }


    private void initLLSReceiver(){
        startLLSReceiver();


    }
    public void stopLLSReceiver(){
        mLLSReceiver.stop();

    }
    public void startLLSReceiver(){
        if (!mLLSReceiver.running) {
            firstLLS  = true;
            sltComplete = false;
            stComplete = false;
            mLLSReceiver.start(this, callBackInterface);
        }
    }




    public void startSignalingFluteSession(int type){
//            int i=0;
        isReady=new boolean[mLLSReceiver.slt.mSLTData.mServices.size()];
        for (int i=0; i<mLLSReceiver.slt.mSLTData.mServices.size(); i++){
            isReady[i]=false;

            String host=mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress;
            String uriString="udp://".concat(host).concat(":").concat(
                    mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort);
            Log.d(TAG,"Opening: "+uriString);
            Uri uri=Uri.parse(uriString);
            DataSpec d=new DataSpec(uri);
            mFluteReceiver.start(d, null, i, type, callBackInterface);
        }
    }


//    public void startCompleteFluteSession(int type, FluteTaskManagerBase fluteTaskManager){
//
//        int i=fluteTaskManager.index();
//        String host=mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress;
//        String uriString="udp://".concat(host).concat(":").concat(
//                mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort);
//        Log.d(TAG,"Opening: "+uriString);
//        Uri uri=Uri.parse(uriString);
//        DataSpec d=new DataSpec(uri);
//        Log.d(TAG,"Opening: "+uriString);
////        DataSpec d2=new DataSpec(Uri.parse(uriString.replace("3000","3001")));
////            mFluteReceiver.start(d, d2, i, type, callBackInterface);
//        mFluteReceiver.start(d, d, i, type, callBackInterface);
//
//    }


}