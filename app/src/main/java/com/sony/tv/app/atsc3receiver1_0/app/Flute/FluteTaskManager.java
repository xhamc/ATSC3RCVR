package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xhamc on 5/9/17.
 */

public class FluteTaskManager{




    private static final String TAG="FluteTaskManager";

    private FluteTaskManager mFluteTaskManager;
    public String error;
    Boolean stopRequest;
    public DataSpec signalingDataSpec;
    public DataSpec avDataSpec;
    //    public FluteFileManagerQu fileManager=FluteFileManagerQu.getInstance();
    public FluteFileManager fileManager;

    private UdpDataSource udpDataSourceAv;
    private byte[] bytes;
    private static int MAX_SOCKET_TIMEOUT=0;
    private FluteReceiver sInstance=FluteReceiver.getInstance();
    public ATSC3.CallBackInterface callBackInterface;

    private boolean manifestFound=false;
    private boolean usbdFound=false;
    private boolean stsidFound=false;
    private boolean first=true;
    private int index;

    public SLS sls=new SLS();


    private ReentrantLock lock = new ReentrantLock();



    public FluteTaskManager (DataSpec signalingDataSpec, ATSC3.CallBackInterface callBackInterface, int index){      //Run for signalling
        mFluteTaskManager=this;
        this.index=index;
        this.callBackInterface=callBackInterface;
        stopRequest=false;
        this.signalingDataSpec=signalingDataSpec;
        fileManager=new FluteFileManager(signalingDataSpec,this);
        new Thread(new RunUpdonThread(signalingDataSpec)).start();
    }

    public FluteTaskManager (DataSpec signalingDataSpec, DataSpec avDataSpec, ATSC3.CallBackInterface callBackInterface, int index){
        mFluteTaskManager=this;
        this.index=index;
        first=false;
        manifestFound=false;
        usbdFound=false;
        stsidFound=false;
        this.callBackInterface=callBackInterface;
        this.signalingDataSpec=signalingDataSpec;
        this.avDataSpec=avDataSpec;
        stopRequest=false;
        if (signalingDataSpec.uri.toString().equals(avDataSpec.uri.toString())){

            fileManager=new FluteFileManager(signalingDataSpec);

            new Thread(new RunUpdonThread(signalingDataSpec)).start();

        }else {
//            fileManager = new FluteFileManagerNAB(signalingDataSpec, avDataSpec);
//            fileManager.reset();
//
//            new Thread(new RunUpdonThread(signalingDataSpec)).start();
//            new Thread(new RunUpdonThread(avDataSpec)).start();
            Log.e(TAG, "Cannot handle different ip addresses between signaling and av");

        }

//        fileManager.reset();


    }


    private class RunUpdonThread implements Runnable{

        DataSpec dataSpec;
        int packetSize=0;
        private boolean running;


        public RunUpdonThread(DataSpec dataSpec){
            this.dataSpec=dataSpec;

        }

        @Override
        public void run() {

            if (ATSC3.FAKEUDPSOURCE) {


                FakeUdpDataSource udpDataSource;
                int len, offset;
//            udpDataSource = new FakeUdpDataSource( new TransferListener<FakeUdpDataSource>() {

                udpDataSource = new FakeUdpDataSource(new TransferListener<FakeUdpDataSource>() {
                    @Override
                    public void onTransferStart(FakeUdpDataSource source, DataSpec dataSpec) {
                        running = true;
                    }

                    @Override
                    public void onBytesTransferred(FakeUdpDataSource source, int bytesTransferred) {
                        packetSize = bytesTransferred;
                    }

                    @Override
                    public void onTransferEnd(FakeUdpDataSource source) {
                        running = false;
                    }
                }, true

//                    UdpDataSource.DEFAULT_MAX_PACKET_SIZE,
//                    MAX_SOCKET_TIMEOUT
                );

                try {
                    udpDataSource.open(dataSpec);
                } catch (UdpDataSource.UdpDataSourceException e) {
                    e.printStackTrace();
                    return;
                }
                mainloop:
                while (!stopRequest && running) {

                    offset = 0;
                    bytes = new byte[UdpDataSource.DEFAULT_MAX_PACKET_SIZE];
                    do {
                        try {
                            len = udpDataSource.read(bytes, offset, UdpDataSource.DEFAULT_MAX_PACKET_SIZE);
                            offset += len;
                        } catch (UdpDataSource.UdpDataSourceException e) {
                            e.printStackTrace();
//                        reportError();
                            break mainloop;
                        }
                    } while (offset < packetSize);
                    transferDataToFluteHandler(bytes, packetSize);

                }
                udpDataSource.close();
                callBackInterface.callBackFluteStopped(index);


            }else {  //NOT FAKE, REAL

                UdpDataSource udpDataSource;
                int len, offset;

                udpDataSource = new UdpDataSource(new TransferListener<UdpDataSource>() {
                    @Override
                    public void onTransferStart(UdpDataSource source, DataSpec dataSpec) {
                        running = true;
                    }

                    @Override
                    public void onBytesTransferred(UdpDataSource source, int bytesTransferred) {
                        packetSize = bytesTransferred;
                    }

                    @Override
                    public void onTransferEnd(UdpDataSource source) {
                        running = false;
                    }
                },
                        UdpDataSource.DEFAULT_MAX_PACKET_SIZE,
                        MAX_SOCKET_TIMEOUT
                );

                try {
                    udpDataSource.open(dataSpec);
                } catch (UdpDataSource.UdpDataSourceException e) {
                    e.printStackTrace();
                    return;
                }
                mainloop:
                while (!stopRequest && running) {

                    offset = 0;
                    bytes = new byte[UdpDataSource.DEFAULT_MAX_PACKET_SIZE];
                    do {
                        try {
                            len = udpDataSource.read(bytes, offset, UdpDataSource.DEFAULT_MAX_PACKET_SIZE);
                            offset += len;
                        } catch (UdpDataSource.UdpDataSourceException e) {
                            e.printStackTrace();
//                        reportError();
                            break mainloop;
                        }
                    } while (offset < packetSize);
                    transferDataToFluteHandler(bytes, packetSize);

                }
                udpDataSource.close();
                callBackInterface.callBackFluteStopped(index);
            }
        }

