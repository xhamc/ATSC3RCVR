/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sony.tv.app.atsc3receiver1_0;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3.CallBackInterface;
import com.sony.tv.app.atsc3receiver1_0.app.AdCategory;
import com.sony.tv.app.atsc3receiver1_0.app.Ads;
import com.sony.tv.app.atsc3receiver1_0.app.FluteReceiver;
import com.sony.tv.app.atsc3receiver1_0.app.FluteTaskManagerBase;
import com.sony.tv.app.atsc3receiver1_0.app.LLSReceiver;

import java.io.IOException;
import java.util.ArrayList;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity extends Activity {

    SampleChooserFragment sampleChooserFragment;
    LLSReceiver mLLSReceiver;
    FluteReceiver mFluteReceiver;
    private static final String TAG="MainActivity";
    private static boolean fragmentsInitialized=false;
    private static boolean sltComplete=false;
    private static boolean stComplete=false;
    private static boolean firstLLS =true;
    public static boolean ExoPlayerStarted=false;
    private static boolean first=true;
    private Ads ads;

    CallBackInterface callBackInterface;

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
        first=true;

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
        AdCategory adCategory=new AdCategory(this);
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
//            String[] dirlist=activity.getApplicationContext().getAssets().list("ADS");
//            for (int i=0; i<dirlist.length; i++) {
//                String[] adList = activity.getApplicationContext().getAssets().list("ADS/".concat(dirlist[i]));
//                for (int j=0; j<adList.length;j++) {
//
//                    if (adList[j].endsWith(".mpd")) {
//                        ads.addAd(Ads.SCHEME_ASSET.concat(":///ADS/").concat(dirlist[i]).concat("/").concat(adList[j]), true);
//
//                    }
//
//                }
//
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Ads.getAdByTitle("Better Call Saul").enabled=true;

        callBackInterface=new CallBackInterface() {
            @Override
            public void callBackSLTFound() {
                if (isFirst()) {
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
//                    if (!fragmentsInitialized)
//                        initFragments();
                    }
                }
            }

            @Override
            public void callBackSTFound() {
                if (isFirst()) {
                    if (sltComplete && firstLLS) {
                        int type;
                        if (LLSReceiver.getInstance().systemTime.getPtpPrepend() != 0) {
                            type = ATSC3.QUALCOMM;
                        } else {
                            type = ATSC3.NAB;
                        }
                        startSignalingFluteSession(type);
                        firstLLS = false;
//                    if (!fragmentsInitialized)
//                        initFragments();
                    }
                }
            }

            @Override
            public void callBackUSBDFound(FluteTaskManagerBase fluteTaskManager) {
                if (isFirst() &&
                        fluteTaskManager.isManifestFound() &&
                        fluteTaskManager.isSTSIDFound()){
//                        fluteTaskManager.stop();
                    first=false;

                    launchPlayer();
                }
            }

            @Override
            public void callBackSTSIDFound(FluteTaskManagerBase fluteTaskManager) {
                if (isFirst() &&
                        fluteTaskManager.isManifestFound() &&
                        fluteTaskManager.isUsbdFound()){
//                        fluteTaskManager.stop();
                    first=false;

                    launchPlayer();
                }
            }

            @Override
            public void callBackManifestFound(FluteTaskManagerBase fluteTaskManager) {
                if (isFirst() &&
                        fluteTaskManager.isUsbdFound() &&
                        fluteTaskManager.isSTSIDFound()){
//                        fluteTaskManager.stop();
                    first=false;
                    launchPlayer();
                }
            }

            @Override
            public void callBackFluteStopped(FluteTaskManagerBase fluteTaskManager){
//                if (isFirst() &&
//                        fluteTaskManager.isUsbdFound() &&
//                        fluteTaskManager.isSTSIDFound() &&
//                        fluteTaskManager.isManifestFound()){
//                    int type;
//                    if (LLSReceiver.getInstance().systemTime.getPtpPrepend()!=0){
//                        type=ATSC3.QUALCOMM;
//                    }else{
//                        type=ATSC3.NAB;
//                    }
//                    startCompleteFluteSession(type, fluteTaskManager);
//                }
            }
        };

        initLLSReceiver();

//        initFragments();
    }

    private boolean isFirst(){
        return first;
    }

    private void launchPlayer() {
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
                initFragments();
//                ATSC3.dataSourceIndex=0;
//                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
//                intent.setAction(PlayerActivity.ACTION_VIEW);
//                intent.setData(Uri.parse("udp://239.255.8.1:3000/ManifestUpdate_Dynamic.mpd"));
//                intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS, false);
//                intent.setComponent(new ComponentName("com.sony.tv.app.atsc3receiver1_0", "com.sony.tv.app.atsc3receiver1_0.PlayerActivity"));
//                startActivity(intent);
//
//            }
//        }, 5000);
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
    /**
     * Initialize fragments.
     * <p/>
     * Note that this also can be called when Fragments have automatically been restored by Android.
     * In this case we need to attach and configure existing Fragments instead of making new ones.
     */
    private void initFragments() {
        Log.d(TAG,"Initializing Fragment");
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // video playback fragment
        sampleChooserFragment = (SampleChooserFragment) fragmentManager.findFragmentByTag(SampleChooserFragment.TAG);
        if (sampleChooserFragment == null) {
            // create a new fragment and add it
            sampleChooserFragment = new SampleChooserFragment();
            transaction.add(R.id.videoFrame, sampleChooserFragment, SampleChooserFragment.TAG);
        }
        transaction.show(sampleChooserFragment);
        transaction.commit();

        fragmentsInitialized=true;
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

            for (int i=0; i<mLLSReceiver.slt.mSLTData.mServices.size(); i++){

                String host=mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress;
                String uriString="udp://".concat(host).concat(":").concat(
                mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort);
                Log.d(TAG,"Opening: "+uriString);
                Uri uri=Uri.parse(uriString);
                DataSpec d=new DataSpec(uri);
                mFluteReceiver.start(d, null, i, type, callBackInterface);
        }
    }


    public void startCompleteFluteSession(int type, FluteTaskManagerBase fluteTaskManager){

            int i=fluteTaskManager.index();
            String host=mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress;
            String uriString="udp://".concat(host).concat(":").concat(
                    mLLSReceiver.slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort);
            Log.d(TAG,"Opening: "+uriString);
            Uri uri=Uri.parse(uriString);
            DataSpec d=new DataSpec(uri);
            Log.d(TAG,"Opening: "+uriString);
//        DataSpec d2=new DataSpec(Uri.parse(uriString.replace("3000","3001")));
//            mFluteReceiver.start(d, d2, i, type, callBackInterface);
            mFluteReceiver.start(d, d, i, type, callBackInterface);

    }



}
