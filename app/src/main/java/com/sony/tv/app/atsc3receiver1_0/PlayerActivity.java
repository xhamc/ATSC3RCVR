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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.AdCategory;
import com.sony.tv.app.atsc3receiver1_0.app.AdContent;
import com.sony.tv.app.atsc3receiver1_0.app.AdsListAdapter;
import com.sony.tv.app.atsc3receiver1_0.app.FluteReceiver;
import com.sony.tv.app.atsc3receiver1_0.app.LLSReceiver;
import com.sony.tv.app.atsc3receiver1_0.app.NewAddDialogFragment;

import org.greenrobot.eventbus.EventBus;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.sony.tv.app.atsc3receiver1_0.app.ATSC3.getContext;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity implements OnClickListener, ExoPlayer.EventListener,
    PlaybackControlView.VisibilityListener{

  public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
  public static final String DRM_LICENSE_URL = "drm_license_url";
  public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";
  public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";
  public static final String CHANNEL_NAME = "channel_name";
  public static final String USB_EVENT = "usb_broadcast_event";
  public static final String TAG = "PlayerActivity";
  private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";


  public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
  public static final String EXTENSION_EXTRA = "extension";

  public static final String ACTION_VIEW_LIST =
      "com.google.android.exoplayer.demo.action.VIEW_LIST";
  public static final String URI_LIST_EXTRA = "uri_list";
  public static final String EXTENSION_LIST_EXTRA = "extension_list";

  private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
  private static final CookieManager DEFAULT_COOKIE_MANAGER;
  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private Realm realm;
  private Handler mainHandler;
  private EventLogger eventLogger;
  private SimpleExoPlayerView simpleExoPlayerView;
  private LinearLayout debugRootView;
  private LinearLayout infoLayout;
  private LinearLayout debugLayout;
  private LinearLayout adSelectLayout;
  private TextView debugTextView;
  private RecyclerView adRecyclerView;
  private AdsListAdapter adsListAdapter;
  private TextView noAdFoundTextView;
  private ImageButton addNewAdButton;

  private Button retryButton;


  private DataSource.Factory mediaDataSourceFactory;
  private SimpleExoPlayer player;
  private DefaultTrackSelector trackSelector;
  private TrackSelectionHelper trackSelectionHelper;
  private DebugTextViewHelper debugViewHelper;
  private boolean playerNeedsSource;

  private boolean shouldAutoPlay;
  private int resumeWindow;
  private long resumePosition;
  private BroadcastReceiver usbBroadCastReceiver;
  private UsbManager usbManager;
  private PendingIntent mPermissionIntent;

  private boolean stopped=false;

  Timer timerForChannelChange;
  TimerTask timerTaskUp,timerTaskDown;

  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    shouldAutoPlay = true;
    clearResumePosition();
    mediaDataSourceFactory = buildDataSourceFactory(true);
    mainHandler = new Handler();
    realm = Realm.getDefaultInstance();
    if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
      CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
    }

    setContentView(R.layout.player_activity);
    View rootView = findViewById(R.id.root);
    rootView.setOnClickListener(this);
    debugRootView = (LinearLayout) findViewById(R.id.controls_root);
    infoLayout = (LinearLayout) findViewById(R.id.info_layout);
    debugLayout = (LinearLayout) findViewById(R.id.debug_layout);
    adSelectLayout = (LinearLayout) findViewById(R.id.ad_drop_down_layout);
    adRecyclerView = (RecyclerView) findViewById(R.id.ad_recycler_view);
    noAdFoundTextView = (TextView) findViewById(R.id.empty_text);
    addNewAdButton = (ImageButton) findViewById(R.id.add_new_ad_button);
    addNewAdButton.setSelected(true);

    addNewAdButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        closeAdSelectorLayout();
        showNewAdDialogScreen();
      }
    });



    debugTextView = (TextView) findViewById(R.id.debug_text_view);
    retryButton = (Button) findViewById(R.id.retry_button);
    retryButton.setOnClickListener(this);

    simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
    simpleExoPlayerView.setControllerVisibilityListener(this);
    simpleExoPlayerView.requestFocus();
    timerForChannelChange=new Timer();
    timerTaskDown=new TimerTask() {
      @Override
      public void run() {
        new DispatchKey(167);
      }
    };
    timerForKey.schedule(timerTask,5*60*1000);
  }


  @Override
  public void onNewIntent(Intent intent) {
    releasePlayer();
    shouldAutoPlay = true;
    clearResumePosition();
    if (intent.getAction().equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)){
      Log.d(TAG, "Do something");
    }
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
//    EventBus.getDefault().register(this);
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
    stopped=false;
  }

  @Override
  public void onResume() {
    super.onResume();
    if ((Util.SDK_INT <= 23 || player == null)) {
      initializePlayer();
    }

    usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
    Iterator<UsbDevice> deviceIterator = devices.values().iterator();
    while (deviceIterator.hasNext()){
      UsbDevice device = deviceIterator.next();

      String model = device.getDeviceName();
      String deviceId = String.valueOf(device.getDeviceId());
      String vendor = device.getManufacturerName();

      if (vendor.equals("Sony")){
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        usbBroadCastReceiver = new UsbBroadcastReceiver();
        registerReceiver(usbBroadCastReceiver, filter);

      }

    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT <= 23) {
      releasePlayer();
    }
  }



  @Override
  public void onStop() {
    super.onStop();
//    EventBus.getDefault().unregister(this);
    if (Util.SDK_INT > 23) {
      releasePlayer();
    }
    timerTaskDown.cancel();
    timerTaskUp.cancel();
    timerForChannelChange.cancel();
    timerForChannelChange=null;
    timerTaskDown=null;
    timerTaskUp=null;
    stopped=true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    realm.close();

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      initializePlayer();
    } else {
      showToast(R.string.storage_permission_denied);
      finish();
    }
  }

  // Activity input

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {

    Log.d("KEY","Key Pressed: "+event.getKeyCode());
    Timer t=new Timer();

    switch (event.getKeyCode()) {
      case 7:
        showAdSelectorLayout();
        break;
      case 15:
        closeAdSelectorLayout();
        break;
      case 19:
        //Up button clicked
        showPlayControllerLayout();
               break;
      case 20:
        //Down button clicked
//        simpleExoPlayerView.setUseController(false);
//        simpleExoPlayerView.hideController();
        break;
      case 21:
        //Left button clicked
        //    showDebugLayout();
        break;
      case 22:
        //Right arrow clicked
        //   showInfoLayout();
        break;
      case 166:
        ATSC3.channelUp(this);
      timerTask=new TimerTask() {
          @Override
          public void run() {
          new DispatchKey(167);
          }
      };

      timerForKey.schedule(timerTask,5*60*1000);
        return true;
      case 167:
        ATSC3.channelDown(this);
      timerTask=new TimerTask() {
          @Override
          public void run() {
          new DispatchKey(166);
          }
      };
      timerForKey.schedule(timerTask,5*60*1000);
        return true;

    }
    // Show the controls on any key event.
    simpleExoPlayerView.showController();
    // If the event was not handled then see if the player view can handle it as a media key event.
    return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
  }

  private class DispatchKey implements Runnable{
    private final int key;
    public DispatchKey(int key){
      this.key=key;
      runOnUiThread(this);
    }
    public void run(){
      KeyEvent keyEvent=new KeyEvent(KeyEvent.ACTION_DOWN,key);
      dispatchKeyEvent(keyEvent);
    }
  }

  float downXValue;
  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    switch (ev.getAction()){
      case MotionEvent.ACTION_DOWN:
        downXValue=ev.getX();

        break;
      case MotionEvent.ACTION_UP:
        if (ev.getX()-downXValue>200){
          ATSC3.channelUp(this);


        }else if(ev.getX()-downXValue<-200){
          ATSC3.channelDown(this);

        }else{
          if (ev.getX()>simpleExoPlayerView.getWidth()/3 && ev.getX()<simpleExoPlayerView.getWidth()*2/3){
            showAdSelectorLayout();
          }else{
            closeAdSelectorLayout();
          }
        }
        break;
    }

    return super.dispatchTouchEvent(ev);
  }


  private void closeAdSelectorLayout() {
    simpleExoPlayerView.setUseController(false);
    simpleExoPlayerView.hideController();
    infoLayout.setVisibility(View.GONE);
    debugLayout.setVisibility(View.GONE);
    adSelectLayout.setVisibility(View.GONE);

  }

  private void showNewAdDialogScreen() {
    NewAddDialogFragment dialogFragment = new NewAddDialogFragment();
    dialogFragment.show(getFragmentManager(), "Dialog");
  }


  private void showAdSelectorLayout() {
    simpleExoPlayerView.setUseController(false);
    simpleExoPlayerView.hideController();
    infoLayout.setVisibility(View.GONE);
    debugLayout.setVisibility(View.GONE);
    adSelectLayout.setVisibility(View.VISIBLE);
    addNewAdButton.requestFocus();


    RealmResults<AdCategory> categoryList = realm.where(AdCategory.class).findAll();
    if (categoryList != null){
      int count = categoryList.size();
      List<AdContent> ads = categoryList.get(0).getAds();
      Log.d(TAG, "Size: " + count);
    }
    if (categoryList != null && categoryList.size() > 0){
      showEmptyText(false);
      adRecyclerView.setHasFixedSize(true);
      adRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
      adsListAdapter = new AdsListAdapter(categoryList, realm);
      adRecyclerView.setAdapter(adsListAdapter);
    }else {
      showEmptyText(true);
    }
  }

  private void showPlayControllerLayout() {
    simpleExoPlayerView.setUseController(true);
    simpleExoPlayerView.showController();
    debugLayout.setVisibility(View.GONE);
    infoLayout.setVisibility(View.GONE);
    adSelectLayout.setVisibility(View.GONE);

  }

  private void showInfoLayout() {
    simpleExoPlayerView.setUseController(false);
    simpleExoPlayerView.hideController();
    debugLayout.setVisibility(View.GONE);
    adSelectLayout.setVisibility(View.GONE);
    infoLayout.setVisibility(View.VISIBLE);
  }

  private void showDebugLayout() {
    simpleExoPlayerView.setUseController(false);
    simpleExoPlayerView.hideController();
    infoLayout.setVisibility(View.GONE);
    adSelectLayout.setVisibility(View.GONE);
    debugLayout.setVisibility(View.VISIBLE);

  }

