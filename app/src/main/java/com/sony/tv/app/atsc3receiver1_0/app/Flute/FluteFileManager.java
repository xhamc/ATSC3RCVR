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

import static com.google.android.exoplayer2.util.Util.parseXsDuration;

/**
 * Created by xhamc on 5/9/17.
 */

public class FluteFileManager {


    public static final long AVAILABILITY_TIME_OFFSET=4500;                    //Offset from time the content is received in buffer to time reported to player
    public static final long MIN_BUFFER_TIME_LONG=0;
    private static final String MIN_BUFFER_TIME="PT2S";                         //Used by player to set lower buffer threshold
    private static final String TIME_SHIFT_BUFFER_OFFSET="PT3S";
    private static final String TIME_SHIFT_BUFFER_DEPTH="PT3S";                 //Used by player to set the depth of the buffer
    private static final String MINIMUM_UPDATE_PERIOD="PT0.75S";                //Frequency the player can request MPD (>0 else can hurt performance)
    private static final String SUGGESTED_PRESENTATION_DELAY="PT0S";

    private static final String TAG="FileManager";
    public static final int MAX_SIGNALING_BUFFERSIZE=10000;                     //Size of the circular buffer to manage signaling
    public static final int MAX_VIDEO_BUFFERSIZE=10000000;                      //Size of the circular buffer to manage video
    private static final int MAX_FILE_RETENTION_MS=10000;                       //Max time to keep stale content around (unused)

    private HashMap<String, ContentFileLocation> mapFileLocationsSig;           //**
    private HashMap<String, ContentFileLocation> mapFileLocationsVid;           //**

    private ArrayList<HashMap<String, ContentFileLocation>> arrayMapFileLocations;  //ArrayList of above
    private HashMap<Long,String> map_TOI_FileNameSig;                        //**Given key TOI/TSI, return value FileName
    private HashMap<Long,String> map_TOI_FileNameVid;                        //**
    private ArrayList<HashMap<Long,String>> array_MapTOI_FileName;                           //ArrayList of above
    private HashMap<Integer/*TSI*/, Integer/*BufferNo*/> mapGetBufferNumberFromTSI;             //retrieve the relevant FileManager from the TSI value

    private byte[] signalingStorage;                                                //**
    private byte[] videoStorage;                                                //Uninitialized circular buffers
    private ArrayList<byte[]> storage=new ArrayList<>();                        //Array list of above

    private ReentrantLock lock = new ReentrantLock();                           //Lock for write and reading buffers

    private int[] firstAvailablePosition={0,0};                               //Tracks the first available write position in each buffer
    private int[] maxAvailablePosition={MAX_SIGNALING_BUFFERSIZE-1,  MAX_VIDEO_BUFFERSIZE-1};  //Tracks the last available write position in each buffer

    private FluteFileManager sInstance;

    public DataSpec baseDataSpec;                                               //Base URI for this filemanager

    private boolean first;                                                      //Indicates the first read from Exoplayer. Uses to set availability start time
    private long availabilityStartTime;                                         //AvailabilityStartTime calc from video write time with respect to period start/segment duration
    private int videoStartNumber;                                               //Used when faking the manifest only
    private long availabilityStartTimeOffset;                                   //Offset from this (set to static offset above)
    private long liveReadFromBufferTime;

    private AdContent lastAdInsertion;
    private String lastAdStart="";


    public boolean manifestFound=false;
    public boolean stsidFound=false;
    public boolean usbdFound=false;
    private FluteTaskManager fluteTaskManager;

    public FluteFileManager(DataSpec dataSpec){
        sInstance=this;
        baseDataSpec=dataSpec;
        reset();
    }

    public FluteFileManager(DataSpec dataSpec, FluteTaskManager fluteTaskManager){
        sInstance=this;
        baseDataSpec=dataSpec;
        this.fluteTaskManager=fluteTaskManager;
        reset();
    }
    /**
     * Initialize the arrays and hashmaps
     */
    public void reset(){

        mapFileLocationsSig=new HashMap<>();
        mapFileLocationsVid=new HashMap<>();

        arrayMapFileLocations = new ArrayList<>();
        map_TOI_FileNameSig = new HashMap<>();
        map_TOI_FileNameVid = new HashMap<>();
        array_MapTOI_FileName =new ArrayList<>();
        mapGetBufferNumberFromTSI=new HashMap<>();             //retrieve the relevant FileManager from the TSI value

        signalingStorage=new byte[MAX_SIGNALING_BUFFERSIZE];
        videoStorage=new byte[MAX_VIDEO_BUFFERSIZE];

        arrayMapFileLocations.add(0,mapFileLocationsSig);
        arrayMapFileLocations.add(1,mapFileLocationsVid);
        array_MapTOI_FileName.add(0,map_TOI_FileNameSig);
        array_MapTOI_FileName.add(1,map_TOI_FileNameVid);

        mapGetBufferNumberFromTSI.put(0, 0);

        storage.add(0,signalingStorage);
        storage.add(1,videoStorage);

        liveReadFromBufferTime=0;


        first=true;

    }

