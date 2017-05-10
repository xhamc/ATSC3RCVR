package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

/**
 * Created by xhamc on 5/9/17.
 */

public class RouteDecode implements RouteDecodeBase {

    private final static String TAG="RouteDecodeNAB";
    private final byte[] PREAMBLE=new byte[]{(byte) 0x12, (byte) 0xA0};
    private final byte[] EXFT_64_PREAMBLE=new byte[]{(byte) 0x40, (byte) 0x00}; // mask last byte with 0xf0
    private final int HEADER_LENGTH_POSITION=2;
    private final int TSI_POSITION=8;
    private final int TOI_POSITION=12;
    private final int HEADER_EXTENSIONS =16;
    private final int INSTANCE_ID_POSITION=17;
    private final int TOTAL_FILE_LENGTH=20;
    private final int MAX_OBJECT_SIZE_POSITION=24;
    private final int ARRAY_POSITION=32;
    public final static int PAYLOAD_START_POSITION=36;

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

    public int tsi(){return tsi;}
    public int arrayPosition(){return arrayPosition;}
    public int toi(){return toi;}
    public String fileName(){return fileName;}
    public int contentLength(){return contentLength;}
    public int efdt_toi(){ return toi;}
    public boolean valid(){return valid;}

    public RouteDecode(byte[] data, int packetSize) {
        valid=false;
        if (data.length < 0x20){
            return;
        }
        if (data[0] == PREAMBLE[0] && (data[1]&0xF0) == (PREAMBLE[1]&0xF0)) {

            toi = ((data[TOI_POSITION] &0xff) <<24 | (data[TOI_POSITION + 1] &0xff)<<16 | (data[TOI_POSITION + 2] &0xff) <<8 | (data[TOI_POSITION + 3]&0xff));
            tsi = ((data[TSI_POSITION] &0xff) <<24 | (data[TSI_POSITION + 1] &0xff)<<16 | (data[TSI_POSITION + 2] &0xff) <<8 | (data[TSI_POSITION + 3]&0xff));
//            if (tsi==0){
//                Log.d(TAG, "TSI =0");
//            }
//            if (data[13]==7){
//                Log.d(TAG, "TOI="+toi);
//            }

            byte length = data[HEADER_LENGTH_POSITION];
            if (length == 8) {
                arrayPosition = ((data[ARRAY_POSITION]&0xff) <<24 | (data[ARRAY_POSITION + 1] &0xff)<<16 | (data[ARRAY_POSITION + 2] &0xff) <<8 | (data[ARRAY_POSITION + 3] & 0xff));
                valid = true;
                if (data[HEADER_EXTENSIONS] == EXFT_64_PREAMBLE[0] && (data[HEADER_EXTENSIONS + 1] & 0xF0) == EXFT_64_PREAMBLE[1]) {
                    instanceId = ((data[INSTANCE_ID_POSITION] & 0x0F) << 16 | (data[INSTANCE_ID_POSITION + 1] & 0xff) << 8 | (data[INSTANCE_ID_POSITION + 2] * 0xff));
                    maxObjectSize =  ((data[MAX_OBJECT_SIZE_POSITION] & 0xff) << 24 | (data[MAX_OBJECT_SIZE_POSITION + 1] & 0xff) << 16 | (data[MAX_OBJECT_SIZE_POSITION + 2] & 0xff) << 8 | (data[MAX_OBJECT_SIZE_POSITION + 3] & 0xff));
                    contentLength =  ((data[TOTAL_FILE_LENGTH] & 0xff) << 24 | (data[TOTAL_FILE_LENGTH + 1] & 0xff) << 16 | (data[TOTAL_FILE_LENGTH + 2] & 0xff) << 8 | (data[TOTAL_FILE_LENGTH + 3] & 0xff));
                    valid=true;

                } else {
                    Log.e(TAG, "Unknown Route preamble: " + data[HEADER_EXTENSIONS] + "  " + (data[HEADER_EXTENSIONS + 1] & 0xF0));
                }

            }else {
                Log.e(TAG,"Unknown Route header length: "+length);
            }

        }else {
            Log.e(TAG,"Unknown Route header preamble: "+data[0]+"  "+data[1]);
        }
    }
}