        private void transferDataToFluteHandler(byte[] bytes, int packetSize) {

            try {
                sInstance.handleTaskState(mFluteTaskManager, FluteReceiver.FOUND_FLUTE_PACKET);
                ContentFileLocation contentFileLocation;
                String fileName;
                RouteDecode routeDecode = new RouteDecode(bytes, packetSize);
                try {
                    if (routeDecode.valid()) {
                        if (routeDecode.type() == RouteDecode.EXT_FDT) {

                            fileName = routeDecode.fileName();
                            Log.d(TAG, "Found file: " + fileName);
                            routeDecode.fileName(routeDecode.fileName().concat(".new"));
                            fileManager.create(routeDecode);

                        } else if (routeDecode.type() == RouteDecode.NONE) {

                            contentFileLocation = fileManager.write(routeDecode, bytes, RouteDecode.EXT_NONE_PAYLOAD_START_POSITION, packetSize - RouteDecode.EXT_NONE_PAYLOAD_START_POSITION);
                            if (null!=contentFileLocation && (contentFileLocation.fileName.toLowerCase().contains(".mpd") ||
                                    contentFileLocation.fileName.toLowerCase().contains("usbd.xml") ||
                                    contentFileLocation.fileName.toLowerCase().contains("s-tsid.xml"))) {
                                sls.create(contentFileLocation);
                                STSIDParser s=sls.getSTSIDParse();
                                if (null!=s) {
                                    int[] tsiMapping = new int[s.getLSSize()];
                                    for (int i = 0; i < tsiMapping.length; i++) {
                                        tsiMapping[i] = sls.getSTSIDParse().getTSI(i);
                                    }
                                    fileManager.setMappingForTSI(tsiMapping);
                                }
                            }

                        } else if (routeDecode.type() == RouteDecode.EXT_FTI) {

                            if (routeDecode.tsi() == 0  && routeDecode.arrayPosition()==0) {
                                if (routeDecode.toi()!=0) {                                                            //signaling except ignore efdt instance
                                    routeDecode.fileName("sls.xml.new");
                                    fileManager.create(routeDecode);
                                }else{
                                    return;
                                }
                            }else if (routeDecode.toi() == 0xFFFFFFFF && routeDecode.tsi() != 0 && routeDecode.arrayPosition()==0) {       //init file
                                routeDecode.fileName(generateInitFileName(routeDecode.tsi()));
                                if (!("").equals(routeDecode.fileName())) {
                                    routeDecode.fileName(routeDecode.fileName().concat(".new"));
                                    fileManager.create(routeDecode);
                                }
                            } else if (routeDecode.arrayPosition()==0) {                                               //media file
                                routeDecode.fileName(generateFileName(routeDecode.toi(), routeDecode.tsi()));
                                if (!("").equals(routeDecode.fileName())) {
                                    routeDecode.fileName (routeDecode.fileName().concat(".new"));
                                    fileManager.create(routeDecode);
                                }
                            }
                            contentFileLocation = fileManager.write(routeDecode, bytes, RouteDecode.EXT_FTI_PAYLOAD_START_POSITION, packetSize - RouteDecode.EXT_FTI_PAYLOAD_START_POSITION);

                            if (null!=contentFileLocation && contentFileLocation.fileName.equals("sls.xml")){
                                sls.create(contentFileLocation);
                                STSIDParser s=sls.getSTSIDParse();
                                if (null!=s) {
                                    int[] tsiMapping = new int[s.getLSSize()];
                                    for (int i = 0; i < tsiMapping.length; i++) {
                                        tsiMapping[i] = sls.getSTSIDParse().getTSI(i);
                                    }
                                    fileManager.setMappingForTSI(tsiMapping);
                                }
                            }
                        }
                    }



                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }finally {
            }


        }


    }




