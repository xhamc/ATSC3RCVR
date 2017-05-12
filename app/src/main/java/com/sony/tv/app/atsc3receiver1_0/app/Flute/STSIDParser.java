package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by xhamc on 4/3/17.
 */

public class STSIDParser {

    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    private static final String TAG="MPDParser)";

    public STSID stsid;
    public StringBuilder stringBuilder=new StringBuilder(10000);
    private HashMap<String, ContentFileLocation> videoMap;
    private HashMap<String, ContentFileLocation> audioMap;
    private String data;


    public STSIDParser(String data){

        this.data=data;
        stsid=new STSID();
        STSIDParse();
    }
    public int getLSSize(){
        return stsid.rs.get(0).ls.size();
    }
    public int getTSI(int index){
        return Integer.parseInt(stsid.rs.get(0).ls.get(index).attrs.get("tsi"));
    }
    public long getBandwidth(int index){
        return Long.parseLong( stsid.rs.get(0).ls.get(index).attrs.get("bw"));
    }
    public String getFileTempate(int index){
        if (null!=stsid && null!=stsid.rs && null!=stsid.rs.get(0) && null !=stsid.rs.get(0).ls && null!=stsid.rs.get(0).ls.get(index)
                && null!=stsid.rs.get(0).ls.get(index).srcFlow && null!=stsid.rs.get(0).ls.get(index).srcFlow.efdt && null!=stsid.rs.get(0).ls.get(index).srcFlow.efdt.fileTemplate )
        return stsid.rs.get(0).ls.get(index).srcFlow.efdt.fileTemplate.text;
        else
            return "";
    }
    public String getFileInitTempate(int index){
        if (null!=stsid && null!=stsid.rs && null!=stsid.rs.get(0) && null !=stsid.rs.get(0).ls && null!=stsid.rs.get(0).ls.get(index)
                && null!=stsid.rs.get(0).ls.get(index).srcFlow && null!=stsid.rs.get(0).ls.get(index).srcFlow.efdt && null!=stsid.rs.get(0).ls.get(index).srcFlow.efdt.fdtParameters
                &&  null!=stsid.rs.get(0).ls.get(index).srcFlow.efdt.fdtParameters.file &&  null!=stsid.rs.get(0).ls.get(index).srcFlow.efdt.fdtParameters.file.attrs)
        return stsid.rs.get(0).ls.get(index).srcFlow.efdt.fdtParameters.file.attrs.get("Content-Location");
        else
            return "";
    }
    public int getFileInitTOI(int index){
        return Integer.parseInt(stsid.rs.get(0).ls.get(index).srcFlow.efdt.fdtParameters.file.attrs.get("TOI"));
    }
    public String getdIpAddr(){
        return stsid.rs.get(0).attrs.get("dIpAddr");
    }

