package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
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
    private String data;


    public MPDParser(String data){
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
