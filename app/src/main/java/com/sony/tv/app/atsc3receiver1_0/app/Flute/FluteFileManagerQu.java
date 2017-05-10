package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.Ad.AdCategory;
import com.sony.tv.app.atsc3receiver1_0.app.Ad.AdContent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;


/**
 * The fileManager for Qualcomm server content. Based on use of EFDT instances rather than TOI instances
 * Created by xhamc on 3/21/17.
 */

public class FluteFileManagerQu implements FluteFileManagerBase {

    private static final long AVAILABILITY_TIME_OFFSET=1500;                    //Offset from time the content is received in buffer to time reported to player
    private static final String MIN_BUFFER_TIME="PT1S";                         //Used by player to set lower buffer threshold
    //    private static final String TIME_SHIFT_BUFFER_OFFSET="PT3S";
    private static final String TIME_SHIFT_BUFFER_DEPTH="PT3S";                 //Used by player to set the depth of the buffer

    private static final String MINIMUM_UPDATE_PERIOD="PT0.75S";                //Frequency the player can request MPD (>0 else can hurt performance)
    private static final String SUGGESTED_PRESENTATION_DELAY="PT0S";

    //        HashMap<String, ContentFileLocation> mapContentLocations;
    private static final String TAG="FileManager";
    private static final int MAX_SIGNALING_BUFFERSIZE=10000;                    //Size of the circular buffer to manage signaling
    private static final int MAX_VIDEO_BUFFERSIZE=20000000;                     //Size of the circular buffer to manage video
    private static final int MAX_AUDIO_BUFFERSIZE=2000000;                      //Size of the circular buffer to manage audio
    private static final int MAX_FILE_RETENTION_MS=10000;                       //Max time to keep stale content around (unused)

    private HashMap<String, ContentFileLocation> mapFileLocationsSig;           //**
    private HashMap<String, ContentFileLocation> mapFileLocationsAud;           //Map of filename to location in buffers
    private HashMap<String, ContentFileLocation> mapFileLocationsVid;           //**

    private ArrayList<HashMap<String, ContentFileLocation>> arrayMapFileLocations;  //ArrayList of above
    private HashMap<Integer,String> map_TOI_FileNameSig;                            //**
    private HashMap<Integer,String> map_TOI_FileNameAud;                            //Given key TOI, return value FileName
    private HashMap<Integer,String> map_TOI_FileNameVid;                            //**
    private ArrayList<HashMap<Integer,String>> array_MapTOI_FileName =new ArrayList<>();    //ArrayList of above
    private HashMap<Integer, Integer> mapGetTSIFromBufferNumber;                //key Buffer array index to value TSI
    private HashMap<Integer, Integer> mapGetBufferNumberFromTSI;                //key TSI value to Buffer Array Index

    private byte[] signalingStorage;                                            //**
    private byte[] videoStorage;                                                //Uninitialized circular buffers
    private byte[] audioStorage;                                                //**
    private ArrayList<byte[]> storage=new ArrayList<>();                        //ArrayList of above buffers

    private ReentrantLock lock = new ReentrantLock();                           //Lock for write and reading buffers

    private int[] firstAvailablePosition={0,0,0};                               //Tracks the first available write position in each buffer
    private int[] maxAvailablePosition={MAX_SIGNALING_BUFFERSIZE-1,  MAX_VIDEO_BUFFERSIZE-1, MAX_AUDIO_BUFFERSIZE-1,};  //Tracks the last available write position in each buffer

    private FluteFileManagerQu sInstance;

    private DataSpec signalingDataSpec;                                         //Uri info from Exoplayer for signal file
    private DataSpec avDataSpec;                                                //Uri info from Exoplayer for AV file

    private boolean first;                                                      //Indicates the first read from Exoplayer. Uses to set availability start time
    private long availabilityStartTime;                                         //AvailabilityStartTime calc from video write time with respect to period start/segment duration
    private long availabilityStartTimeOffset;                                   //Offset from this (set to static offset above)

    private AdContent lastAdInsertion;                                             //Which Ad was last inserted
    private String lastAdStart="";                                              //Time for that ad in Manifest time format

