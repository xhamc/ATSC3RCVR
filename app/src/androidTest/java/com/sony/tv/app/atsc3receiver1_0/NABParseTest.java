package com.sony.tv.app.atsc3receiver1_0;

import android.test.InstrumentationTestCase;

import com.sony.tv.app.atsc3receiver1_0.app.Flute.STSIDParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by xhamc on 4/3/17.
 */

public class NABParseTest extends InstrumentationTestCase {

    private String manifest="";
    private String usbd="";
    private String stsid="";



    public void testSLSLDecode() throws IOException {
        InputStream open = getInstrumentation().getContext().getResources().getAssets().open("multipart.xml");
        byte[] source=new byte[5000];
        int len=open.read(source,0,5000);
        String sls=new String(source, 0, source.length);
        boolean man=extractManifest(sls);
        boolean us=extractUSBD(sls);
        boolean st=extractSTSID(sls);

        assertTrue (manifest, man);
        assertTrue (usbd, us);
        assertTrue (stsid, st);

    }

    private boolean extractManifest(String sls){
        if (sls.contains("<MPD") && sls.contains("/MPD>")){
            int start=sls.indexOf("<MPD");
            int end=sls.indexOf("/MPD>")+5;
            manifest= sls.subSequence(start,end).toString();
            return true;
        }
        return false;
    }
    private boolean extractUSBD (String sls){
        if (sls.contains("<bundleDescriptionROUTE") && sls.contains("/bundleDescriptionROUTE>")){
            int start=sls.indexOf("<bundleDescriptionROUTE");
            int end=sls.indexOf("/bundleDescriptionROUTE>")+24;
            usbd= sls.subSequence(start,end).toString();
            return true;
        }
        return false;


    }
    private boolean extractSTSID(String sls){
        if (sls.contains("<S-TSID") && sls.contains("/S-TSID>")){
            int start=sls.indexOf("<S-TSID");
            int end=sls.indexOf("/S-TSID>")+8;
            stsid= sls.subSequence(start,end).toString();
            STSIDParser stsidObj=new STSIDParser(stsid);
            stsidObj.STSIDParse();

            return true;
        }
        return false;
    }



}
