package com.sony.tv.app.atsc3receiver1_0.app.LLS;

import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSpec;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Structure for EFDT message
 * Created by xhamc on 3/19/17.
 */

public class EFDT_DATA {

    public String xmlString;

    private final String TSI="TSI";
    private final String IDREF="IDRef";
    private final String EXPIRES="Expires";
    private final String FEC_ENCODING_ID="FEC-OTI-FEC-Encoding-ID";
    private final String FEC_MAX_BLOCK_LENGTH="FEC-OTI-Maximum-Source-Block-Length";
    private final String FEC_SYMBOL_BLOCK_LENGTH="FEC-OTI-Encoding-Symbol-Length";
    private final String TOI="TOI";
    private final String CONTENT_LOCATION="Content-Location";
    private final String CONTENT_LENGTH="Content-Length";

    public int tsi;
    public String idRef;
    public long expires;
    public String encodindId;
    public int maxBlockLength;
    public int symbolLength;
    public int toi;
    public String location;
    public int contentlength;


    public EFDT_DATA(String data){

        this.xmlString=data;

    }

    public boolean parse(String key, String value){
        boolean returnValue=false;
        if (key.equals(TSI)){
            this.tsi=Integer.parseInt(value);
            returnValue=true;

        }else if (key.equals(IDREF)){
            this.idRef=value;
            returnValue=true;

        }else if (key.equals(EXPIRES)){
            this.expires=Long.parseLong(value);
            returnValue=true;

        }else if (key.equals(FEC_ENCODING_ID)){
            this.encodindId=value;
            returnValue=true;

        }else if (key.equals(FEC_MAX_BLOCK_LENGTH)){
            this.maxBlockLength=Integer.parseInt(value);
            returnValue=true;

        }else if (key.equals(FEC_SYMBOL_BLOCK_LENGTH)){
            this.symbolLength=Integer.parseInt(value);
            returnValue=true;

        }else if (key.equals(TOI)){
            this.toi=Integer.parseInt(value);
            returnValue=true;

        }else if (key.equals(CONTENT_LOCATION)){
            Uri uri= Uri.parse(value);
            DataSpec d= new DataSpec(uri);
            this.location=d.uri.getPath();
            returnValue=true;

        }else if (key.equals(CONTENT_LENGTH)){
            this.contentlength=Integer.parseInt(value);
            returnValue=true;

        }
        return returnValue;

    }


}