    private FluteTaskManagerQu fluteTaskManagerQu;


    public FluteFileManagerQu(DataSpec dataSpec, FluteTaskManagerQu fluteTaskManagerQu){                                 //Create just a signaling filemanager
        sInstance=this;
        signalingDataSpec=dataSpec;
        this.fluteTaskManagerQu = fluteTaskManagerQu;

    }

    public FluteFileManagerQu(DataSpec signallingDataSpec, DataSpec av, FluteTaskManagerQu fluteTaskManagerQu){          //Create bith signaling and av filemanager
        sInstance=this;
        this.signalingDataSpec=signallingDataSpec;
        this.avDataSpec=av;
        this.fluteTaskManagerQu = fluteTaskManagerQu;
    }

    /**
     * Initialize the arrays and hashmaps
     */
    public void reset(){

        mapFileLocationsSig=new HashMap<>();
        mapFileLocationsAud=new HashMap<>();
        mapFileLocationsVid=new HashMap<>();

        arrayMapFileLocations = new ArrayList<>();
        map_TOI_FileNameSig = new HashMap<>();
        map_TOI_FileNameAud = new HashMap<>();
        map_TOI_FileNameVid = new HashMap<>();
        array_MapTOI_FileName =new ArrayList<>();
        mapGetTSIFromBufferNumber=new HashMap<>();             //retrieve the relevant FileManager from the TSI value
        mapGetBufferNumberFromTSI=new HashMap<>();             //retrieve the relevant FileManager from the TSI value

        signalingStorage=new byte[MAX_SIGNALING_BUFFERSIZE];
        videoStorage=new byte[MAX_VIDEO_BUFFERSIZE];
        audioStorage=new byte[MAX_AUDIO_BUFFERSIZE];

        arrayMapFileLocations.add(0,mapFileLocationsSig);
        arrayMapFileLocations.add(1,mapFileLocationsVid);
        arrayMapFileLocations.add(2,mapFileLocationsAud);
        array_MapTOI_FileName.add(0,map_TOI_FileNameSig);
        array_MapTOI_FileName.add(1,map_TOI_FileNameVid);
        array_MapTOI_FileName.add(2,map_TOI_FileNameAud);

            /*default mapping of TSI to buffer positions*/
        mapGetTSIFromBufferNumber.put(0,0);
        mapGetTSIFromBufferNumber.put(1,1);
        mapGetTSIFromBufferNumber.put(2,2);
        mapGetBufferNumberFromTSI.put(0, 0);
        mapGetBufferNumberFromTSI.put(1, 1);
        mapGetBufferNumberFromTSI.put(2, 2);

        storage.add(0,signalingStorage);
        storage.add(1,videoStorage);
        storage.add(2,audioStorage);

        first=true;

    }


    public int getNumberOfAudioFiles(){
        return map_TOI_FileNameAud.size();
    }

    /**
     * Force player to calc availability start time again
     */
    public void resetTimeStamp(){
        first=true;
    }


    public FluteFileManagerQu getInstance(){ return sInstance; }
//    public static FluteFileManagerQu getInstance(){ return sInstance; }


    private static final int MPD=0;
    private static final int SLS=1;
    private static final int AV=2;

    private byte[] mMPDbytes;

    private int[] bytesToRead=new int[100];
    private int[] bytesRead=new int [100];
    private int[] bytesToSkip=new int[100];
    private int[] byteOffset=new int[100];
    private boolean[] timeOffsetFirst=new boolean[100];

    /**
     * Class to hold buffer pointers based on filename
     */
    private class FileBuffer{
        public byte[] buffer;
        public int contentLength;
        public int startOfContent;
        public FileBuffer(byte[] buffer, int contentLength, int startOfContent){
            this.buffer=buffer; this.contentLength=contentLength; this.startOfContent=startOfContent;
        }
    }

    /**
     * allows up to 100 different threads to open a file!
     */
    private FileBuffer[] fileBuffer=new FileBuffer[100];

