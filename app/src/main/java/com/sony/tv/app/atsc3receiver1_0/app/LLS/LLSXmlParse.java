package com.sony.tv.app.atsc3receiver1_0.app.LLS;

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

public class LLSXmlParse {
    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    private static final String TAG="XML";
    private LLSData llsData;

    public static final String SLTTAG="SLT";
    public static final String SYSTEMTIMETAG="SystemTime";
    public static final String EFDT_INSTANCE_TAG="EFDT-Instance";

    String type;
    String data;

    public LLSXmlParse(final String data, final String type){
        if (type.equals(SLTTAG) || type.equals(SYSTEMTIMETAG)) {
            llsData = new LLSData(type, data);
        }
        this.type=type;
        this.data=data;
    }

    public LLSData LLSParse(){
        int tagLevel=-1;
        int tagLevel1=0;
        int [] tagLevel2=new int[10];

        boolean foundStartTag =false;

        try {
            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            StringReader s=new StringReader(data);
            xpp.setInput(s);
            int eventType = xpp.getEventType();
            while (eventType!=XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_DOCUMENT) {

                } else if(eventType == XmlPullParser.START_TAG) {
//                    Log.d(TAG,"Start Tag"+xpp.getName());
                    tagLevel++;
                    if (tagLevel==1) tagLevel1++;
                    if (tagLevel==2) tagLevel2[tagLevel1-1]++;
//                    Log.d(TAG,"taglevel: "+ tagLevel+ "serviceItems "+ tagLevel1 + "  broadcastItems[0]:  "+ tagLevel2[0] );
                    if (type.equals(xpp.getName())){
                        foundStartTag=true;
                        for (int i=0; i<xpp.getAttributeCount(); i++){
//                            Log.d(TAG,"attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));
                            try {

                                llsData.hashMap.get(xpp.getAttributeName(i)).invoke(llsData, xpp.getAttributeValue(i));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                            if (xpp.getAttributeName(i).equals("ptpPrepend")) {
                                long senderTime = Long.parseLong(xpp.getAttributeValue(i)) / 1000;
                                long receiverTime = System.currentTimeMillis();
//                                Log.d(TAG, "sTime: " + senderTime + "rTime: " + receiverTime + "diffTime: " + (senderTime - receiverTime));
                                long diffTime=(senderTime-receiverTime);
                                llsData.hashMap.get("diffTime").invoke(llsData, diffTime);
                            }

                        }
                    }else if (foundStartTag) {
                        try {
                            if (tagLevel == 1) {
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
//                                    Log.d(TAG,"attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));

                                    llsData.hashMap.get(xpp.getAttributeName(i)).invoke(llsData, tagLevel1, xpp.getAttributeValue(i));
                                }
                            } else if (tagLevel == 2) { //*****currently allowing for XML two levels only
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
//                                    Log.d(TAG,"attribute: "+xpp.getAttributeName(i)+"="+xpp.getAttributeValue(i));

                                    llsData.hashMap.get(xpp.getAttributeName(i)).invoke(llsData, tagLevel1, tagLevel2[tagLevel1-1], xpp.getAttributeValue(i));
                                }
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
//                    Log.d(TAG,"End tag "+xpp.getName());
                    tagLevel--;
//                    Log.d(TAG,"taglevel: "+ tagLevel+ "serviceItems "+ tagLevel1 + "  broadcastItems[0]:  "+ tagLevel2[0] );



                } else if(eventType == XmlPullParser.TEXT) {
                    if (!
                            xpp.getText().trim().equals("")) {
//                        Log.d(TAG, "Text " + xpp.getText());
                    }
                }else{


                }
                eventType = xpp.next();
            }

//
        }catch (Exception e){
            e.printStackTrace();
        }

        return llsData;
    }





}
