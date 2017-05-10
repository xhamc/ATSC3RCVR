package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import com.sony.tv.app.atsc3receiver1_0.app.LLS.EFDT_DATA;
import com.sony.tv.app.atsc3receiver1_0.app.LLS.LLSXmlParse;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xhamc on 4/2/17.
 */

public class RouteDecodeQu implements RouteDecodeBase {

    private final static String TAG="RouteDecodeQu";
    private final int HEADER_LENGTH_POSITION=2;
    private final int TSI_POSITION=8;
    private final int TOI_POSITION=12;
    private final int INSTANCE_ID_POSITION=17;
    private final int HEADER_EXTENSIONS =16;
    private final int ARRAY_POSITION=16;
    private final int EXPIRY_POSITION=24;
    private final int MAX_OBJECT_SIZE_POSITION=28;
    private final int EFDT_CONTENT_START_POSITION=40;
    public final static int PAYLOAD_START_POSITION=20;
    private final byte[] PREAMBLE=new byte[]{(byte) 0x10, (byte) 0xA0};
    private final byte[] EXFT_192_PREAMBLE=new byte[]{(byte) 0xC0, (byte) 0x10}; // mask last byte with 0xf0
    private final byte[] EXFT_64_PREAMBLE=new byte[]{(byte) 0x40, (byte) 0x40}; // mask last byte with 0xf0


    public boolean valid=false;
    public boolean efdt=false;
    public int tsi;
    public int toi;
    public int instanceId;
    public int maxObjectSize;
    public int arrayPosition;
    public long expiry;
    public String fileName="";
    public int contentLength;
    public int efdt_toi;

    public void fileName(String fileName){
        this.fileName=fileName;
    }
    public boolean valid(){return valid;}

    public int tsi(){return tsi;}
    public int toi(){return toi;}
    public int arrayPosition(){return (int) arrayPosition;}
    public String fileName() {return fileName;}
    public int contentLength(){return contentLength;}
    public int efdt_toi(){return efdt_toi;}

    ReentrantLock lock=new ReentrantLock();

    public RouteDecodeQu(byte[] data, int packetSize) {
        if (data.length < 0x20) return;
        if (data[0] == PREAMBLE[0] && data[1] == PREAMBLE[1]) {

            toi = ((data[TOI_POSITION] &0xff) <<24 | (data[TOI_POSITION + 1] &0xff)<<16 | (data[TOI_POSITION + 2] &0xff) <<8 | (data[TOI_POSITION + 3]&0xff));
            tsi = ((data[TSI_POSITION] &0xff) <<24 | (data[TSI_POSITION + 1] &0xff)<<16 | (data[TSI_POSITION + 2] &0xff) <<8 | (data[TSI_POSITION + 3]&0xff));
            byte length = data[HEADER_LENGTH_POSITION];
            if (length == 4) {
                arrayPosition = ((data[ARRAY_POSITION]&0xff) <<24 | (data[ARRAY_POSITION + 1] &0xff)<<16 | (data[ARRAY_POSITION + 2] &0xff) <<8 | (data[ARRAY_POSITION + 3] & 0xff));
                valid = true;
            } else if (length>0 && toi==0) {
                if (data[HEADER_EXTENSIONS] == EXFT_192_PREAMBLE[0] && (data[HEADER_EXTENSIONS + 1] & 0xF0) == EXFT_192_PREAMBLE[1]) {
                    instanceId = ((data[INSTANCE_ID_POSITION] & 0x0F) << 16 | (data[INSTANCE_ID_POSITION + 1] & 0xff) << 8 | (data[INSTANCE_ID_POSITION + 2] * 0xff));
                    expiry = System.currentTimeMillis() + ((data[EXPIRY_POSITION] & 0xff) << 24 | (data[EXPIRY_POSITION + 1] & 0xff) << 16 | (data[EXPIRY_POSITION + 2] & 0xff) << 8 | (data[EXPIRY_POSITION + 3] & 0xff)) * 1000;
                    maxObjectSize = ((data[MAX_OBJECT_SIZE_POSITION] & 0xff) << 24 | (data[MAX_OBJECT_SIZE_POSITION + 1] & 0xff) << 16 | (data[MAX_OBJECT_SIZE_POSITION + 2] & 0xff) << 8 | (data[MAX_OBJECT_SIZE_POSITION + 3] & 0xff));
                    String s = new String(data, EFDT_CONTENT_START_POSITION, packetSize - EFDT_CONTENT_START_POSITION);
                    EFDT_DATA e = (new EFDTXmlParse(s, LLSXmlParse.EFDT_INSTANCE_TAG)).EFDTParse();
                    fileName = e.location;
                    contentLength = e.contentlength;
                    efdt_toi = e.toi;
                    efdt = true;
                } else {
                    Log.e(TAG, "Unknown Route preamble: " + data[HEADER_EXTENSIONS] + "  " + (data[HEADER_EXTENSIONS + 1] & 0xF0));
                }

            }else {
                Log.e(TAG,"Unknown Route header length: "+length);
            }
        }
    }

}
