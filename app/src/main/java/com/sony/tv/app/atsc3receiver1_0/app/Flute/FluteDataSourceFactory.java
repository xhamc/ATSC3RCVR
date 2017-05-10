package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import com.google.android.exoplayer2.upstream.DataSource;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;

/**
 * Factory class to create fulte data sources for Exoplayer. the thread is needed so that one filemanager
 * can handle all requests from the player (signaling/audio/video)
 * Created by xhamc on 3/21/17.
 */

public class FluteDataSourceFactory implements DataSource.Factory{
    public static int thread=-1;

    public FluteDataSourceFactory(){
//        thread=-1;
    }
    @Override
    public DataSource createDataSource() {


        thread++;
        if (thread>99) thread=0;
        return new FluteDataSource(thread, ATSC3.getContext());

    }


}
