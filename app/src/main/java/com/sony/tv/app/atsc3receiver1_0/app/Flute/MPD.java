package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.android.exoplayer2.util.Util.parseXsDuration;

/**
 * Created by xhamc on 3/24/17.
 */

public final class MPD {
    private final static String TAG="MPD";
    private final static String tag_MPD="MPD";
    private final static String attr_xmlString="xmlns";
    private final static String attr_minBufferTime="minBufferTime";
    private final static String attr_minimumUpdatePeriod="minimumUpdatePeriod";
    private final static String attr_type="type";
    private final static String attr_availabilityStartTime="availabilityStartTime";
    private final static String attr_timeShiftBufferDepth="timeShiftBufferDepth";
    private final static String attr_mediaPresentationDuration="mediaPresentationDuration";
    private final static String attr_profiles="profiles";
    private final static String tag_ProgramInformation="ProgramInformation";
    private final static String attr_moreInformationURL="moreInformationURL";
    private final static String tag_Title="Title";
    private final static String tag_Period="Period";
    private final static String attr_start="start";
    private final static String attr_duration="duration";
    private final static String tag_BaseUrl="BaseUrl";
    private final static String tag_AdaptationSet="AdaptationSet";
    private final static String attr_segmentAligment="segmentAlignment";
    private final static String attr_maxWidth="maxWidth";
    private final static String attr_maxHeight="maxHeight";
    private final static String attr_maxFrameRate="maxFrameRate";
    private final static String attr_par="par";
    private final static String attr_lang="lang";
    private final static String tag_Representation="Representation";
    private final static String attr_id="id";
    private final static String attr_mimeType="mimeType";
    private final static String attr_codecs="codecs";
    private final static String attr_width="width";
    private final static String attr_height="height";
    private final static String attr_frameRate="frameRate";
    private final static String attr_sar="sar";
    private final static String attr_startsWithSAP="startsWithSAP";
    private final static String attr_bandwidth="bandwidth";
    private final static String attr_timeScale="timescale";
    private final static String attr_media="media";
    private final static String attr_startNumber="startNumber";
    private final static String attr_initialization="initialization";
    private final static String tag_SegmentTemplate="SegmentTemplate";
    private final static String tag_S="S";
    private final static String tag_SegmentTimeline="SegmentTimeline";

    private final static String tag_AudioChannelConfiguration="AudioChannelConfiguration";


//    public String attributes;

    private ProgramInformation programInformation;
    private StringBuilder sb;
//    public String getAttributes(){return attributes;}

    private HashMap<String, String> attrs=new HashMap<>();

    public ArrayList<Period> periods=new ArrayList<>();

    public MPD(StringBuilder sb){
        this.sb=sb;
    }

    private int currentPeriodCount=0;
    private int currentAdaptationSetCount=0;
    private int currentRepresentationsCount=0;
    private String currentStartTag="";
    private String lastStartTag="";
    private String lastEndTag="";
    private Object textObject;
    private boolean titleText=false;
    private BaseUrl currentbaseUrl;