    /**
     * Called from Exoplayer via FluteDataSource to open a file to read
     * @param dataSpec
     * @param thread tracks which Exoplayer sink is calling the filemanager (sig/aud/vid)
     * @return length of the file to read or -1 if not found;
     * @throws IOException
     */
    public long open(DataSpec dataSpec, int thread) throws IOException {
        lock.lock();
//        mFluteTaskManager=FluteReceiver.getInstance().mFluteTaskManager;

        try{

            bytesToRead[thread]=0;
            bytesToSkip[thread]=0;
            bytesRead[thread]=0;
            byteOffset[thread]=0;


            Log.d("TAG", "ExoPlayer trying to open :"+dataSpec.uri);
            String host = dataSpec.uri.getHost();
            int port = dataSpec.uri.getPort();
//            if ( mFluteTaskManager.dataSpec.uri.getHost().equals(host) && mFluteTaskManager.dataSpec.uri.getPort()==port){
            if (signalingDataSpec.uri.getHost().equals(host) && signalingDataSpec.uri.getPort()==port ||
                    avDataSpec.uri.getHost().equals(host) && avDataSpec.uri.getPort()==port){

                String path=dataSpec.uri.getPath();
                bytesToSkip[thread]=(int) dataSpec.position;
                FileBuffer fb = openInternal(path, thread);
                if (fb==null){
                    Log.d(TAG, "Couldn't fine file while trying to open: "+path);

                    //throw new IOException("Couldn't fine file while trying to open: "+path);
                    return -1;
                }else{
                    fileBuffer[thread]=fb;
                    bytesToRead[thread]=fb.contentLength-bytesToSkip[thread];
                    byteOffset[thread]=fb.startOfContent+bytesToSkip[thread];
                }
                return (bytesToRead[thread]-bytesToSkip[thread]);


            } else{

                throw new IOException("Attempted to open a url that is not active: ".concat(dataSpec.toString()));
            }

        }finally{

            lock.unlock();
        }

    }

    /**
     * Called from Exoplayer via FluteDataSource to read from opened buffer
     * @param buffer
     * @param offset
     * @param readLength
     * @param thread
     * @return
     * @throws IOException
     */
    public int read(byte[] buffer, int offset, int readLength, int thread) throws IOException {
        lock.lock();
        try {
            if (readLength == 0) {
                return 0;
            }
            if (bytesToRead[thread] != C.LENGTH_UNSET) {
                long bytesRemaining = bytesToRead[thread] - bytesRead[thread];
                if (bytesRemaining == 0) {
                    return C.RESULT_END_OF_INPUT;
                }
                readLength = (int) Math.min(readLength, bytesRemaining);
            } else {
                return C.LENGTH_UNSET;
            }
            if (readLength < 0) {
                Log.d(TAG, "BytesToRead: " + bytesToRead + "  bytesRead: " + bytesRead + " readLength:  " + readLength);
                throw new IOException("Read Length is less than 0");

            }
            if (byteOffset[thread] + bytesRead[thread] + readLength < MAX_VIDEO_BUFFERSIZE) {
                System.arraycopy(fileBuffer[thread].buffer, byteOffset[thread] + bytesRead[thread], buffer, offset, readLength);
                bytesRead[thread] += readLength;
            } else {
                Log.e(TAG, "Error trying to read from local buffer, overrun: bytesRead: " + bytesRead[thread] + "  byteOffset: " + byteOffset[thread] + "  length:  " + readLength);
            }
            //            listenertener.onBytesTransferred(this, read);
            //        }
            return readLength;
        }finally{
            lock.unlock();
        }
    }

    /**
     * Determines the buffer location of the file and returns it. If request is manifest then return adjusted manifest (buffer times, AST, ad inserted periods)
     * @param fileName
     * @param thread
     * @return
     */
    private FileBuffer openInternal(String fileName, int thread){
        Log.d(TAG,"Opening new File: ***********"+fileName);

        int index=0; ContentFileLocation f; int contentLength=0;
        do {
            f = arrayMapFileLocations.get(index).get(fileName);
            index++;
        }while (index < arrayMapFileLocations.size() && f==null) ;

        if (f != null) {
            index--;
            if (fileName.toLowerCase().endsWith(".mpd")) {
                String mpdData = new String(storage.get(0), f.start, f.contentLength);
                mMPDbytes = parseManifest(mpdData);
                contentLength=mMPDbytes.length;

                return new FileBuffer(mMPDbytes, contentLength, 0);


            }else {

                return new FileBuffer(storage.get(index),  f.contentLength,   f.start);

            }

        } else{
            Log.d(TAG,"Couldn't fine file while trying to open: "+fileName);
            return null;
        }

    }