    public String getdPort(){
        return stsid.rs.get(0).attrs.get("dport");
    }
    public boolean STSIDParse()
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
                    stsid.addTag(xpp);
                } else if(eventType == XmlPullParser.END_TAG) {
                    //stsid.endTag(xpp);
                } else if(eventType == XmlPullParser.TEXT) {
                    stsid.addText(xpp);
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

    /*
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:routesls="http://www.atsc.org/XMLSchemas/ATSC3/Delivery/ROUTESLS/1.0/" xmlns:fdt="urn:ietf:params:xml:ns:fdt" targetNamespace="http://www.atsc.org/XMLSchemas/ATSC3/Delivery/ROUTESLS/1.0/" elementFormDefault="qualified">
	<xs:import namespace="http://www.w3.org/XML/1998/namespace" schemaLocation="http://www.w3.org/2001/xml.xsd"/>
	<xs:annotation>
		<xs:documentation>
			11/23/15 - included in S33-1-479r2
		</xs:documentation>
	</xs:annotation>
	<xs:element name="S-TSID" type="routesls:STSIDType"/>
	<xs:complexType name="STSIDType">
		<xs:sequence>
			<xs:element name="RS" type="routesls:rSessionType" minOccurs="1" maxOccurs="unbounded"/>
		</xs:sequence>
		<xs:attribute name="serviceId" type="xs:unsignedShort" use="required"/>
	</xs:complexType>
	<xs:complexType name="rSessionType">
		<xs:sequence>
			<xs:element name="LS" type="routesls:lSessionType" minOccurs="1" maxOccurs="unbounded"/>
		</xs:sequence>
		<xs:attribute name="bsid" type="xs:unsignedShort" use="optional"/>
		<xs:attribute name="sIpAddr" type="routesls:AddressType" use="optional"/>
		<xs:attribute name="dIpAddr" type="routesls:AddressType" use="optional"/>
		<xs:attribute name="dPort" type="routesls:PortType" use="optional"/>
		<xs:attribute name="PLPID" type="routesls:PLPIdType" use="optional"/>
	</xs:complexType>
	<xs:complexType name="lSessionType">
		<xs:sequence>
			<xs:element name="SrcFlow" type="routesls:srcFlowType" minOccurs="0" maxOccurs="1"/>
			<xs:element name="RepairFlow" type="routesls:rprFlowType" minOccurs="0" maxOccurs="1"/>
		</xs:sequence>
		<xs:attribute name="tsi" type="xs:unsignedInt" use="required"/>
		<xs:attribute name="PLPID" type="routesls:PLPIdType" use="optional"/>
		<xs:attribute name="bw" type="xs:unsignedInt" use="optional"/>
		<xs:attribute name="startTime" type="xs:dateTime" use="optional"/>
		<xs:attribute name="endTime" type="xs:dateTime" use="optional"/>
	</xs:complexType>
	<xs:simpleType name="PLPIdType">
		<xs:restriction base="xs:unsignedByte">
			<xs:maxInclusive value="63"/>
		</xs:restriction>
	</xs:simpleType>
	<xs:simpleType name="AddressType">
		<xs:restriction base="xs:token">
			<xs:pattern value="(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])"/>
		</xs:restriction>
	</xs:simpleType>
	<xs:simpleType name="PortType">
		<xs:restriction base="xs:unsignedShort">
			<xs:minInclusive value="1"/>
		</xs:restriction>
	</xs:simpleType>
	<xs:complexType name="srcFlowType">
		<xs:sequence>
			<xs:element name="EFDT" type="routesls:efdtType" minOccurs="0" maxOccurs="1"/>
			<xs:element name="ContentInfo" type="xs:string" minOccurs="0" maxOccurs="1"/>
			<xs:element name="Payload" type="routesls:payloadType" minOccurs="0" maxOccurs="1"/>
		</xs:sequence>
		<xs:attribute name="rt" type="xs:boolean" use="optional" default="false"/>
		<xs:attribute name="minBuffSize" type="xs:unsignedInt" use="optional"/>
	</xs:complexType>
	<xs:complexType name="payloadType">
		<xs:sequence>
			<xs:element name="FECParams" type="routesls:fecParamType" minOccurs="0" maxOccurs="1"/>
		</xs:sequence>
		<xs:attribute name="codePoint" type="xs:unsignedByte" use="optional" default="0"/>
		<xs:attribute name="formatID" type="xs:unsignedByte" use="required"/>
		<xs:attribute name="frag" type="xs:unsignedByte" use="optional" default="0"/>
		<xs:attribute name="order" type="xs:unsignedByte" use="optional" default="0"/>
		<xs:attribute name="srcFecPayloadID" type="xs:unsignedByte" use="optional" default="1"/>
	</xs:complexType>
	<xs:complexType name="rprFlowType">
		<xs:sequence>
			<xs:element name="FECParameters" type="routesls:fecParametersType" minOccurs="0" maxOccurs="1"/>
		</xs:sequence>
	</xs:complexType>
	<xs:complexType name="fecParametersType">
		<xs:sequence>
			<xs:element name="FECOTI" type="routesls:fecParamType" minOccurs="1" maxOccurs="1"/>
			<xs:element name="ProtectedObject" type="routesls:protectedObjectType" minOccurs="0" maxOccurs="unbounded"/>
		</xs:sequence>
		<xs:attribute name="maximumDelay" type="xs:unsignedInt" use="optional" default="0"/>
		<xs:attribute name="overhead" type="routesls:percentageType" use="optional"/>
		<xs:attribute name="minBuffSize" type="xs:unsignedInt" use="optional"/>
	</xs:complexType>
	<xs:simpleType name="percentageType">
		<xs:restriction base="xs:unsignedShort">
			<xs:maxInclusive value="1000"/>
		</xs:restriction>
	</xs:simpleType>
	<xs:complexType name="protectedObjectType">
		<xs:attribute name="sessionDescription" type="xs:string" use="optional"/>
		<xs:attribute name="tsi" type="xs:unsignedInt" use="required"/>
		<xs:attribute name="sourceTOI" type="xs:string" use="optional"/>
		<xs:attribute name="fedTransportObjectSize" type="xs:unsignedInt" use="optional"/>
	</xs:complexType>
	<xs:complexType name="efdtType">
		<xs:sequence>
			<xs:element name="FileTemplate" type="xs:string" minOccurs="0" maxOccurs="1"/>
			<xs:element name="FDTParameters" type="fdt:FDT-InstanceType" minOccurs="0" maxOccurs="1"/>
		</xs:sequence>
		<xs:attribute name="tsi" type="xs:unsignedInt" use="optional"/>
		<xs:attribute name="idRef" type="xs:anyURI" use="optional"/>
		<xs:attribute name="version" type="xs:unsignedInt" use="optional"/>
		<xs:attribute name="maxExpiresDelta" type="xs:unsignedInt" use="optional"/>
		<xs:attribute name="maxTransportSize" type="xs:unsignedInt" use="optional"/>
	</xs:complexType>
	<xs:complexType name="fecParamType">
		<xs:simpleContent>
			<xs:extension base="xs:string"/>
		</xs:simpleContent>
	</xs:complexType>
</xs:schema>
*/



    private class STSID {
        public HashMap<String, String> attrs=new HashMap<>();
        public ArrayList<RS> rs=new ArrayList<>();
        int currentRS=0;
        int currentLS=0;

        private final static String tag_RS = "RS";
        private final static String tag_LS = "LS";
        private final static String tag_SrcFlow = "SrcFlow";
        private final static String tag_RepairFlow = "RepairFlow";
        private final static String tag_EFDT = "EFDT";
        private final static String tag_ContentInfo = "ContentInfo";
        private final static String tag_Payload = "Payload";
        private final static String tag_FECParams = "FECParams";
        private final static String tag_FECOTI = "FECOTI";
        private final static String tag_ProtectedObject = "ProtectedObject";
        private final static String tag_FileTemplate = "FileTemplate";
        private final static String tag_FDTParameters = "FDTParameters";
        private final static String tag_File = "File";

        private Object textObject;


        public void addTag(XmlPullParser xpp){
            String tag=xpp.getName();
            if (tag.equals(tag_RS)){
                this.rs.add(new RS(xpp));
                currentLS=0;
                currentRS++;
            }else if (tag.equals(tag_LS)){
                this.rs.get(currentRS-1).addTag(new LS(xpp));
                currentLS++;
            }else if (tag.equals(tag_SrcFlow)){
                this.rs.get(currentRS-1).ls.get(currentLS-1).addTag(new SrcFlow(xpp));
            }else if (tag.equals(tag_RepairFlow)){
            }else if (tag.equals(tag_EFDT)){
                this.rs.get(currentRS-1).ls.get(currentLS-1).srcFlow.addTag(new EFDT(xpp));
            }else if (tag.equals(tag_ContentInfo)){
                this.rs.get(currentRS-1).ls.get(currentLS-1).srcFlow.addTag(new ContentInfo(xpp));
            }else if (tag.equals(tag_Payload)){
                this.rs.get(currentRS-1).ls.get(currentLS-1).srcFlow.addTag(new Payload(xpp));
            }else if (tag.equals(tag_FECParams)){

            }else if (tag.equals(tag_FECOTI)){

            }else if (tag.equals(tag_ProtectedObject)){

            }else if (tag.equals(tag_FileTemplate)){
                textObject=this.rs.get(currentRS-1).ls.get(currentLS-1).srcFlow.efdt.addTag(new FileTemplate(xpp));
            }else if (tag.equals(tag_FDTParameters)){
                this.rs.get(currentRS-1).ls.get(currentLS-1).srcFlow.efdt.addTag(new FDTParameters(xpp));

            }else if (tag.equals(tag_File)){
                this.rs.get(currentRS-1).ls.get(currentLS-1).srcFlow.efdt.fdtParameters.addTag(new File(xpp));

            }

        }

        public void addText(XmlPullParser xpp){
            if (null==textObject){
                return;
            }
            String text=xpp.getText();
            try {
                textObject.getClass().getMethod("set",XmlPullParser.class).invoke(textObject,xpp);
                textObject=null;
            } catch (NoSuchMethodException e) {
                Log.e(TAG,"Writing text to a method that doesn't exist");
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private class RS{
            //attributes: @serviceId
            public HashMap<String, String> attrs=new HashMap<>();
            public ArrayList<LS> ls=new ArrayList<>();
            public RS(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public void addTag(LS ls) {
                this.ls.add(ls);
            }

        }
        private class LS{
            public HashMap<String, String> attrs=new HashMap<>();

            public SrcFlow srcFlow;

            //            private RepairFlow repairFlow;
            public LS(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public void addTag(SrcFlow s) {
                srcFlow=s;
            }

//            public void addTag(RepairFlow r) {
//                repairFlow=r;
//            }
        }
        private class SrcFlow{
            public HashMap<String, String> attrs=new HashMap<>();
            public EFDT efdt;
            public ContentInfo contentInfo;
            public Payload payload;

            public SrcFlow(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public void addTag(EFDT efdt) {
                this.efdt=efdt;
            }
            public void addTag(ContentInfo c) {
                contentInfo=c;
            }
            public void addTag(Payload p) {
                payload=p;
            }

        }

        private class EFDT{
            public HashMap<String, String> attrs=new HashMap<>();
            public FileTemplate fileTemplate;
            public FDTParameters fdtParameters;
            public EFDT(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public Object addTag(FileTemplate f) {
                fileTemplate=f;
                return this.fileTemplate;
            }
            public void addTag(FDTParameters f) {
                fdtParameters=f;
            }

        }
        private class ContentInfo{
            public HashMap<String, String> attrs=new HashMap<>();
            public ContentInfo(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public void addTag(XmlPullParser xpp) {
            }

        }
        private class Payload{
            public HashMap<String, String> attrs=new HashMap<>();
            public Payload(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public void addTag(XmlPullParser xpp) {
            }

        }

        private class FileTemplate{
            public HashMap<String, String> attrs=new HashMap<>();
            public FDTParameters fdtParameters;
            public String text;
            public FileTemplate(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public void addTag(FDTParameters f) {
                this.fdtParameters=f;
            }
            public Object set(XmlPullParser xpp){
                this.text=xpp.getText();
                return this;
            }


        }
        private class FDTParameters{
            public HashMap<String, String> attrs=new HashMap<>();
            public File file;
            public FDTParameters(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }
            public void addTag(File f) {
                file=f;
            }


        }
        private class File{
            public HashMap<String, String> attrs=new HashMap<>();
            public File(XmlPullParser xpp){
                for (int i=0; i<xpp.getAttributeCount(); i++){
                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
                }
            }

            public void addTag(XmlPullParser xpp) {
            }


        }
//        private class RepairFlow{
//            private HashMap<String, String> attrs=new HashMap<>();
//
//            public RepairFlow(XmlPullParser xpp){
//                for (int i=0; i<xpp.getAttributeCount(); i++){
//                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
//                }
//            }
//            public void addTag(XmlPullParser xpp) {
//            }
//
//        }
//
//        private class FECParams{
//            private HashMap<String, String> attrs=new HashMap<>();
//            public FECParams(XmlPullParser xpp){
//                for (int i=0; i<xpp.getAttributeCount(); i++){
//                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
//                }
//            }
//            public void addTag(XmlPullParser xpp) {
//            }
//
//
//        }
//        private class FECOTI{
//            private HashMap<String, String> attrs=new HashMap<>();
//            public FECOTI(XmlPullParser xpp){
//                for (int i=0; i<xpp.getAttributeCount(); i++){
//                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
//                }
//            }
//            public void addTag(XmlPullParser xpp) {
//            }
//
//
//        }
//        private class ProtectedObject{
//            private HashMap<String, String> attrs=new HashMap<>();
//            public ProtectedObject(XmlPullParser xpp){
//                for (int i=0; i<xpp.getAttributeCount(); i++){
//                    attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
//                }
//            }
//            public void addTag(XmlPullParser xpp) {
//            }
//
//
//        }

    }


}