    public void addTag(XmlPullParser xpp) {
        String tag=xpp.getName();

        if (tag.equals(tag_MPD)) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        } else if (tag.equals(tag_ProgramInformation)){
            this.programInformation=new ProgramInformation(xpp) ;
        } else if (tag.equals(tag_Period)){
            periods.add(new Period(xpp));
            currentPeriodCount++;
            currentAdaptationSetCount=0;
            currentRepresentationsCount=0;
        }else if (tag.equals(tag_AdaptationSet)){
            periods.get(currentPeriodCount-1).addTag(new AdaptationSet(xpp));
            currentAdaptationSetCount++;
        }else if (tag.equals(tag_Representation)){
            periods.get(currentPeriodCount-1).adaptationSet.get(currentAdaptationSetCount-1).addTag (new Representation(xpp));
            currentRepresentationsCount++;
        }else if (tag.equals(tag_SegmentTemplate)) {
            periods.get(currentPeriodCount - 1).adaptationSet.get(currentAdaptationSetCount - 1).addTag(new SegmentTemplate(xpp));
        } else if (tag.equals(tag_SegmentTimeline)) {
            periods.get(currentPeriodCount - 1).adaptationSet.get(currentAdaptationSetCount - 1).segmentTemplate.addTag(new SegmentTimeline(xpp));
        }else if (tag.equals(tag_S)){
            periods.get(currentPeriodCount - 1).adaptationSet.get(currentAdaptationSetCount - 1).segmentTemplate.segmentTimeline.addTag(new TagS(xpp));
        }else if (tag.equals(tag_AudioChannelConfiguration)){
            periods.get(currentPeriodCount - 1).adaptationSet.get(currentAdaptationSetCount - 1).addTag(new AudioChannelConfiguration(xpp));
        }else if (tag.equals(tag_BaseUrl)){
            currentbaseUrl=new BaseUrl();
            if (currentRepresentationsCount>0){
                textObject=periods.get(currentPeriodCount-1).adaptationSet.get(currentAdaptationSetCount-1).representation.addTag(currentbaseUrl);
            } else if (currentAdaptationSetCount>0){
                textObject=periods.get(currentPeriodCount-1).adaptationSet.get(currentAdaptationSetCount-1).addTag(currentbaseUrl);
            } else if (currentPeriodCount>0){
                textObject=periods.get(currentPeriodCount-1).addTag(currentbaseUrl);
            } else{
                Log.e(TAG, "Error parsing MPD: Base Url not associated with any tag");
            }
        } else if (tag.equals(tag_Title)){
            programInformation.title=new Title();
            textObject=programInformation.title;
        }

    }

    public void endTag(XmlPullParser xpp) {
        //****Nothing to do here
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
    public HashMap<String,String> getAttributes(){
        return attrs;
    }
    public String getAttribute(String attribute){
        return getAttribute(this.attrs, attribute);
    }
    public void setAttribute(String attribute, String value){
        attrs.put(attribute,value);
    }

    public StringBuilder toStringBuilder(){
        sb.append("<MPD");
        Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String,String> it=iterator.next();
            sb.append(" ").append( it.getKey()).append("=\"").append(it.getValue()).append("\"");
        }
        sb.append(">\n");
        if (programInformation!=null){
            programInformation.addToStringBuffer();
        }

        for (int i=0; i<periods.size();i++){
            periods.get(i).addToStringBuffer();
        }
        sb.append("</MPD>");
        return sb;
    }

    public long getAvailabilityStartTimeFromVideos(HashMap<String,ContentFileLocation> videos){

        loop:for (int period = 0; period < periods.size(); period++) {
            String baseUrl=periods.get(period).getAttribute(tag_BaseUrl)!=""?periods.get(period).getAttribute(tag_BaseUrl):"/";
            for (int adaptationSet = 0; adaptationSet <periods.get(period).adaptationSet.size() ; adaptationSet++) {
                baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).getAttribute(tag_BaseUrl));
                baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).representation.getAttribute(tag_BaseUrl));

                if (periods.get(period).adaptationSet.get(adaptationSet).getAttribute("mimeType").contains("video") ||
                        periods.get(period).adaptationSet.get(adaptationSet).representation.getAttribute("mimeType").contains("video")){

                    SegmentTemplate videoTemplate =periods.get(period).adaptationSet.get(adaptationSet).segmentTemplate;
                    if (videoTemplate.getAttribute("media").contains("$RepresentationID")){
                        videoTemplate.setAttribute("media", videoTemplate.getAttribute("media").replaceAll("\\$RepresentationID\\$",
                                periods.get(period).adaptationSet.get(adaptationSet).representation.getAttribute("id") ));
                    }
                    if (videoTemplate.getAttribute("initialization").contains("$RepresentationID")){
                        videoTemplate.setAttribute("initialization", videoTemplate.getAttribute("initialization").replaceAll("\\$RepresentationID\\$",
                                periods.get(period).adaptationSet.get(adaptationSet).representation.getAttribute("id") ));
                    }
                    String[] check=baseUrl.concat(videoTemplate.getAttribute(attr_media)).split("\\$Number\\$");
                    for (Map.Entry<String,ContentFileLocation> video:videos.entrySet()){
                        if (!video.getKey().equals(baseUrl.concat(videoTemplate.getAttribute("initialization")))) {
                            if (video.getKey().startsWith(check[0]) && (video.getKey().endsWith(check[1]))) {
                                int videoSegmentNumber = Integer.parseInt(video.getKey().replace(check[0], "").replace(check[1], ""));
                                int periodStartNumber=Integer.parseInt(videoTemplate.getAttribute(attr_startNumber));
                                double videoSegmentDuration;
                                if (null==videoTemplate.segmentTimeline) {
                                    videoSegmentDuration = (long) 1000 * Double.parseDouble(videoTemplate.getAttribute(attr_duration)) /
                                            Double.parseDouble(videoTemplate.getAttribute(attr_timeScale));
                                } else {
                                    videoSegmentDuration = (long) 1000 * Double.parseDouble(videoTemplate.segmentTimeline.tagS.get(0).getAttribute("d")) /
                                            Double.parseDouble(videoTemplate.getAttribute(attr_timeScale));
                                }
                                long periodStartTimeOffset=parseXsDuration(periods.get(0).getAttribute(attr_start));

                                long periodStartTime=parseXsDuration(periods.get(period).getAttribute(attr_start));
                                long periodDuration=parseXsDuration(periods.get(period).getAttribute(attr_duration));
                                long videoStartTime=video.getValue().time;
                                long periodEndNumber=periodStartNumber+(long)(periodDuration/videoSegmentDuration);
                                if (videoSegmentNumber>=periodStartNumber && videoSegmentNumber<periodEndNumber) {
                                    long videoSegmentOffset =  (long) ((videoSegmentNumber - periodStartNumber) * videoSegmentDuration) + periodStartTime;
                                    long availabilityTime = videoStartTime - videoSegmentOffset;
                                    Log.d("TIME: ", "videoSegmentOffset ms: "+videoSegmentOffset+"   segment number: "+videoSegmentNumber+"  avail Time: "+availabilityTime);
                                    return availabilityTime;
                                }

                            }

                        }
                    }
                }
            }

        }

        return -1;
    }




    public class ProgramInformation{
        private HashMap<String, String> attrs=new HashMap<>();
        public Title title;
        public ProgramInformation(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer(){
            sb.append("<ProgramInformation");
            for (Map.Entry<String, String> it : attrs.entrySet()) {
                sb.append(" ").append(it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null!=title) {
                sb.append(">\n");
                title.addToStringBuffer();
                sb.append("</ProgramInformation>\n");
            }else {
                sb.append(" />");
            }
        }
    }

    public class Title{
        private String title;

        public Title Title(){
            return this;
        }
        public void  set(XmlPullParser xpp){
            this.title=xpp.getText();
        }
        public String get(){
            return title;
        }
        public void addToStringBuffer(){
            sb.append("<Title>").append(title).append("</Title>\n");
        }
    }

    public class Period{
        private BaseUrl baseUrl;
        public ArrayList<AdaptationSet> adaptationSet=new ArrayList<>();
        private HashMap<String, String> attrs=new HashMap<>();
        public Period(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public Object addTag (BaseUrl bu){
            this.baseUrl=bu;
            return this;
        }
        public void addTag (AdaptationSet as){
            adaptationSet.add(as);
            currentRepresentationsCount=0;
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer(){
            sb.append("<Period");

            for (Map.Entry<String, String> it : attrs.entrySet()) {
                sb.append(" ").append(it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl && null==adaptationSet){
                sb.append(" />\n");
                return;
            }
            sb.append(">\n");
            if (null!=baseUrl){
                baseUrl.addToStringBuffer();
            }
            for (int i=0; i<adaptationSet.size();i++) {
                adaptationSet.get(i).addToStringBuffer();
            }
            sb.append("</Period>\n");
        }
    }

    public class BaseUrl{
        public String baseUrl;
        public void set(XmlPullParser xpp){
            this.baseUrl=xpp.getText();
        }
        public String get(){
            return baseUrl;
        }
        public void addToStringBuffer(){
            sb.append("<BaseUrl>").append(baseUrl).append(("</BaseUrl>\n"));
        }
    }


    public class AdaptationSet{

        private BaseUrl baseUrl;
        public  Representation representation;
        public SegmentTemplate segmentTemplate;
        private AudioChannelConfiguration audioChannelConfiguration;

        private HashMap<String, String> attrs=new HashMap<>();
        public AdaptationSet(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }

        public Object addTag (BaseUrl bu){
            this.baseUrl=bu;
            return this;
        }

        public void addTag (Representation r){
            representation=r;
        }

        public void addTag(SegmentTemplate st){
            segmentTemplate=st;
        }


        public void addTag(AudioChannelConfiguration acc){
            audioChannelConfiguration=acc;
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }


        public void addToStringBuffer(){
            sb.append("<AdaptationSet");

            for (Map.Entry<String, String> it : attrs.entrySet()) {
                sb.append(" ").append(it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            if (null==baseUrl && null==segmentTemplate && null==audioChannelConfiguration ){
                sb.append(" />\n");
                return;
            }
            sb.append(">\n");
            if (null!=representation) {
                representation.addToStringBuffer();
            }
            if (null!=audioChannelConfiguration){
                audioChannelConfiguration.addToStringBuffer();
            }
            if (null!=segmentTemplate){
                segmentTemplate.addToStringBuffer();
            }
            sb.append("</AdaptationSet>\n");
        }
    }

    public class Representation{

        private BaseUrl baseUrl;
        private SegmentTemplate segmentTemplate;
        private HashMap<String, String> attrs=new HashMap<>();
        public Representation(XmlPullParser xpp){
            for (int i=0; i<xpp.getAttributeCount(); i++){
                attrs.put(xpp.getAttributeName(i),xpp.getAttributeValue(i));
            }
        }
        public Object addTag (BaseUrl bu){
            this.baseUrl=bu;
            return this;
        }


        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }



        public void addToStringBuffer(){
            sb.append("<Representation");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();

            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }

            sb.append(">\n");
            if (null!=baseUrl){
                baseUrl.addToStringBuffer();
            }
            sb.append("</Representation>\n");
        }


    }


    public class SegmentTemplate {

        private SegmentTimeline segmentTimeline;
        private HashMap<String, String> attrs=new HashMap<>();


        public SegmentTemplate(XmlPullParser xpp) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }
        public void addTag(SegmentTimeline st){
            segmentTimeline=st;
        }


        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer() {
                sb.append("<SegmentTemplate");
                Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String,String> it=iterator.next();
                    sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
                }
                sb.append(" />\n");
        }
    }

    public class SegmentTimeline {
        private HashMap<String, String> attrs=new HashMap<>();
        private ArrayList<TagS> tagS=new ArrayList<>();

        public SegmentTimeline(XmlPullParser xpp){
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }


        public void addTag(TagS s){
            tagS.add(s);
        }

        public void addToStringBuffer() {
            sb.append("<S");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            sb.append(" />\n");
        }


    }
    public class TagS {

        private HashMap<String, String> attrs=new HashMap<>();


        public TagS(XmlPullParser xpp){
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }

    }

    public class AudioChannelConfiguration {
        private HashMap<String, String> attrs=new HashMap<>();

        public AudioChannelConfiguration(XmlPullParser xpp) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
            }
        }

        public String getAttribute(String attribute){
            return MPD.getAttribute(attrs,attribute);
        }
        public void setAttribute(String attribute, String value){
            MPD.setAttribute(attrs, attribute, value);
        }

        public void addToStringBuffer() {
            sb.append("<AudioChannelConfiguration");
            Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String,String> it=iterator.next();
                sb.append(" ").append( it.getKey()).append("=").append("\"").append(it.getValue()).append("\"");
            }
            sb.append(" />");


        }

    }
    private static void setAttribute(HashMap<String,String> attrs, String attribute, String value){
        attrs.put(attribute,value);
        Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
    }

    private static String getAttribute(HashMap<String,String> attrs, String attribute){
        Iterator<Map.Entry<String,String>> iterator=attrs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String,String> it=iterator.next();
            if (it.getKey().equals(attribute)) {
                return it.getValue();
            }
        }
        return "";
    }