    /**
     * Called from FluteTaskManagerQu to write an object received over flute;
     * If last object then make available by removing .new off name and timestampe it;
     *
     * @param r
     * @param input
     * @param offset
     * @param length
     * @return
     */
    public String write(RouteDecodeBase r, byte[] input, int offset, int length){
        int toi=r.toi();
        lock.lock();
        try{
            int index=mapGetBufferNumberFromTSI.get(r.tsi());
            HashMap<Integer,String> t=array_MapTOI_FileName.get(index);
            HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);

            if (t.containsKey(toi) && m.containsKey(t.get(toi))){
                ContentFileLocation l= m.get(t.get(toi));
                if (length<=(l.contentLength - l.nextWritePosition)){
                    System.arraycopy(input, offset, storage.get(index), l.start + l.nextWritePosition, length);
                    l.nextWritePosition+=length;
                    if (l.nextWritePosition==l.contentLength){
                        String fileName=l.fileName.substring(0,l.fileName.length()-4);    //Finished so copy object to not ".new"
                        Iterator<Map.Entry<Integer, String>> it=t.entrySet().iterator();
                        while (it.hasNext()){
                            Map.Entry<Integer, String> entry=it.next();
                            if (entry.getValue().equals(fileName)){
                                it.remove();
                            }
                        }
                        m.remove(l.fileName);
                        l.fileName=fileName;
                        Date now=Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
                        l.time=now.getTime();
                        m.put(fileName, l);
                        if (index==1){      //video
                            fluteTaskManagerQu.callBackInterface.callBackVideoFileSize(fluteTaskManagerQu.index(),m.size());
                        }
                        t.remove(toi);
                        t.put(toi, fileName);
                        Log.d(TAG, "Wrote file: "+ fileName +" of size "+ l.contentLength + " to buffer: "+index + "which is connected to TSI: " + mapGetTSIFromBufferNumber.get(index));
                        return fileName;  //complete
                    }
                }else {
                    Log.e(TAG, "Attempt at buffer write overrun: "+l.fileName);
                }
            }
            return "";
        } finally{
            lock.unlock();
        }

    }

    /**
     * Manipulate the Manifest by replacing AST, changing buffering params and inserting ads
     * @param mpdData input data
     * @return changed data
     */
    private byte[] parseManifest(String mpdData){

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if (first){
            MPDParser mpdParser=new MPDParser(mpdData, mapFileLocationsVid, mapFileLocationsAud);
            mpdParser.MPDParse();
            availabilityStartTimeOffset=AVAILABILITY_TIME_OFFSET;
            availabilityStartTime=mpdParser.mpd.getAvailabilityStartTimeFromVideos(mapFileLocationsVid)+availabilityStartTimeOffset;
            Log.d(TAG,"AvailabilityStartTime set to "+availabilityStartTime +  "  difference: "+ ((Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()).getTime()-availabilityStartTime));
            first=false;
        }
        String[] mpdSplit=mpdData.split("\\?>",2);
        String mpdHeaderStart=mpdSplit[1];
        String[] mpdDataSplit=mpdHeaderStart.split(">",2);
        String mpdHeader=mpdDataSplit[0].concat(">");


        formatter.setTimeZone(timeZone);
        String availabilityStartTimeString= formatter.format(new Date(availabilityStartTime));

        MPDParser mpdParser=new MPDParser(mpdHeader, mapFileLocationsVid, mapFileLocationsAud);
        mpdParser.MPDParse();
        mpdParser.mpd.getAttributes().put("minBufferTime",MIN_BUFFER_TIME);
//        mpdParser.mpd.getAttributes().put("timeShiftBufferOffset",TIME_SHIFT_BUFFER_OFFSET);
        mpdParser.mpd.getAttributes().put("timeShiftBufferDepth",TIME_SHIFT_BUFFER_DEPTH);
        mpdParser.mpd.getAttributes().put("minimumUpdatePeriod",MINIMUM_UPDATE_PERIOD);
        mpdParser.mpd.getAttributes().put("availabilityStartTime",availabilityStartTimeString);
        mpdParser.mpd.getAttributes().put ("mediaPresentationDuration","PT1000H20M35S");

        mpdData=mpdParser.toStringBuilder().toString().split("</MPD>")[0].concat(mpdDataSplit[1]);

        if (ATSC3.ADS_ENABLED){
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser xpp = factory.newPullParser();
                StringReader s=new StringReader(mpdData);
                xpp.setInput(s);
                int periodNumber=0;
                int eventType = xpp.getEventType();
                while (eventType!=XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_DOCUMENT) {

                    } else if(eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals("Period")){
                            for (int i=0; i<xpp.getAttributeCount(); i++){
                                if (xpp.getAttributeName(i).startsWith("xlink")){
                                    int indexStart=0;
                                    int indexEnd=0;
                                    String period="";
                                    for (int j=0; j<=periodNumber; j++){
                                        indexStart=mpdData.indexOf("<Period", indexEnd+9);
                                        indexEnd=mpdData.indexOf("</Period>", indexEnd+9);
                                    }
                                    String start=xpp.getAttributeValue(null,"start");
                                    if (lastAdInsertion==null || !start.equals(lastAdStart)){
                                        lastAdStart=start;
                                        start="start=\"".concat(start).concat("\"");
                                        lastAdInsertion= AdCategory.getNextAd(false);
                                        lastAdInsertion.period=lastAdInsertion.period.replaceFirst( "start=['|\"][PTMHS\\.0-9]+['|\"]",start);

                                    }
                                    mpdData=mpdData.substring(0,indexStart).concat(lastAdInsertion.period).concat(mpdData.substring(indexEnd+9,mpdData.length()));
                                    break;
                                }
                            }
                            periodNumber++;

                        }
                    } else if(eventType == XmlPullParser.END_TAG) {
                    } else if(eventType == XmlPullParser.TEXT) {
                    }else{
                    }
                    eventType = xpp.next();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return mpdData.getBytes();

    }

    /**
     * New EFDT file instance found so create in relevant buffer labeled .new
     * Remove any overwritten entries;
     * @param r
     * @return
     * @throws Exception
     */
    public boolean create(RouteDecodeBase r) throws Exception {
        lock.lock();
        try {
            int tsi=r.tsi();
            int index=mapGetBufferNumberFromTSI.get(tsi);

            HashMap<Integer,String> t=array_MapTOI_FileName.get(index);
            HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);
            firstAvailablePosition[index] = (firstAvailablePosition[index] + r.contentLength()) > maxAvailablePosition[index] ? 0 : firstAvailablePosition[index];
            Date now=Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();

            ContentFileLocation c = new ContentFileLocation(r.fileName().concat(".new"), r.efdt_toi(), firstAvailablePosition[index], r.contentLength(),
                    now.getTime(), now.getTime() + MAX_FILE_RETENTION_MS);
            Iterator<Map.Entry<String,ContentFileLocation>> iterator= m.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String,ContentFileLocation> it= iterator.next();
                int itStart=it.getValue().start;
                int itEnd=it.getValue().start+it.getValue().contentLength-1;
                int cStart=c.start;
                int cEnd=c.start+c.contentLength-1;
                if ( (itStart<=cStart && itEnd>=cStart) || (itStart>=cStart && itStart<=cEnd) ) {             //the new content will overwrite this entry so remove from list;
                    t.remove(it.getValue().toi);
                    iterator.remove();
                }
            }

            m.put(c.fileName, c);
            t.put(r.efdt_toi(),c.fileName);

            firstAvailablePosition[index] += r.contentLength();
            return true;

        } finally {
            lock.unlock();
        }
    }

}
