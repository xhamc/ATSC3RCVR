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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSCXmlParse;
import com.sony.tv.app.atsc3receiver1_0.app.LLSData;
import com.sony.tv.app.atsc3receiver1_0.app.LLSReceiver;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static java.lang.Thread.sleep;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    SampleChooserFragment sampleChooserFragment;
    LLSReceiver mLLSReceiver;
    private static final String TAG="MainActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initFragments();
    }

    /**
     * Initialize fragments.
     * <p/>
     * Note that this also can be called when Fragments have automatically been restored by Android.
     * In this case we need to attach and configure existing Fragments instead of making new ones.
     */
    private void initFragments() {
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

        initLLSReceiver();
    }

    private void initLLSReceiver(){
        mLLSReceiver=LLSReceiver.getInstance();
        mLLSReceiver.start();

//       SLTData s=new SLTData();
//        try {
//            Log.d(TAG,"Value of bsid before"+(s.hashMap.get("bsid")).invoke(s));
//            s.bsid=1234;
//            Log.d(TAG,"Value of bsid after"+(s.hashMap.get("bsid")).invoke(s));
//
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//
//        LLSData llsData=new LLSData();
//
//        try {
//            llsData.hashMap.get("bsid").invoke(llsData, llsData.newSLTData(), "1");
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }


    }
    public void stopLLSReceiver(){
        mLLSReceiver.stop();

    }
    public void startLLSReceiver(){
//        mLLSReceiver.start();

    }
}