//    for (int period = 0; period < periods.size(); period++) {
//        String baseUrl=periods.get(period).getAttribute(tag_BaseUrl)!=""?periods.get(period).getAttribute(tag_BaseUrl):"/";
//        for (int adaptationSet = 0; adaptationSet < 2; adaptationSet++) {
//            baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).getAttribute(tag_BaseUrl));
//            baseUrl=baseUrl.concat(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).getAttribute(tag_BaseUrl));
//
//            if (periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).getAttribute("mimeType").contains("video")){
//                adapSetVideoIndex[period]=adaptationSet;
//                SegmentTemplate videoTemplate =periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate;
//                videoSegmentDuration[period]=Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_duration))/
//                        Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_timeScale));
//
//                String[] check=baseUrl.concat(videoTemplate.getAttribute(attr_media)).split("\\$Number\\$");
//
//                for (Map.Entry<String,ContentFileLocation> video:videos.entrySet()){
//                    if (!video.getKey().equals(baseUrl.concat(videoTemplate.getAttribute("initialization")))) {
//                        if (video.getKey().startsWith(check[0]) && (video.getKey().endsWith(check[1]))) {
//                            videoIndexPresent[period]=true;
//                            int result = Integer.parseInt(video.getKey().replace(check[0], "").replace(check[1], ""));
//                            oldestVideoIndex[period] = Math.min(result, oldestVideoIndex[period]);
//                            earliestVideoIndex[period] = Math.max(result, earliestVideoIndex[period]);
//                        }
//
//                    }
//                }
//            } else {
//                adapSetAudioIndex[period]=adaptationSet;
//                SegmentTemplate audioTemplate = periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate;
//                audioSegmentDuration[period] = Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_duration)) /
//                        Double.parseDouble(periods.get(period).adaptationSet.get(adaptationSet).representations.get(0).segmentTemplate.getAttribute(attr_timeScale));
//                String[] check = baseUrl.concat(audioTemplate.getAttribute(attr_media)).split("\\$Number\\$");
//
//                for (Map.Entry<String, ContentFileLocation> audio : audios.entrySet()) {
//                    if (!audio.getKey().equals(baseUrl.concat(audioTemplate.getAttribute(attr_initialization)))) {
//                        if (audio.getKey().startsWith(check[0]) && (audio.getKey().endsWith(check[1]))) {
//                            audioIndexPresent[period]=true;
//                            int result = Integer.parseInt(audio.getKey().replace(check[0], "").replace(check[1], ""));
//                            oldestAudioIndex[period] = Math.min(result, oldestAudioIndex[period]);
//                            earliestAudioIndex[period] = Math.max(result, earliestAudioIndex[period]);
//                        }
//
//                    }
//                }
//            }
//        }
//
//    }

}
