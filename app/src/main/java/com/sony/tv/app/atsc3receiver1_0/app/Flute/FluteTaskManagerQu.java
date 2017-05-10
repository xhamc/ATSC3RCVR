package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;


import com.sony.tv.app.atsc3receiver1_0.app.ATSC3.*;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages running the flute receivers on their own threads.
 * Created by xhamc on 3/22/17.
 */

public class FluteTaskManagerQu implements FluteTaskManagerBase{

    private static final String TAG="FluteTaskManagerQu";

    private FluteTaskManagerQu mFluteTaskManagerQu;
    public String error;
    boolean stopRequest;
    public DataSpec signalingDataSpec;
    public DataSpec avDataSpec;
//    public FluteFileManagerQu fileManager=FluteFileManagerQu.getInstance();
    public FluteFileManagerBase fileManager;

    private UdpDataSource udpDataSource;
    private UdpDataSource udpDataSourceAv;
    private byte[] bytes;
    private static int MAX_SOCKET_TIMEOUT=20*1000;
    private FluteReceiver sInstance=FluteReceiver.getInstance();
    public CallBackInterface callBackInterface;

    private boolean manifestFound=false;
    private boolean usbdFound=false;
    private boolean stsidFound=false;
    private boolean first=true;
    private int index;

    private ReentrantLock lock = new ReentrantLock();



    public FluteTaskManagerQu(DataSpec signalingDataSpec, CallBackInterface callBackInterface, int index){      //Run for signalling
        mFluteTaskManagerQu =this;
        this.index=index;
        this.callBackInterface=callBackInterface;
        stopRequest=false;
        this.signalingDataSpec=signalingDataSpec;
        fileManager=new FluteFileManagerQu(signalingDataSpec, this);
        fileManager.reset();
        new Thread(new RunUdponThread(signalingDataSpec)).start();
    }

    public FluteTaskManagerQu(DataSpec signalingDataSpec, DataSpec avDataSpec, CallBackInterface callBackInterface, int index){
        mFluteTaskManagerQu =this;
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

            fileManager=new FluteFileManagerQu(signalingDataSpec, this);
            fileManager.reset();

            new Thread(new RunUdponThread(signalingDataSpec)).start();

        }else {
            fileManager = new FluteFileManagerQu(signalingDataSpec, avDataSpec, this);
            fileManager.reset();

            new Thread(new RunUdponThread(signalingDataSpec)).start();
            new Thread(new RunUdponThread(avDataSpec)).start();

        }

        fileManager.reset();


    }


    private class RunUdponThread implements Runnable{

        DataSpec dataSpec;
        int packetSize=0;
        private boolean running;


        public RunUdponThread(DataSpec dataSpec){
            this.dataSpec=dataSpec;

        }

        @Override
        public void run(){
            UdpDataSource udpDataSource;
            int len,offset;

            udpDataSource = new UdpDataSource(new TransferListener<UdpDataSource>() {
                @Override
                public void onTransferStart(UdpDataSource source, DataSpec dataSpec) {
                    running=true;
                }

                @Override
                public void onBytesTransferred(UdpDataSource source, int bytesTransferred) {
                    packetSize=bytesTransferred;
                }
                @Override
                public void onTransferEnd(UdpDataSource source) {
                    running=false;
                }
            },
                    UdpDataSource.DEFAULT_MAX_PACKET_SIZE,
                    0
            );

            try {
                udpDataSource.open(dataSpec);
            } catch (UdpDataSource.UdpDataSourceException e) {
                e.printStackTrace();
                return;
            }
            mainloop:while (!stopRequest && running) {

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
        private void transferDataToFluteHandler(byte[] bytes, int packetSize ) {
            try {
                sInstance.handleTaskState(mFluteTaskManagerQu, FluteReceiver.FOUND_FLUTE_PACKET);
                String fileName;
                RouteDecodeBase routeDecode = new RouteDecodeQu(bytes, packetSize);
                try {
                    if (routeDecode.toi() == 0 && routeDecode.tsi() == 0) {
                        fileName = routeDecode.fileName();
                        if (fileName.toLowerCase().contains(".mpd") || fileName.toLowerCase().contains("usbd.xml") || fileName.toLowerCase().contains("s-tsid.xml")) {
                            Log.d(TAG, "Found file: " + fileName);

                            fileManager.create(routeDecode);
                        } else {
                            Log.e(TAG, "Unrecognized fileName: " + fileName);
                        }
                    } else if (routeDecode.toi() == 0) {
                        fileManager.create(routeDecode);
                    } else {
                        fileName = fileManager.write(routeDecode, bytes, RouteDecodeQu.PAYLOAD_START_POSITION, packetSize - RouteDecodeQu.PAYLOAD_START_POSITION);
                        if (!fileName.equals("")) {
                            if (fileName.toLowerCase().contains(".mpd")) {
                                manifestFound = true;
                                callBackInterface.callBackManifestFound(index);
                            } else if (fileName.toLowerCase().contains("usbd.xml")) {
                                usbdFound = true;
                                callBackInterface.callBackUSBDFound(index);
                            } else if (fileName.toLowerCase().contains("s-tsid.xml")) {
                                stsidFound = true;
                                callBackInterface.callBackSTSIDFound(index);
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

    public FluteFileManagerBase fileManager(){
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




}