    public int getNumberOfVideoFiles(){
        return map_TOI_FileNameVid.size();
    }

    public void resetTimeStamp(){
        first=true;
    }


    public FluteFileManager getInstance(){ return sInstance; }


    private static final int MPD=0;
    private static final int SLS=1;
    private static final int AV=2;

    private byte[] mMPDbytes;

    private HashMap<Integer, byte[]> threadBufferPointer=new HashMap<>();

    private int[] bytesToRead=new int[100];
    private int[] bytesRead=new int [100];
    private int[] bytesToSkip=new int[100];
    private int[] byteOffset=new int[100];
    private boolean[] timeOffsetFirst=new boolean[100];

    private class FileBuffer{
        public byte[] buffer;
        public int contentLength;
        public int startOfContent;
        public FileBuffer(byte[] buffer, int contentLength, int startOfContent){
            this.buffer=buffer; this.contentLength=contentLength; this.startOfContent=startOfContent;
        }
    }
    private FileBuffer[] fileBuffer=new FileBuffer[100];

    /**
     * Open a file to read from buffer
     * @param dataSpec  path
     * @param thread    which exoplayer thread is accessing
     * @return          How many bytes can be read
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
            if (baseDataSpec.uri.getHost().equals(host) && baseDataSpec.uri.getPort()==port){

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
     * Player reads from buffers. Emulate the action of a stream
     * @param buffer        destination buffer
     * @param offset        offset into the buffer
     * @param readLength    max length to write into the buffer
     * @param thread        tags the Exoplayer DataSource to allow multithreading
     * @return              actual bytes written
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
     * Create the mapping for the buffer storage locations based on filename
     * @param r     Route header
     * @return      return true if the buffer has been allocated to the relevant TSI
     * @throws Exception
     */
    public boolean create(RouteDecode r) throws Exception {
        int tsi=r.tsi();
        Integer index=mapGetBufferNumberFromTSI.get(tsi);
        if (null==index){
            Log.e(TAG,"Asking for tsi that isn't there: :"+tsi);
            return false;
        }
        HashMap<Long,String> t=array_MapTOI_FileName.get(index);                                       //TOI maps to a filename
        HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);
        firstAvailablePosition[index] = (firstAvailablePosition[index] + r.contentLength()) > maxAvailablePosition[index] ? 0 : firstAvailablePosition[index];
        Date now= Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        ContentFileLocation c = new ContentFileLocation(r.fileName(), r.toi(), r.tsi(), firstAvailablePosition[index], r.contentLength(),
                now.getTime(), now.getTime() + MAX_FILE_RETENTION_MS, storage.get(index));
        Iterator<Map.Entry<String,ContentFileLocation>> iterator= m.entrySet().iterator();
        while (iterator.hasNext()) {                                                                      //remove any content from the map that will get overwritten by this file
            Map.Entry<String,ContentFileLocation> it= iterator.next();
            int itStart=it.getValue().start;
            int itEnd=it.getValue().start+it.getValue().contentLength-1;
            int cStart=c.start;
            int cEnd=c.start+c.contentLength-1;
            if ( (itStart<=cStart && itEnd>=cStart) || (itStart>=cStart && itStart<=cEnd) ) {             //the new content will overwrite this entry so remove from list;
                t.remove(intsTolong(it.getValue().toi, it.getValue().tsi));
                iterator.remove();
            }
        }
        m.put(c.fileName, c);
        if (r.type()==RouteDecode.EXT_FDT){

            t.put(intsTolong(r.efdt_toi(),r.tsi()),c.fileName);
        }else {
            t.put(intsTolong(r.toi(), r.tsi()), c.fileName);
        }
        firstAvailablePosition[index] += r.contentLength();

