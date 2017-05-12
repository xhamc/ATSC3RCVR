package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.upstream.AssetDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;

import java.io.IOException;


/**
 * Maps the filemanager to the DataSource needed by ExoPlayer
 * Supports non flute content as well (asset/http/file) when BaseUrl
 * in Manifest is absolute address. Used for Ad insertion for instance
 * Created by xhamc on 3/16/17.
 */

public class FluteDataSource implements DataSource {


//    public Receiver Receiver;
    private static final String TAG="FluteDataSource";
    private DataSpec mExoPlayerUri;
    private FluteReceiver fluteReceiver;
    private FluteFileManager fileManager;
    private int thread;
    private int indexToFileManager=0;



    private static final String SCHEME_ASSET = "asset";
    private static final String SCHEME_FLUTE = "flute";
    private static final String SCHEME_HTTP  = "http";
    private final static String XLINK_HREF  ="/@xlink:href ";

    private final DataSource fileDataSource;
    private final DataSource assetDataSource;
    private final DataSource httpDataDataSource;

    private DataSource dataSource;
    private TransferListener<? super DataSource> listener;

    /**
     * Constructs a new instance that delegates to a provided {@link DataSource} for URI schemes other
     * than file, asset and content.
     *
     * @param context A context.
     * @param thread  link the filemanager to a thread.
     */

    public FluteDataSource(int thread, Context context){

        this.listener=new TransferListener<DataSource>(){

            @Override
            public void onTransferStart(DataSource source, DataSpec dataSpec) {

            }

            @Override
            public void onBytesTransferred(DataSource source, int bytesTransferred) {

            }

            @Override
            public void onTransferEnd(DataSource source) {

            }
        };

        this.fileDataSource = new FileDataSource(listener);
        this.assetDataSource = new AssetDataSource(context, listener);
        this.httpDataDataSource = new DefaultHttpDataSource(ATSC3.userAgent, null, listener, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);

        indexToFileManager=ATSC3.dataSourceIndex;
        fluteReceiver=FluteReceiver.getInstance();
        fileManager=fluteReceiver.mFluteTaskManager[indexToFileManager].fileManager();
        if (thread>2){
            Log.d(TAG,"Created new FluteDataSource >2: "+thread);
        }
        this.thread=thread;
        Log.d(TAG, "Created new FluteDataSource at thread: "+thread+ "  that points to filemanager at index: "+indexToFileManager);

    }
    @Override
    public long open(DataSpec dataSpec) throws IOException{
        String scheme = dataSpec.uri.getScheme();
        try {
        if (SCHEME_FLUTE.equals(scheme)) {
//            if (dataSpec.uri.getPath().startsWith("asset/")){
//                String absoluteUri="asset://".concat(dataSpec.uri.getPath().substring("asset/".length()));
//                dataSpec=new DataSpec(Uri.parse(absoluteUri));
//            }else{
                mExoPlayerUri=dataSpec;
                return fileManager.open(dataSpec, thread);
//            }
        }
        Log.d("TAG", "ExoPlayer trying to open :"+dataSpec.uri);

            if (Util.isLocalFileUri(dataSpec.uri)) {
                dataSource = fileDataSource;
            } else if (SCHEME_ASSET.equals(scheme)) {

                dataSource = assetDataSource;
            } else if (SCHEME_HTTP.equals(scheme)) {
                dataSource = httpDataDataSource;
            } else {
                Log.e(TAG, "URI scheme not recognized");
                dataSource = null;
            }
            // Open the source and return.
            if (dataSource != null) return dataSource.open(dataSpec);
        }catch(IOException e){
            Log.e(TAG,"Couldn't find file: "+dataSpec.uri);
            throw new IOException(e);
        }

        return -1;

    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (dataSource!= null)
            return dataSource.read(buffer, offset, readLength);

        return fileManager.read(buffer, offset, readLength, thread );

    }

    @Override
    public Uri getUri() {
        if (dataSource!=null)
            return dataSource == null ? null : dataSource.getUri();

        return mExoPlayerUri.uri;
    }

    @Override
    public void close() throws IOException {
        if (dataSource != null) {
            try {
                dataSource.close();
            } finally {
                dataSource = null;
            }
        }
    }





}
