package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;

/**
 * Interface to support Qualcomm and NAB flavors of FileManager
 * Created by xhamc on 4/2/17.
 */

public interface FluteFileManagerBase {

    void reset();
    void resetTimeStamp();
    boolean create(RouteDecodeBase r) throws Exception;

    String write(RouteDecodeBase r, byte[] input, int offset, int length);

    long open(DataSpec dataSpec, int thread) throws IOException;
    int read(byte[] buffer, int offset, int readLength, int thread) throws IOException;

    int getNumberOfAudioFiles();
}
