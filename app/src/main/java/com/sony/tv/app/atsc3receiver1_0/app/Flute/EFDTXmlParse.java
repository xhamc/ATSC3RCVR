package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import com.sony.tv.app.atsc3receiver1_0.app.LLS.EFDT_DATA;
import com.sony.tv.app.atsc3receiver1_0.app.LLS.LLSData;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by xhamc on 3/14/17.
 * This class parses the LLS message body into SLT object and ST Object
 * using Reflection (invoke) to match the xml attribute to the relevant class method
 * or:
 * Parses the EFDT header
 *
 */

public class EFDTXmlParse {
    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    private static final String TAG="XML";
    private LLSData llsData;
    private EFDT_DATA efdtData;

    public static final String SLTTAG="SLT";
    public static final String SYSTEMTIMETAG="SystemTime";
    public static final String EFDT_INSTANCE_TAG="EFDT-Instance";

    String type;
    String data;

    public EFDTXmlParse(final String data, final String type){
        if (type.equals(SLTTAG) || type.equals(SYSTEMTIMETAG)) {
            llsData = new LLSData(type, data);
        }else if (type.equals(EFDT_INSTANCE_TAG)){
            efdtData=new EFDT_DATA(data);
        }
        this.type=type;
        this.data=data;
    }


    public EFDT_DATA EFDTParse(){

        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(data);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            while (eventType!=XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
//                    Log.v(TAG,"Start Tag"+xpp.getName());
                        for (int i=0; i<xpp.getAttributeCount(); i++){
                            if (efdtData.parse(xpp.getAttributeName(i), xpp.getAttributeValue(i))){
//                                Log.v(TAG,"attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));
                            }else {
//                                Log.v(TAG,"TAG NOT FOUND attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));
                            }
                        }

                } else if(eventType == XmlPullParser.END_TAG) {
//                    Log.v(TAG,"End tag "+xpp.getName());
                } else if(eventType == XmlPullParser.TEXT) {
                    if (!
                            xpp.getText().trim().equals("")) {
//                        Log.v(TAG, "Text " + xpp.getText());
                    }
                }else{


                }
                eventType = xpp.next();
            }

//
        }catch (Exception e){
            e.printStackTrace();
        }

        return efdtData;
    }



}