//  @Subscribe(threadMode = ThreadMode.MAIN)
//  public void onNewAddInserted(OnNewAdInsertedEvent event) {
//    showAdSelectorLayout();
//
//  };

//
//    Log.d("KEY","Key Pressed: "+event.getKeyCode());
//    boolean channelChange=false;
//    if (event.getKeyCode()==166){
//      channelChange=ATSC3.channelUp(this);
//    }else if(event.getKeyCode()==167){
//      channelChange=ATSC3.channelDown(this);
//    }
//    if (channelChange){
//
//    }
//    // Show the controls on any key event.
//    simpleExoPlayerView.showController();
//    // If the event was not handled then see if the player view can handle it as a media key event.
//    return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
//  }

  // OnClickListener methods

  @Override
  public void onClick(View view) {
    if (view == retryButton) {
      initializePlayer();
    } else if (view.getParent() == debugRootView) {
      MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        trackSelectionHelper.showSelectionDialog(this, ((Button) view).getText(),
            trackSelector.getCurrentMappedTrackInfo(), (int) view.getTag());
      }
    }
  }

  public void showEmptyText(boolean showText) {
    if (showText){
      adRecyclerView.setVisibility(View.GONE);
      noAdFoundTextView.setVisibility(View.VISIBLE);
    }else {
      noAdFoundTextView.setVisibility(View.GONE);
      adRecyclerView.setVisibility(View.VISIBLE);
    }
  }

  // PlaybackControlView.VisibilityListener implementation

  @Override
  public void onVisibilityChange(int visibility) {
    debugRootView.setVisibility(visibility);
  }

  // Internal methods

  private void initializePlayer() {
    Intent intent = getIntent();
    if (player == null) {
      boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
      UUID drmSchemeUuid = intent.hasExtra(DRM_SCHEME_UUID_EXTRA)
          ? UUID.fromString(intent.getStringExtra(DRM_SCHEME_UUID_EXTRA)) : null;
      DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
      if (drmSchemeUuid != null) {
        String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
        String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
        Map<String, String> keyRequestProperties;
        if (keyRequestPropertiesArray == null || keyRequestPropertiesArray.length < 2) {
          keyRequestProperties = null;
        } else {
          keyRequestProperties = new HashMap<>();
          for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
            keyRequestProperties.put(keyRequestPropertiesArray[i],
                keyRequestPropertiesArray[i + 1]);
          }
        }
        try {
          drmSessionManager = buildDrmSessionManager(drmSchemeUuid, drmLicenseUrl,
              keyRequestProperties);
        } catch (UnsupportedDrmException e) {
          int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
              : (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                  ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
          showToast(errorStringId);
          return;
        }
      }

      @SimpleExoPlayer.ExtensionRendererMode int extensionRendererMode = SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF;

      TrackSelection.Factory videoTrackSelectionFactory =
          new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
      trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
      trackSelectionHelper = new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
      player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl(),
          drmSessionManager, extensionRendererMode);
      player.addListener(this);

      eventLogger = new EventLogger(trackSelector);
      player.addListener(eventLogger);
      player.setAudioDebugListener(eventLogger);
      player.setVideoDebugListener(eventLogger);
      player.setMetadataOutput(eventLogger);

      simpleExoPlayerView.setPlayer(player);
      player.setPlayWhenReady(shouldAutoPlay);
      debugViewHelper = new DebugTextViewHelper(player, debugTextView);
      debugViewHelper.start();
      playerNeedsSource = true;
    }
    if (playerNeedsSource) {
      String action = intent.getAction();
      Uri[] uris;
      String[] extensions;
      if (ACTION_VIEW.equals(action)) {
        uris = new Uri[] {intent.getData()};
        extensions = new String[] {intent.getStringExtra(EXTENSION_EXTRA)};
      } else if (ACTION_VIEW_LIST.equals(action)) {
        String[] uriStrings = intent.getStringArrayExtra(URI_LIST_EXTRA);
        uris = new Uri[uriStrings.length];
        for (int i = 0; i < uriStrings.length; i++) {
          uris[i] = Uri.parse(uriStrings[i]);
        }
        extensions = intent.getStringArrayExtra(EXTENSION_LIST_EXTRA);
        if (extensions == null) {
          extensions = new String[uriStrings.length];
        }
      } else {
        showToast(getString(R.string.unexpected_intent_action, action));
        return;
      }
      if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
        // The player will be reinitialized if the permission is granted.
        return;
      }
      MediaSource[] mediaSources = new MediaSource[uris.length];
      for (int i = 0; i < uris.length; i++) {
        mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
      }
      MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
          : new ConcatenatingMediaSource(mediaSources);
      boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
