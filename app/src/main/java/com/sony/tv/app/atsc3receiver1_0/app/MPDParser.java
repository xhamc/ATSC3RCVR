package com.sony.tv.app.atsc3receiver1_0.app;

import android.provider.Settings;
import android.util.Log;

import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.util.SystemClock;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by xhamc on 3/24/17.
 */

public class MPDParser {

    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    private static final String TAG="MPDParser)";

    public MPD mpd;
    public StringBuilder stringBuilder=new StringBuilder(10000);
    private HashMap<String, ContentFileLocation> videoMap;
    private HashMap<String, ContentFileLocation> audioMap;
    private String data;


    public MPDParser(String data, HashMap<String, ContentFileLocation> videoMap, HashMap<String, ContentFileLocation> audioMap){
        this.videoMap=videoMap;
        this.audioMap=audioMap;
        this.data=data;
        stringBuilder.append("<?xml version=\"1.0\"?>\n");
        long now=System.currentTimeMillis();
        this.mpd=new MPD(stringBuilder);
        Log.d(TAG,"Time to parse MPD: "+(System.currentTimeMillis()-now));
    }



    public boolean MPDParse()
    {
        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(data);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            while (eventType!=XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {

                } else if(eventType == XmlPullParser.START_TAG) {
                    mpd.addTag(xpp);
                } else if(eventType == XmlPullParser.END_TAG) {
                    mpd.endTag(xpp);
                } else if(eventType == XmlPullParser.TEXT) {
                    mpd.addText(xpp);
                }else{

                }
                eventType = xpp.next();
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public StringBuilder toStringBuilder(){
        return mpd.toStringBuilder ();
    }


}
