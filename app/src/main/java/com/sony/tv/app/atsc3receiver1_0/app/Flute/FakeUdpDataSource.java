package com.sony.tv.app.atsc3receiver1_0.app.Flute;

import android.net.Uri;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.UdpDataSource;
import com.sony.tv.app.atsc3receiver1_0.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

/**
 * Uses wireshark captured binary data to fake streaming to the filemanager
 * Created by xhamc on 4/3/17.
 */

public class FakeUdpDataSource implements DataSource{


    /**
     * Thrown when an error is encountered when trying to read from a {@link UdpDataSource}.
     */
    public static final class UdpDataSourceException extends IOException {

        public UdpDataSourceException(IOException cause) {
            super(cause);
        }

    }

    /**
     * The default maximum datagram packet size, in bytes.
     */
    public static final int DEFAULT_MAX_PACKET_SIZE = 2000;

    /**
     * The default socket timeout, in milliseconds.
     */
    public static final int DEAFULT_SOCKET_TIMEOUT_MILLIS = 8 * 1000;

    private final TransferListener<? super FakeUdpDataSource> listener;
    private final int socketTimeoutMillis;
    private final byte[] packetBuffer;
    private final DatagramPacket packet;

    private Uri uri;
    private DatagramSocket socket;
    private MulticastSocket multicastSocket;
    private InetAddress address;
    private InetSocketAddress socketAddress;
    private boolean opened;

    private int packetRemaining;

    private InputStream is;
    private long packetSendTime=0;
    private long packetStartTime=0;
    private long packetNumber=0;

    private boolean isAV=false;

    private double PACKETDELAY;

    /**
     * @param listener An optional listener.
     */
    public FakeUdpDataSource(TransferListener<FakeUdpDataSource> listener, boolean type ) {
        this(listener);
        this.isAV=type;

        if (type){
            PACKETDELAY=59975.0*1472.0/37163136;
        }else{
            PACKETDELAY=250;
        }
    }

    /**
     * @param listener An optional listener.
     */
    public FakeUdpDataSource(TransferListener<FakeUdpDataSource> listener) {
        this(listener, DEFAULT_MAX_PACKET_SIZE);
    }

    /**
     * @param listener An optional listener.
     * @param maxPacketSize The maximum datagram packet size, in bytes.
     */
    public FakeUdpDataSource(TransferListener<FakeUdpDataSource> listener, int maxPacketSize) {
        this(listener, maxPacketSize, DEAFULT_SOCKET_TIMEOUT_MILLIS);
    }

    /**
     * @param listener An optional listener.
     * @param maxPacketSize The maximum datagram packet size, in bytes.
     * @param socketTimeoutMillis The socket timeout in milliseconds. A timeout of zero is interpreted
     *     as an infinite timeout.
     */
    public FakeUdpDataSource(TransferListener<FakeUdpDataSource> listener, int maxPacketSize,
                             int socketTimeoutMillis) {
        super();
        this.listener = listener;
        this.socketTimeoutMillis = socketTimeoutMillis;
        packetBuffer = new byte[maxPacketSize];
        packet = new DatagramPacket(packetBuffer, 0, maxPacketSize);
    }

    @Override
    public long open(DataSpec dataSpec) throws UdpDataSource.UdpDataSourceException {
        uri = dataSpec.uri;
        String host = uri.getHost().concat(".bin");
        try {
            is=MainActivity.activity.getAssets().open(host);
        } catch (IOException e) {
            e.printStackTrace();
        }
        opened = true;
        if (listener != null) {
            listener.onTransferStart(this, dataSpec);
        }
        packetStartTime= System.currentTimeMillis();
        packetNumber++;
        packetSendTime=packetStartTime+(long)(packetNumber*PACKETDELAY);
        return C.LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws UdpDataSource.UdpDataSourceException {

        while(System.currentTimeMillis()<packetSendTime){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        packetNumber++;
        packetSendTime=packetStartTime+ (long)(packetNumber*PACKETDELAY);
        if (readLength == 0) {
            return 0;
        }
        int bytesToRead=0;
        if (isAV)
        {
            try {
                int len=is.read(packetBuffer,0,36);
                if (len<0){
                    throw new Exception("End of Stream");
                }

                if (packetBuffer[0]!=0x12){
                    throw new Exception("First byte is wrong: "+packetBuffer[0]);
                }

                int file_len =  ((packetBuffer[20]&0xff) <<24 | (packetBuffer[21] &0xff)<<16 | (packetBuffer[22] &0xff) <<8 | (packetBuffer[23] & 0xff));
                int position =  ((packetBuffer[32]&0xff) <<24 | (packetBuffer[33] &0xff)<<16 | (packetBuffer[34] &0xff) <<8 | (packetBuffer[35] & 0xff));
                if ((file_len-position)<1472-36){
                    packetRemaining=(file_len-position);
                    bytesToRead=packetRemaining+36;
                }else{
                    bytesToRead=1472;
                    packetRemaining=bytesToRead-36;
                }
                len=is.read(packetBuffer, 36, packetRemaining);
                if (len<0){
                    throw new Exception("End of Stream");
                }

                System.arraycopy(packetBuffer, 0, buffer, offset, bytesToRead);

            } catch (Exception e) {
                e.printStackTrace();
                close();
            }
        }else{
            try {
                int len=is.read(packetBuffer, 0, 1);
                if (len<0){
                    throw new Exception("End of Stream");
                }
                if (packetBuffer[0]==0x01){
                    bytesToRead=276;
                }else{
                    bytesToRead=131;
                }
                packetRemaining=bytesToRead-1;
                len=is.read(packetBuffer, 1, packetRemaining);
                if (len<0){
                    throw new Exception("End of Stream");
                }
                System.arraycopy(packetBuffer, 0, buffer, offset, bytesToRead);


            } catch (Exception e) {
                e.printStackTrace();
                close();
            }
        }

        if (listener != null) {
            listener.onBytesTransferred(this, bytesToRead);
        }

        return bytesToRead;
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
