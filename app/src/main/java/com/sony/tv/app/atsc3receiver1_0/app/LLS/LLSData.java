package com.sony.tv.app.atsc3receiver1_0.app.LLS;

import android.util.Log;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by xhamc on 3/15/17.
 */

public class LLSData {


    public HashMap<String,Method> hashMap;
    public SLTData mSLTData;
    public STData mSTData;
    public String type;
    public String xmlString;

    public LLSData(String type, String data){

        hashMap=new HashMap<>();
        try {
            hashMap.put("xmlns", LLSData.class.getMethod("putXmlns", String.class));
            hashMap.put("bsid", LLSData.class.getMethod("putBsid",  String.class));
            hashMap.put("SLTCapabilities", LLSData.class.getMethod("putCapabilities", String.class));
            hashMap.put("SLTInetUrl", LLSData.class.getMethod("putSLTInetUrl",String.class));
            hashMap.put("serviceId", LLSData.class.getMethod("putServiceId",int.class, String.class ));
            hashMap.put("sltSvcSeqNum", LLSData.class.getMethod("putSltSvcSeqNum",int.class, String.class ));
            hashMap.put("protected", LLSData.class.getMethod("putProtected",int.class, String.class ));
            hashMap.put("majorChannelNo", LLSData.class.getMethod("putMajorChannelNo", int.class, String.class ));
            hashMap.put("minorChannelNo", LLSData.class.getMethod("putMinorChannelNo",int.class, String.class ));
            hashMap.put("serviceCategory", LLSData.class.getMethod("putServiceCategory",int.class, String.class ));
            hashMap.put("shortServiceName", LLSData.class.getMethod("putShortServiceName",int.class, String.class ));
            hashMap.put("slsProtocol", LLSData.class.getMethod("putSlsProtocol",int.class, int.class, String.class ));
            hashMap.put("slsPlpId", LLSData.class.getMethod("putSlsPlpId",int.class, int.class, String.class ));
            hashMap.put("slsMajorProtocolVersion", LLSData.class.getMethod("putSlsMajorProtocolVersion",int.class, int.class, String.class ));
            hashMap.put("slsMinorProtocolVersion", LLSData.class.getMethod("putSlsMinorProtocolVersion",int.class, int.class, String.class ));
            hashMap.put("slsDestinationIpAddress", LLSData.class.getMethod("putSlsDestinationIpAddress",int.class, int.class, String.class ));
            hashMap.put("slsSourceIpAddress", LLSData.class.getMethod("putSlsSourceIpAddress",int.class, int.class, String.class ));
            hashMap.put("slsDestinationUdpPort", LLSData.class.getMethod("putSlsDestinationUdpPort",int.class, int.class, String.class ));
            hashMap.put("currentUtcOffset", LLSData.class.getMethod("putCurrentUtcOffset",  String.class));
            hashMap.put("dsDayOfMonth", LLSData.class.getMethod("putDsDayOfMonth",  String.class));
            hashMap.put("dsHour", LLSData.class.getMethod("putDsHour",  String.class));
            hashMap.put("dsStatus", LLSData.class.getMethod("putDsStatus",  String.class));
            hashMap.put("leap59", LLSData.class.getMethod("putLeap59",  String.class));
            hashMap.put("leap61", LLSData.class.getMethod("putLeap61",  String.class));
            hashMap.put("ptpPrepend", LLSData.class.getMethod("putPrepend",  String.class));
            hashMap.put("utcLocalOffset", LLSData.class.getMethod("putLocalOffset",  String.class));
            hashMap.put("diffTime", LLSData.class.getMethod("putDiffTime",  Long.class));


        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        if (type.equals(LLSXmlParse.SLTTAG)){
            mSLTData=new SLTData();
        }else if (type.equals(LLSXmlParse.SYSTEMTIMETAG)){
            mSTData=new STData();
        }/*TODO add other types*/
        this.type=type;
        this.xmlString=data;
    }



    public void putXmlns(String value){
        if (type.equals("SLT"))
            mSLTData.xmlns=value;
        else if (type.equals(LLSXmlParse.SYSTEMTIMETAG)){
            mSTData.xmlns=value;
        }
    }
    public void putBsid(String value){ mSLTData.bsid=Integer.parseInt(value);}
    public void putSLTInetUrl(String value){ mSLTData.sLTInetUrl=URI.create(value);}
    public void putCapabilities(String value){ mSLTData.capabilities=value;}
    public void putServiceId(int item, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        s.serviceID=Short.parseShort(value);
    }
    public void putSltSvcSeqNum(int item, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        s.sltSvcSeqNum=Byte.parseByte(value);
    }
    public void putProtected(int item, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        s.mProtected=Boolean.parseBoolean(value);
    }
    public void putMajorChannelNo(int item, String value){
        SLTData.Service s;
        if (item> mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        s.majorChannelNo=Short.parseShort(value);
    }
    public void putMinorChannelNo(int item, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        s.minorChannelNo=Short.parseShort(value);
    }
    public void putServiceCategory(int item, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        s.serviceCategory=Byte.parseByte(value);
    }
    public void putShortServiceName(int item, String value){
        Log.d("***","size of item: "+ item+"  size of mServices: "+mSLTData.mServices.size());

        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        s.shortServiceName=value;
    }
    public void putSlsProtocol(int item, int item2, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        SLTData.Service.BroadcastService b;
        if (item2>s.broadcastServices.size()) b=s.addBroadcastService(item2-1); else b=s.broadcastServices.get(item2-1);
        b.slsProtocol=Byte.parseByte(value);
    }

    public void putSlsPlpId(int item, int item2, String value){

    }

    public void putSlsMajorProtocolVersion(int item, int item2, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        SLTData.Service.BroadcastService b;
        if (item2>s.broadcastServices.size()) b=s.addBroadcastService(item2-1); 
        else b=s.broadcastServices.get(item2-1);
        b.slsMajorProtocolVersion=Byte.parseByte(value);
    }
    public void putSlsMinorProtocolVersion(int item, int item2, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        SLTData.Service.BroadcastService b;
        if (item2>s.broadcastServices.size()) b=s.addBroadcastService(item2-1); else b=s.broadcastServices.get(item2-1);
        b.slsMinorProtocolVersion=Byte.parseByte(value);
    }
    public void putSlsDestinationIpAddress(int item, int item2, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        SLTData.Service.BroadcastService b;
        if (item2>s.broadcastServices.size()) b=s.addBroadcastService(item2-1); else b=s.broadcastServices.get(item2-1);
        b.slsDestinationIpAddress=value;
    }

    public void putSlsSourceIpAddress(int item, int item2, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        SLTData.Service.BroadcastService b;
        if (item2>s.broadcastServices.size()) b=s.addBroadcastService(item2-1); else b=s.broadcastServices.get(item2-1);
        b.slsSourceIpAddress=URI.create(value);
    }

    public void putSlsDestinationUdpPort(int item, int item2, String value){
        SLTData.Service s;
        if (item>mSLTData.mServices.size()) s=mSLTData.addService(item-1); else s=mSLTData.mServices.get(item-1);
        SLTData.Service.BroadcastService b;
        if (item2>s.broadcastServices.size()) b=s.addBroadcastService(item2-1); else b=s.broadcastServices.get(item2-1);
        b.slsDestinationUdpPort=value;
    }


    public class SLTData{
        public String xmlns;
        public int bsid;
        public String capabilities;
        public URI sLTInetUrl;
        public ArrayList<Service> mServices;
        public SLTData(){
            mServices=new ArrayList<Service>();
        }
        public Service addService(int item){
            Service s=new Service();
            mServices.add(item, s);
            return s;
        }
        public class Service{
            public ArrayList<BroadcastService> broadcastServices;
            public short serviceID;
            public byte sltSvcSeqNum;
            public boolean mProtected;
            public short majorChannelNo;
            public short minorChannelNo;
            public byte serviceCategory;
            public String shortServiceName;
            public Service(){
                broadcastServices=new ArrayList<BroadcastService>();
            }
            public BroadcastService addBroadcastService(int item){
                BroadcastService b=new BroadcastService();
                broadcastServices.add(item,b);
                return b;
            }
            public class BroadcastService{
                public byte slsProtocol;
                public byte slsMajorProtocolVersion;
                public byte slsMinorProtocolVersion;
                public String slsDestinationIpAddress;
                public String slsDestinationUdpPort;
                public URI slsSourceIpAddress;
                public BroadcastService(){
                }
            }
        }
    }

    public void putCurrentUtcOffset(String value) {mSTData.currentUtcOffset=Integer.parseInt(value);} /*TODO spec says byte bt tets config gives 28800*/
    public void putDsDayOfMonth(String value){mSTData.dsDaysOfMonth=Byte.parseByte(value);}
    public void putDsHour(String value){mSTData.dsHour=Byte.parseByte(value);}
    public void putDsStatus(String value){mSTData.dcStatus=Boolean.parseBoolean(value);}
    public void putLeap59(String value){mSTData.leap59=Boolean.parseBoolean(value);}
    public void putLeap61(String value){mSTData.leap61=Boolean.parseBoolean(value);}
    public void putPrepend(String value){mSTData.ptpPrepend=Long.parseLong(value);}
    public void putLocalOffset(String value){mSTData.utcLocalOffset=value;}
    public void putDiffTime(Long value){mSTData.diffTime=value;}
    public long getPtpPrepend(){return mSTData.ptpPrepend;}


    public class STData {
        public String xmlns;
        public int currentUtcOffset;
        public long ptpPrepend;
        public boolean leap59;
        public boolean leap61;
        public String utcLocalOffset;
        public boolean dcStatus;
        public byte dsDaysOfMonth;
        public byte dsHour;

        /*custom field sender - receiver time in ms:
        */
        public long diffTime;
    }


}