    public void stop(){
        stopRequest=true;
        sInstance.handleTaskState(this, FluteReceiver.TASK_STOPPED);

    }

    public FluteFileManager fileManager(){
        return this.fileManager;
    }

    public boolean isManifestFound(){ return manifestFound;}
    public boolean isUsbdFound(){ return usbdFound;}
    public boolean isSTSIDFound(){ return stsidFound;}
    public boolean isFirst(){ return first;}
    public int index(){return index;}




    private void reportError(){
        stopRequest=true;
        sInstance.handleTaskState(this, FluteReceiver.TASK_ERROR);
    }



    private String generateFileName(int toi, int tsi){

        if (null!=sls.getSTSIDParse()){

            String template=sls.mapTSItoFileTemplate.get(tsi);
            if (null!=template){
                String toi$ = String.format("%d", toi);
                template=template.replaceAll("\\$TOI\\$", toi$);
                if (!template.startsWith("/"))
                    template="/".concat(template);
                return template;
            }

        }
        return "";
    }

    private String generateInitFileName(int tsi){

        if (null!=sls.getSTSIDParse()){

            String template=sls.mapTSItoFileInitTemplate.get(tsi);
            if (null!=template) {
                if (!template.startsWith("/"))
                    template="/".concat(template);
                return template;
            }

        }
        return "";
    }


    public class SLS{

        private String manifest="";
        private String usbd="";
        private String stsid="";
        private String fileTemplate="";
        private STSIDParser stsidParsed;
        private ContentFileLocation slsLocation;
        public HashMap<Integer,String> mapTSItoFileTemplate=new HashMap<>();
        public HashMap<Integer,String> mapTSItoFileInitTemplate=new HashMap<>();

        public void create(ContentFileLocation contentFileLocation){
            slsLocation=contentFileLocation;
            String sls=new String(contentFileLocation.storage,contentFileLocation.start,contentFileLocation.contentLength);
            if (extractManifest(sls)){
                manifestFound=true;
                callBackInterface.callBackManifestFound(index());

                //TODO create a manifest file in buffer
            }
            if (extractUSBD(sls)){
                usbdFound=true;
                callBackInterface.callBackUSBDFound(index());


                //TODO create a usbd file in buffer
            }
            if (extractSTSID(sls)){
                stsidFound=true;
                callBackInterface.callBackSTSIDFound(index());
            }

        }


        public String getManifest(){return manifest;}
        public String getUSBD(){
            return usbd;
        }
        public String getSTSID(){
            return stsid;
        }
        public STSIDParser getSTSIDParse(){
            return stsidParsed;
        }

        public void mapTSIToFileTemplate(){
            for (int i=0; i<stsidParsed.getLSSize(); i++){
                mapTSItoFileTemplate.put(stsidParsed.getTSI(i),stsidParsed.getFileTempate(i));
            }
        }

        public void mapTSIToInitTemplate(){
            for (int i=0; i<stsidParsed.getLSSize(); i++){
                mapTSItoFileInitTemplate.put(stsidParsed.getTSI(i),stsidParsed.getFileInitTempate(i));
            }
        }

        private boolean extractManifest(String sls){
            if (sls.contains("<MPD") && sls.contains("/MPD>")){
                int start=sls.indexOf("<MPD");
                int end=sls.indexOf("/MPD>")+5;
                manifest=("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").concat(sls.subSequence(start,end).toString());

                return true;
            }
            return false;
        }
        private boolean extractUSBD (String sls){
            if (sls.contains("<bundleDescriptionROUTE") && sls.contains("/bundleDescriptionROUTE>")){
                int start=sls.indexOf("<bundleDescriptionROUTE");
                int end=sls.indexOf("/bundleDescriptionROUTE>")+24;
                usbd= ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").concat(sls.subSequence(start,end).toString());
                return true;
            }
            return false;


        }
        private boolean extractSTSID(String sls){
            if (sls.contains("<S-TSID") && sls.contains("/S-TSID>")){
                int start=sls.indexOf("<S-TSID");
                int end=sls.indexOf("/S-TSID>")+8;
                stsid= ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").concat(sls.subSequence(start,end).toString());
                stsidParsed=new STSIDParser(stsid);
                mapTSIToFileTemplate();
                mapTSIToInitTemplate();

                return true;
            }
            return false;
        }


    }




}
