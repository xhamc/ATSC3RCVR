package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import com.sony.tv.app.atsc3receiver1_0.app.ATSC3;
import com.sony.tv.app.atsc3receiver1_0.app.ATSC3.*;


/**
 * Managers starting and stopping the FluteTaskManagers
 * Created by xhamc on 3/18/17.
 */

public class FluteReceiver  {


    private static final String TAG="FluteReceiver";


    private TransferListener<? super DataSource> listener;

    public FluteTaskManagerBase[] mFluteTaskManager;

    private Handler mHandler;
    public static final int TASK_ERROR=-1;
    public final int TASK_COMPLETE=1;
    public static final int TASK_STARTED=2;
    public static final int TASK_STOPPED=3;
    public static final int FOUND_FLUTE_PACKET=4;
    public final int FOUND_FLUTE_INSTANCE=5;
    public static final int FOUND_FLUTE_FILE=6;

    public long timeOffset=0;

    private static FluteReceiver sInstance=new FluteReceiver();
    public long getTimeOffset(){ return timeOffset;}


    /**
     *
     * @return instance of this singleton FluteReceiver
     */
    public static FluteReceiver getInstance(){
        return sInstance;
    }


    /**
     * FluteReceiver is singleton
     */
    private FluteReceiver() {

        this.listener=null;
        mHandler=new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                FluteTaskManagerBase flutetask= (FluteTaskManagerBase) inputMessage.obj;
                switch (inputMessage.what) {
                    // The decoding is done
//                    case TASK_COMPLETE:
//                        Log.d(TAG,"FOUND LLS MESSAGES");
//                        break;
                    case TASK_STARTED:
                        Log.d(TAG,"STARTED");
                        break;
                    case FOUND_FLUTE_PACKET:
//                        Log.d(TAG,"FOUND FLUTE PACKET: current size of locations: "+flutetask.fileManager.locations.size())
                        ;
                        break;
                    case FOUND_FLUTE_INSTANCE:
                        Log.d(TAG,"FOUND FLUTE INSTANCE");

                        break;
                    case FOUND_FLUTE_FILE:
                        break;
                    case TASK_ERROR:
//                        Log.d(TAG,"FOUND ERROR: "+flutetask.error);
                        break;
                    case TASK_STOPPED:
                        Log.d(TAG,"STOP REQUEST ISSUED");

                        break;
                    default:
                        super.handleMessage(inputMessage);

                }
            }
        };
    }

    public void setListener(TransferListener<? super DataSource> listener ){
        this.listener=listener;
    }



    /**
     * start the task managers
     */
    public void start(DataSpec signalingDataSpec, DataSpec avDataSpec, int index, int type, CallBackInterface callBackInterface) {
            if (null==avDataSpec) {
                if (type == ATSC3.QUALCOMM) {
                    if (null == mFluteTaskManager)
                        mFluteTaskManager = new FluteTaskManagerQu[2];
                    mFluteTaskManager[index] = new FluteTaskManagerQu(signalingDataSpec, callBackInterface,index);
                } else {
                    if (null == mFluteTaskManager)
                        mFluteTaskManager = new FluteTaskManagerNAB[2];
                    mFluteTaskManager[index] = new FluteTaskManagerNAB(signalingDataSpec, callBackInterface,index);
                }
                //new Thread(mFluteTaskManager[index]).start();
            }else{
                if (type == ATSC3.QUALCOMM) {
                    if (null == mFluteTaskManager)
                        mFluteTaskManager = new FluteTaskManagerQu[2];
                    mFluteTaskManager[index] = new FluteTaskManagerQu(signalingDataSpec,avDataSpec, callBackInterface,index);
                } else {
                    if (null == mFluteTaskManager)
                        mFluteTaskManager = new FluteTaskManagerNAB[2];
                    mFluteTaskManager[index] = new FluteTaskManagerNAB(signalingDataSpec, avDataSpec, callBackInterface,index);
                }
                //new Thread(mFluteTaskManager[index]).start();

            }

    }

    public void resetTimeStamp() {

        if (null!=mFluteTaskManager) {
            for (int i = 0; i < mFluteTaskManager.length; i++) {
                if (null != mFluteTaskManager[i]) {
                    if (null != mFluteTaskManager[i].fileManager()) {

                        mFluteTaskManager[i].fileManager().resetTimeStamp();
                    }
                }
            }
        }

    }

    /**
     * stop the task manager
     */
    public void stop(){
        if (null!=mFluteTaskManager){
            for (int i=0; i<mFluteTaskManager.length; i++){
                mFluteTaskManager[i].stop();

            }
        }

    }

    /**
     * handleTaskState is passes Flute Datagram packets to UI thread
     * @param task  The FluteTaskManagerQu holding the data
     * @param state The state of the task manager (error, completed ...)
     */
    public void handleTaskState(FluteTaskManagerBase task, int state){
//        Log.d(TAG,"Message to send: "+state);

        (mHandler.obtainMessage(state, task)).sendToTarget();
    }

    /**
     * Class to handle getting udp flute data and decoding it
     */







}
