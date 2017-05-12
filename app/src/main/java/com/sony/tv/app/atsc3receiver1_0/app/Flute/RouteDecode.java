package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import com.sony.tv.app.atsc3receiver1_0.app.LLS.LLSXmlParse;

/**
 * Created by xhamc on 5/9/17.
 */

public class RouteDecode  {

    private final static String TAG="RouteDecode";
    public final static int NONE=-1;
    public final static int EXT_FTI=0;
    public final static int EXT_FDT=1;
    private final byte[] PREAMBLE=new byte[]{(byte) 0x10, (byte) 0xA0};
    private final byte[] EXT_FTI_PREAMBLE=new byte[]{(byte) 0x40, (byte) 0x00}; // mask last byte with 0xf0
    private final byte[] EXT_FDT_PREAMBLE=new byte[]{(byte) 0xC0, (byte) 0x10}; // mask last byte with 0xf0

    private final int HEADER_LENGTH_POSITION=2;
    private final int TSI_POSITION=8;
    private final int TOI_POSITION=12;
    private final int HEADER_EXTENSIONS =16;
    private final int INSTANCE_ID_POSITION=17;
    private final int TOTAL_FILE_LENGTH=20;
    private final int EXT_FTI_MAX_OBJECT_SIZE_POSITION=24;
    private final int EXT_FDT_MAX_OBJECT_SIZE_POSITION=28;
    private final int EXT_FTI_ARRAY_POSITION=32;
    private final int EXT_FDT_ARRAY_POSITION=36;
    private final int EXT_NONE_ARRAY_POSITION=16;
    public final static int EXT_FTI_PAYLOAD_START_POSITION=36;
    public final static int EXT_NONE_PAYLOAD_START_POSITION=20;
    private final int EFDT_CONTENT_START_POSITION=40;
    private final int EXPIRY_POSITION=24;


    private boolean valid=false;
    private int tsi;
    private int toi;
    private int instanceId;
    private int maxObjectSize;
    private int arrayPosition;
    private long expiry;
    private String fileName="";
    private int contentLength;
    private int efdt_toi;
    private int type;

    public void fileName(String fileName){
        this.fileName=fileName;
    }

    public int tsi(){return tsi;}
    public int arrayPosition(){return arrayPosition;}
    public int toi(){return toi;}
    public String fileName(){return fileName;}
    public int contentLength(){return contentLength;}
    public int efdt_toi(){ return efdt_toi;}
    public boolean valid(){return valid;}
    public int type(){return type;}

    public RouteDecode(byte[] data, int packetSize) {
        valid=false;
        if (data.length < 0x20){
            return;
        }
        if ( (data[0]&0xF0) == PREAMBLE[0] && (data[1]&0xF0) == (PREAMBLE[1]&0xF0)) {

            toi = ((data[TOI_POSITION] &0xff) <<24 | (data[TOI_POSITION + 1] &0xff)<<16 | (data[TOI_POSITION + 2] &0xff) <<8 | (data[TOI_POSITION + 3]&0xff));
            tsi = ((data[TSI_POSITION] &0xff) <<24 | (data[TSI_POSITION + 1] &0xff)<<16 | (data[TSI_POSITION + 2] &0xff) <<8 | (data[TSI_POSITION + 3]&0xff));


            byte length = data[HEADER_LENGTH_POSITION];
            if (length == 4) {
                type=NONE;
                arrayPosition = ((data[EXT_FDT_ARRAY_POSITION]&0xff) <<24 | (data[EXT_FDT_ARRAY_POSITION + 1] &0xff)<<16 | (data[EXT_FDT_ARRAY_POSITION + 2] &0xff) <<8 | (data[EXT_FDT_ARRAY_POSITION + 3] & 0xff));
                valid = true;
            } else if (length==9 && toi==0) {

                if (data[HEADER_EXTENSIONS] == EXT_FDT_PREAMBLE[0] && (data[HEADER_EXTENSIONS + 1] & 0xF0) == EXT_FDT_PREAMBLE[1]) {
                    type=EXT_FDT;
                    instanceId = ((data[INSTANCE_ID_POSITION] & 0x0F) << 16 | (data[INSTANCE_ID_POSITION + 1] & 0xff) << 8 | (data[INSTANCE_ID_POSITION + 2] * 0xff));
                    expiry = System.currentTimeMillis() + ((data[EXPIRY_POSITION] & 0xff) << 24 | (data[EXPIRY_POSITION + 1] & 0xff) << 16 | (data[EXPIRY_POSITION + 2] & 0xff) << 8 | (data[EXPIRY_POSITION + 3] & 0xff)) * 1000;
                    maxObjectSize = ((data[EXT_FDT_MAX_OBJECT_SIZE_POSITION] & 0xff) << 24 | (data[EXT_FDT_MAX_OBJECT_SIZE_POSITION + 1] & 0xff) << 16 | (data[EXT_FDT_MAX_OBJECT_SIZE_POSITION + 2] & 0xff) << 8 | (data[EXT_FDT_MAX_OBJECT_SIZE_POSITION + 3] & 0xff));
                    String s = new String(data, EFDT_CONTENT_START_POSITION, packetSize - EFDT_CONTENT_START_POSITION);
                    EFDT_DATA e = (new EFDTXmlParse(s)).EFDTParse();
                    fileName = e.location;
                    contentLength = e.contentlength;
                    efdt_toi = e.toi;
                    valid=true;
                } else {
                    Log.e(TAG, "Unknown Route preamble: " + data[HEADER_EXTENSIONS] + "  " + (data[HEADER_EXTENSIONS + 1] & 0xF0));
                }

            }else if (length == 8) {
                type=EXT_FTI;
                if (data[HEADER_EXTENSIONS] == EXT_FTI_PREAMBLE[0] && (data[HEADER_EXTENSIONS + 1] & 0xF0) == EXT_FTI_PREAMBLE[1]) {
                    arrayPosition = ((data[EXT_FTI_ARRAY_POSITION]&0xff) <<24 | (data[EXT_FTI_ARRAY_POSITION + 1] &0xff)<<16 | (data[EXT_FTI_ARRAY_POSITION + 2] &0xff) <<8 | (data[EXT_FTI_ARRAY_POSITION + 3] & 0xff));
                    instanceId = ((data[INSTANCE_ID_POSITION] & 0x0F) << 16 | (data[INSTANCE_ID_POSITION + 1] & 0xff) << 8 | (data[INSTANCE_ID_POSITION + 2] * 0xff));
                    maxObjectSize =  ((data[EXT_FTI_MAX_OBJECT_SIZE_POSITION] & 0xff) << 24 | (data[EXT_FTI_MAX_OBJECT_SIZE_POSITION + 1] & 0xff) << 16 | (data[EXT_FTI_MAX_OBJECT_SIZE_POSITION + 2] & 0xff) << 8 | (data[EXT_FTI_MAX_OBJECT_SIZE_POSITION + 3] & 0xff));
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