        return true;
    }

    private long intsTolong(int a, int b){

        return (long) b<<32|a;
    }

    /**
     * write the contents received into required buffer.
     *
     * @param r         The decoded header information
     * @param input     data received
     * @param offset    header offset to get to real payload
     * @param length    length of rela payload
     * @return          return the filename is the write is complete else empty string;
     */
    public ContentFileLocation write(RouteDecode r, byte[] input, int offset, int length){
        int toi=r.toi();
        int tsi=r.tsi();
        long toi_tsi=intsTolong(toi,tsi);
        lock.lock();
        try{
//            Log.d(TAG,"TSI: "+r.tsi()+"  TOI: "+r.toi()+ " POS: "+r.arrayPosition()+"  Length: "+r.contentLength());

            if (!mapGetBufferNumberFromTSI.containsKey(tsi)){
//                Log.d(TAG, "TSI value not found yet so skip object");
                return null;
            }

            int index=mapGetBufferNumberFromTSI.get(tsi);                               //maps to the storage buffer assigned this tsi

            String fileName;

            HashMap<Long,String> t=array_MapTOI_FileName.get(index);                     //TOI maps to a filename
            HashMap<String, ContentFileLocation> m=arrayMapFileLocations.get(index);        //and the fileName maps to the COntent location in buffer info
            if (t.containsKey(toi_tsi) && m.containsKey(t.get(toi_tsi))){
                ContentFileLocation l= m.get(t.get(toi_tsi));
                if (length<=(l.contentLength - l.nextWritePosition)){
                    System.arraycopy(input, offset, storage.get(index), l.start + l.nextWritePosition, length);
                    l.nextWritePosition+=length;
                    if (l.nextWritePosition==l.contentLength){
                        fileName=l.fileName.substring(0,l.fileName.length()-4);    //Finished so copy object to not ".new"
                        m.remove(l.fileName);
                        l.fileName=fileName;
                        Date now= Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
                        l.time=now.getTime();
                        m.put(fileName, l);
                        if (index==1){      //video
                            fluteTaskManager.callBackInterface.callBackVideoFileSize(fluteTaskManager.index(),m);
                        }
                        if (ATSC3.FAKEMANIFEST) {

                            if (fileName.contains("video") && fileName.contains(".m4s") && first) {
                                int v1 = fileName.indexOf("/", 1);
                                int v2 = fileName.indexOf(".", 1);
                                String number = fileName.substring(v1 + 1, v2);
                                videoStartNumber = Integer.parseInt(number);
                            }
                        }
                        t.remove(toi_tsi);
                        t.put(toi_tsi, fileName);
                        Log.d(TAG, "Wrote file: "+ baseDataSpec.uri.toString().concat(fileName) +" of size "+ l.contentLength + " to buffer: "+index );
                        return l;
                    }
                }else {
                    Log.e(TAG, "Attempt at buffer write overrun: "+l.fileName);
                }
            }
            return null;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }finally{
            lock.unlock();
        }

    }

    public void setMappingForTSI(int[] tsi){
        for (int i=0; i<tsi.length; i++) {
            int index=(tsi[i]==0?0:1);
            mapGetBufferNumberFromTSI.put(tsi[i], index); //TODO: Use bw to determine video if there is both audio and video
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

        if (fileName.toLowerCase().endsWith(".mpd")) {
            String mpdData="";
            if (ATSC3.FAKEMANIFEST) {
                mpdData = ATSC3.manifestContents;
            }else{
                mpdData=fluteTaskManager.sls.getManifest();
            }

            mpdData=mpdData.replaceAll("Header\\.m4s","2.m4s");             //Kluge for NAB where manifest doesn't match filename for initialization

            mMPDbytes = parseManifest(mpdData);
            contentLength=mMPDbytes.length;

            return new FileBuffer(mMPDbytes, contentLength, 0);

        }else{

            do {
                f = arrayMapFileLocations.get(index).get(fileName);
                index++;
            }while (index < arrayMapFileLocations.size() && f==null) ;

            if (f != null) {

                liveReadFromBufferTime=f.time;              //Exoplayer is reading from here so is closest time to live edge we know of
                Log.d("DebugTime".concat(baseDataSpec.uri.getHost()),"liveBufferTimeRead offset from period zero secs: "+(liveReadFromBufferTime-availabilityStartTime-periodStart)/1000.0 +
                        "  fileName: "+f.fileName);
                index--;
                return new FileBuffer(storage.get(index), f.contentLength, f.start);
            }else{
                Log.d(TAG,"Couldn't fine file while trying to open: "+fileName);
                return null;

            }
        }
    }


    long periodStart=0;

    /**
     * Manipulate the Manifest by replacing AST, changing buffering params and inserting ads
     * @param mpdData input data
     * @return changed data
     */
    private byte[] parseManifest(String mpdData){

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar c= Calendar.getInstance(timeZone);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        MPDParser mpdParser;
        if (first){
            long firstTimeOffset=0;
            mpdParser=new MPDParser(mpdData);
            mpdParser.MPDParse();

            availabilityStartTimeOffset=AVAILABILITY_TIME_OFFSET;
            availabilityStartTime=mpdParser.mpd.getAvailabilityStartTimeFromVideos(mapFileLocationsVid)+availabilityStartTimeOffset-firstTimeOffset;

            Log.d(TAG,"AvailabilityStartTime set to "+availabilityStartTime);
            first=false;

        }
        if (ATSC3.FAKEMANIFEST) {
            String replacement = "startNumber=\"".concat(String.format("%d", videoStartNumber)).concat("\"");
            mpdData = mpdData.replaceAll("startNumber=\"([0-9])+\"", replacement);

        }


        String[] mpdSplit=mpdData.split("\\?>",2);
        String mpdHeaderStart=mpdSplit[1];
        String[] mpdDataSplit=mpdHeaderStart.split(">",2);
        String mpdHeader=mpdDataSplit[0].concat(">");


        formatter.setTimeZone(timeZone);
        if (ATSC3.FAKEMANIFEST) {
            availabilityStartTimeOffset = AVAILABILITY_TIME_OFFSET;
            Date now = c.getTime();
            availabilityStartTime = now.getTime() + AVAILABILITY_TIME_OFFSET;
        }

        String availabilityStartTimeString= formatter.format(availabilityStartTime);
        Log.d(TAG, availabilityStartTimeString);

        mpdParser=new MPDParser(mpdHeader);
        mpdParser.MPDParse();
        mpdParser.mpd.getAttributes().put("minBufferTime",MIN_BUFFER_TIME);
//        mpdParser.mpd.getAttributes().put("timeShiftBufferOffset",TIME_SHIFT_BUFFER_OFFSET);
        mpdParser.mpd.getAttributes().put("timeShiftBufferDepth",TIME_SHIFT_BUFFER_DEPTH);
        mpdParser.mpd.getAttributes().put("profiles", "urn:mpeg:dash:profile:isoff-live:2011");
        mpdParser.mpd.getAttributes().put("minimumUpdatePeriod",MINIMUM_UPDATE_PERIOD);
        mpdParser.mpd.getAttributes().put("suggestedPresentationDelay",SUGGESTED_PRESENTATION_DELAY);
        mpdParser.mpd.getAttributes().put ("mediaPresentationDuration","PT1000H20M35S");
        mpdParser.mpd.getAttributes().put("availabilityStartTime",availabilityStartTimeString);
        mpdParser.mpd.getAttributes().remove("publishTime");
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
                                String start=xpp.getAttributeValue(null,"start");
                                if (xpp.getAttributeName(i).startsWith("xlink")){
                                    int indexStart=0;
                                    int indexEnd=0;
                                    String period="";
                                    for (int j=0; j<=periodNumber; j++){
                                        indexStart=mpdData.indexOf("<Period", indexEnd+9);
                                        indexEnd=mpdData.indexOf("</Period>", indexEnd+9);
                                    }
                                    if (lastAdInsertion==null || !start.equals(lastAdStart)){
                                        lastAdStart=start;
                                        start="start=\"".concat(start).concat("\"");
                                        lastAdInsertion= AdCategory.getNextAd(false);
                                        if (null!=lastAdInsertion) {
                                            lastAdInsertion.period = lastAdInsertion.period.replaceFirst("start=['|\"][PTMHS\\.0-9]+['|\"]", start);
//                                            Log.d("DebugTime".concat(baseDataSpec.uri.getHost()), "Switch ad period position: ads now in period: " + periodNumber);
                                        }
                                    }
//                                    if (periodNumber==0){
//                                        Log.d("DebugTime".concat(baseDataSpec.uri.getHost()),"Switch ad period position: ads now in period: "+periodNumber);
//                                    }
                                    if (null!=lastAdInsertion) {

                                        mpdData = mpdData.substring(0, indexStart).concat(lastAdInsertion.period).concat(mpdData.substring(indexEnd + 9, mpdData.length()));
                                    }
                                    break;
                                }else {
                                    if (lastAdInsertion != null) {


                                    }
                                }
                                if (periodNumber==0){
                                    periodStart=parseXsDuration(start);
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




}
