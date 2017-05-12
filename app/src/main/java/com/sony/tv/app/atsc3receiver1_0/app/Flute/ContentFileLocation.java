package com.sony.tv.app.atsc3receiver1_0.app.Flute;

/**
 * Used by filemanager for managing relevant Fluet file propoerties
 * Created by xhamc on 3/21/17.
 */

public class ContentFileLocation{
    public String fileName;
    public int toi;
    public int tsi;
    public long time;
    public long expiry;
    public int start;
    public int contentLength;
    public int nextWritePosition;
    public byte[] storage;

    //        public int toi;
    public ContentFileLocation(String fileName, int toi, int tsi, int start, int length, long time, long expiry,  byte[] storage){
        this.fileName=fileName;
        this.toi=toi;
        this.tsi=tsi;
        this.time=time;
        this.expiry=expiry;
        this.start=start;
        this.contentLength=length;
        this.nextWritePosition=0;
        this.storage=storage;
    }



}