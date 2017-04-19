/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sony.tv.app.atsc3receiver1_0;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.sony.tv.app.atsc3receiver1_0.SampleListLoader.SampleGroup;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSCSample;
import com.sony.tv.app.atsc3receiver1_0.app.LLSReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * An activity for selecting from a list of samples.
 */
public class SampleChooserFragment extends Fragment {

  protected static final String TAG = "SampleChooserFragment";
  private Activity activity;
  private Context context;
  private AdapterLoader loaderTask;
  private ExpandableListView sampleListView;
    private static boolean test=false;

  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    activity = getActivity();
    context=activity.getApplicationContext();
    return inflater.inflate(R.layout.sample_chooser_activity, container, false);
  }

  @Override
  public void onStart() {
    super.onStart();

          String[] uris;

          Intent intent = getActivity().getIntent();

          String dataUri = intent.getDataString();

//        String[] uris;
          if (dataUri != null) {
              uris = new String[]{dataUri};
          } else {
              ArrayList<String> uriList = new ArrayList<>();
              AssetManager assetManager = getActivity().getAssets();
              try {
                  for (String asset : assetManager.list("")) {

                      if (asset.endsWith(".exolist.json")) {
                          uriList.add("asset:///" + asset);
                      }
                  }
              } catch (IOException e) {
                  Toast.makeText(activity.getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
                          .show();
              }
              uris = new String[uriList.size()];
              uriList.toArray(uris);
              Arrays.sort(uris);
          }
          loaderTask = new AdapterLoader(uris);
          loaderTask.execute();
        if (activity instanceof MainActivity){
            ((MainActivity)getActivity()).startLLSReceiver();

        }else{
            ((MainPhoneActivity)getActivity()).startLLSReceiver();

        }


  }
  @Override
  public void onStop(){
    super.onStop();
    loaderTask=null;
  }

  @Override
  public void onResume(){
      super.onResume();

  }

//  public void refreshFragments(){
//      String[] uris;
//      Intent intent = getActivity().getIntent();
//      String dataUri = intent.getDataString();
//
////        String[] uris;
//      if (dataUri != null) {
//          uris = new String[]{dataUri};
//      } else {
//          ArrayList<String> uriList = new ArrayList<>();
//          AssetManager assetManager = getActivity().getAssets();
//          try {
//              for (String asset : assetManager.list("")) {
//
//                  if (asset.endsWith(".exolist2.json")) {
//                      uriList.add("asset:///" + asset);
//                  }
//              }
//          } catch (IOException e) {
//              Toast.makeText(activity.getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
//                      .show();
//          }
//          uris = new String[uriList.size()];
//          uriList.toArray(uris);
//          Arrays.sort(uris);
//      }
//
//        loaderTask = new AdapterLoader(uris);
//        loaderTask.execute();
//  }
  private final class AdapterLoader extends AsyncTask<Void, Void, SampleListLoader> {

    private String[] uris;
    public AdapterLoader(String[] uris) {
      this.uris = uris;

    }

    @Override
    protected SampleListLoader doInBackground(Void... voids) {
      return new SampleListLoader(context, uris);
    }

    @Override
    protected void onPostExecute(SampleListLoader sampleList){
        onSampleGroupsAddATSC(sampleList.getSampleGroup(), sampleList.getError() );
    }

    private void onSampleGroupsAddATSC(List<SampleGroup> groups, boolean sawError){
        SampleGroup g=new SampleGroup("ATSC3.0 Content");
        Log.d("***", "size: "+LLSReceiver.getInstance().slt.mSLTData.mServices.size());
        for (int i=0; i< LLSReceiver.getInstance().slt.mSLTData.mServices.size(); i++) {
            String url = LLSReceiver.getInstance().slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationIpAddress;
            String port = LLSReceiver.getInstance().slt.mSLTData.mServices.get(i).broadcastServices.get(0).slsDestinationUdpPort;
            String name = ATSC3.manifest; /* TODO detect automatically from USBD*/
            String title = LLSReceiver.getInstance().slt.mSLTData.mServices.get(i).shortServiceName;
            ATSCSample s = new ATSCSample(title, null, null, null, false, url, port, name);
            g.samples.add(s);
        }
        groups.add(0,g);
        onSampleGroups(groups, sawError);
    }

    private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
      if (sawError) {
        Toast.makeText(context, R.string.sample_list_load_error, Toast.LENGTH_LONG)
                .show();
      }else {
        sampleListView = (ExpandableListView) activity.findViewById(R.id.sample_list);
        final SampleAdapter sampleAdapter=  new SampleAdapter(groups);
        sampleListView.setAdapter(sampleAdapter);
        sampleListView.setOnChildClickListener(new OnChildClickListener() {
          @Override
          public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
                                      int childPosition, long id) {
            onSampleSelected(groups.get(groupPosition).samples.get(childPosition), childPosition);
            return true;
          }
        });

          sampleListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
              @Override
              public void onGroupExpand(int groupPosition){
                  if (groupPosition==0){
                      Log.d(TAG,"Started Flute Signalling receivers");
//                      ((MainActivity) activity).startFluteSession(FluteReceiver.SIGNALLING);
                  }

              }
          });

          sampleListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
              @Override
              public void onGroupCollapse(int groupPosition){
                  if (groupPosition==0){
                      Log.d(TAG,"Stopped Flute Signalling receivers");
//                      ((MainActivity) activity).stopFluteSession(FluteReceiver.SIGNALLING);
                  }

              }
          });

      }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                onSampleSelected(groups.get(0).samples.get(0), 0);
            }
        },8000);


    }

    private void onSampleSelected(Sample sample, int childPosition) {
//        ((MainActivity)activity).stopLLSReceiver();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).ExoPlayerStarted = true;
        }else{
            ((MainPhoneActivity) activity).ExoPlayerStarted = true;

        }
        ATSC3.dataSourceIndex=childPosition;
        activity.startActivity(sample.buildIntent(context));
    }

  }




    private final class SampleAdapter extends BaseExpandableListAdapter {

//    private final Context context;
        private final List<SampleListLoader.SampleGroup> sampleGroups;

        public SampleAdapter(List<SampleListLoader.SampleGroup>  sampleGroups) {
          this.sampleGroups = sampleGroups;
        }

        @Override
        public Sample getChild(int groupPosition, int childPosition) {
          return getGroup(groupPosition).samples.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
          return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
          View view = convertView;
          if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.simple_text_layout, parent,
                    false);

          }
          ((TextView) view).setText(getChild(groupPosition, childPosition).name);

          return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
          return getGroup(groupPosition).samples.size();
        }

        @Override
        public SampleListLoader.SampleGroup  getGroup(int groupPosition) {
          return sampleGroups.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
          return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
          View view = convertView;
          if (view == null) {
            view = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1,
                    parent, false);
          }
          ((TextView) view).setText(getGroup(groupPosition).title);
          return view;
        }

        @Override
        public int getGroupCount() {
          return sampleGroups.size();
        }

        @Override
        public boolean hasStableIds() {
          return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
          return true;
        }

      }


}
