package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;

import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3.*;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xhamc on 3/22/17.
 */

public class FluteTaskManagerNAB implements FluteTaskManagerBase  {



    private static final String TAG="FluteTaskManagerQu";

    private FluteTaskManagerNAB mFluteTaskManager;
    public String error;
    Boolean stopRequest;
    public DataSpec signalingDataSpec;
    public DataSpec avDataSpec;
    //    public FluteFileManagerQu fileManager=FluteFileManagerQu.getInstance();
    public FluteFileManagerNAB fileManager;

    private UdpDataSource udpDataSourceAv;
    private byte[] bytes;
    private static int MAX_SOCKET_TIMEOUT=0;
    private FluteReceiver sInstance=FluteReceiver.getInstance();
    public CallBackInterface callBackInterface;

    private boolean manifestFound=false;
    private boolean usbdFound=false;
    private boolean stsidFound=false;
    private boolean first=true;
    private int index;

    private ReentrantLock lock = new ReentrantLock();



    public FluteTaskManagerNAB (DataSpec signalingDataSpec, CallBackInterface callBackInterface,  int index){      //Run for signalling
        mFluteTaskManager=this;
        this.index=index;
        this.callBackInterface=callBackInterface;
        stopRequest=false;
        this.signalingDataSpec=signalingDataSpec;
        fileManager=new FluteFileManagerNAB(signalingDataSpec,this);
        new Thread(new RunUpdonThread(signalingDataSpec)).start();
    }

    public FluteTaskManagerNAB (DataSpec signalingDataSpec, DataSpec avDataSpec, CallBackInterface callBackInterface, int index){
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

            fileManager=new FluteFileManagerNAB(signalingDataSpec);

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
                String fileName;
                RouteDecodeNAB routeDecode = new RouteDecodeNAB(bytes, packetSize);
                try {
                    if (routeDecode.valid())

                        fileName = fileManager.write(routeDecode, bytes, RouteDecodeNAB.PAYLOAD_START_POSITION, packetSize - RouteDecodeNAB.PAYLOAD_START_POSITION);

                    else
                        Log.d(TAG, "Invalid Route decode");

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            } finally {
            }
        }


    }



    private String lookUpFileName(int toi, int tsi){
        return "";
    }
    private String generateFileName(int toi, int tsi){
        return "";
    }

    public void stop(){
        stopRequest=true;
        sInstance.handleTaskState(this, FluteReceiver.TASK_STOPPED);

    }

    public FluteFileManagerBase fileManager(){
        return this.fileManager;
    }

    public boolean isManifestFound(){ return this.fileManager.manifestFound;}
    public boolean isUsbdFound(){ return this.fileManager.usbdFound;}
    public boolean isSTSIDFound(){ return this.fileManager.stsidFound;}
    public boolean isFirst(){ return first;}
    public int index(){return index;}




    private void reportError(){
        stopRequest=true;
        sInstance.handleTaskState(this, FluteReceiver.TASK_ERROR);
    }




}