//      if (haveResumePosition) {
//        player.seekTo(resumeWindow, resumePosition);
//      }
      player.prepare(mediaSource, !haveResumePosition, false);
      playerNeedsSource = false;
      updateButtonVisibilities();
    }
  }

  private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
    int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
        : uri.getLastPathSegment());
    switch (type) {
      case C.TYPE_SS:
        return new SsMediaSource(uri, buildDataSourceFactory(false),
            new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
      case C.TYPE_DASH:
        return new DashMediaSource(uri, buildDataSourceFactory(false),
            new DefaultDashChunkSource.Factory(mediaDataSourceFactory,1), mainHandler, eventLogger);
      case C.TYPE_HLS:
        return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
      case C.TYPE_OTHER:
        return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
            mainHandler, eventLogger);
      default: {
        throw new IllegalStateException("Unsupported type: " + type);
      }
    }
  }

  private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid,
                                                                         String licenseUrl, Map<String, String> keyRequestProperties) throws UnsupportedDrmException {
    if (Util.SDK_INT < 18) {
      return null;
    }
    HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
        buildHttpDataSourceFactory(false), keyRequestProperties);
    return new DefaultDrmSessionManager<>(uuid,
        FrameworkMediaDrm.newInstance(uuid), drmCallback, null, mainHandler, eventLogger);
  }

  public void releasePlayer() {
    if (player != null) {
      debugViewHelper.stop();
      debugViewHelper = null;
      shouldAutoPlay = player.getPlayWhenReady();
      //updateResumePosition();
      clearResumePosition();
      player.release();
      player = null;
      trackSelector = null;
      trackSelectionHelper = null;
      eventLogger = null;
    }
  }

  private void updateResumePosition() {
    resumeWindow = player.getCurrentWindowIndex();
    resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
        : C.TIME_UNSET;
  }

  private void clearResumePosition() {
    resumeWindow = C.INDEX_UNSET;
    resumePosition = C.TIME_UNSET;
  }

  /**
   * Returns a new DataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
   *     DataSource factory.
   * @return A new DataSource factory.
   */
  private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
    return ((ATSC3) getApplication())
        .buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  /**
   * Returns a new HttpDataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
   *     DataSource factory.
   * @return A new HttpDataSource factory.
   */
  private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
    return ( (ATSC3) getApplication()).buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  // ExoPlayer.EventListener implementation

  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    if (playbackState == ExoPlayer.STATE_ENDED) {
      showControls();
    }
    updateButtonVisibilities();
  }

  @Override
  public void onPositionDiscontinuity() {
    if (playerNeedsSource) {
      // This will only occur if the user has performed a seek whilst in the error state. Update the
      // resume position so that if the user then retries, playback will resume from the position to
      // which they seeked.
      updateResumePosition();
    }
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest) {
    // Do nothing.
  }

  @Override
  public void onPlayerError(ExoPlaybackException e) {
    String errorString = null;
    if (e.type == ExoPlaybackException.TYPE_RENDERER) {
      Exception cause = e.getRendererException();
      if (cause instanceof DecoderInitializationException) {
        // Special case for decoder initialization failures.
        DecoderInitializationException decoderInitializationException =
            (DecoderInitializationException) cause;
        if (decoderInitializationException.decoderName == null) {
          if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
            errorString = getString(R.string.error_querying_decoders);
          } else if (decoderInitializationException.secureDecoderRequired) {
            errorString = getString(R.string.error_no_secure_decoder,
                decoderInitializationException.mimeType);
          } else {
            errorString = getString(R.string.error_no_decoder,
                decoderInitializationException.mimeType);
          }
        } else {
          errorString = getString(R.string.error_instantiating_decoder,
              decoderInitializationException.decoderName);
        }
      }
    }
    if (errorString != null) {
      showToast(errorString);
    }
    playerNeedsSource = true;
    if (isBehindLiveWindow(e)) {
      clearResumePosition();
      initializePlayer();
    } else {
      updateResumePosition();
      updateButtonVisibilities();
      showControls();
    }
  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    updateButtonVisibilities();
    MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
    if (mappedTrackInfo != null) {
      if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
          == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        showToast(R.string.error_unsupported_video);
      }
      if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
          == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
        showToast(R.string.error_unsupported_audio);
      }
    }
  }

  // User controls

  private void updateButtonVisibilities() {
    debugRootView.removeAllViews();

    retryButton.setVisibility(playerNeedsSource ? View.VISIBLE : View.GONE);
    debugRootView.addView(retryButton);

    if (player == null) {
      return;
    }

    MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
    if (mappedTrackInfo == null) {
      return;
    }

    for (int i = 0; i < mappedTrackInfo.length; i++) {
      TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
      if (trackGroups.length != 0) {
        Button button = new Button(this);
        int label;
        switch (player.getRendererType(i)) {
          case C.TRACK_TYPE_AUDIO:
            label = R.string.audio;
            break;
          case C.TRACK_TYPE_VIDEO:
            label = R.string.video;
            break;
          case C.TRACK_TYPE_TEXT:
            label = R.string.text;
            break;
          default:
            continue;
        }
        button.setText(label);
        button.setTag(i);
        button.setOnClickListener(this);
        debugRootView.addView(button, debugRootView.getChildCount() - 1);
      }
    }
  }

  private void showControls() {
    debugRootView.setVisibility(View.VISIBLE);
  }

  private void showToast(int messageId) {
    showToast(getString(messageId));
  }

  private void showToast(String message) {
    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
  }

  private static boolean isBehindLiveWindow(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }
    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof BehindLiveWindowException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }


  public class UsbBroadcastReceiver extends BroadcastReceiver {

      @Override
      public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          Log.d(USB_EVENT, action);
          if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
              UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null){
              if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                Log.d(TAG, "Usb permission granted");

              }else {
                Log.d(TAG, "Usb permission denied");
                usbManager.requestPermission(device, mPermissionIntent);
              }

            }

          }
      }

    private UsbInterface findAdbInterface(UsbDevice device) {
      Log.d(TAG, "findAdbInterface " + device);
      int count = device.getInterfaceCount();
      for (int i = 0; i < count; i++) {
        UsbInterface intf = device.getInterface(i);
        if (intf.getInterfaceClass() == 255 && intf.getInterfaceSubclass() == 66 &&
                intf.getInterfaceProtocol() == 1) {
          return intf;
        }
      }
      return null;
    }
  }

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this){
          UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
            if (device != null){
              Log.d(TAG, "Usb permission granted");
            }
          }else {
            Log.d(TAG, "Usb permission denied");
          }
        }

      }

    }
  };



}
