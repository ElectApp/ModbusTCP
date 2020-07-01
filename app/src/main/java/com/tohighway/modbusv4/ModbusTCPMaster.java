package com.tohighway.modbusv4;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class ModbusTCPMaster {

    //Connection
    private String ip;
    private int port, timeout;
    private Socket socket;
    private TCPBytesClient tcp;
    //MODBUS
    // Function Code
    public static final int FN_READ_HOLDING = 0x03;
    public static final int FN_WRITE_SINGLE = 0x06;
    // Index on frame
    private static final int HEADER_LEN = 6;
    private static final int FRAME_MIN = HEADER_LEN + 3; //Header(6) + SlaveID(1) + Fn(1) + data(1)
    // Task
    private MbTask cReq, nextReq;
    private static String err;
    private int cTransId;
    //Other
    private Handler handler;
    private int scanRate;
    private OnResultListener onResultListener;
    private OnConnectionListener onConnectionListener;
    private static final int SCAN_DEFAULT = 1000, TIMEOUT_DEFAULT = 1000;
    private static final int BUFF_MAX = 262;
    public static final int READ_LEN_MAX = (BUFF_MAX>>1) - HEADER_LEN;
    private static final String TAG = "ModbusTCPMaster";

    public ModbusTCPMaster(@NonNull OnResultListener listener){
        onResultListener = listener;
        scanRate = SCAN_DEFAULT;
        timeout = TIMEOUT_DEFAULT;
        socket = null;
        handler = null;
        cReq = null;
        nextReq = null;
        err = null;
        cTransId = 0;
    }

    public void setOnConnectionListener(OnConnectionListener onConnectionListener){
        this.onConnectionListener = onConnectionListener;
    }

    public void setConnection(String ip, int port, int timeout){
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
    }

    public void setScanRate(int sr){
        scanRate = sr;
    }

    public void refresh(){
        cTransId = 0;
        err = null;
    }

    public void readHoldingRegister(int slaveId, int startAddress, int length){
        Log.w(TAG, "F#03 Address="+startAddress+", Len="+length);
        MbTask task = new MbTask(FN_READ_HOLDING, slaveId, new int[]{startAddress, length});
        if(cReq==null || !isConnected()){
            sendRequest(task);
        }else {
            nextReq = task;
        }
    }

    public void writeSingleRegister(int slaveId, int address, int value){
        Log.w(TAG, "F#06 Address="+address+", Value="+value);
        MbTask task = new MbTask(FN_WRITE_SINGLE, slaveId, new int[]{address, value});
        if(cReq==null || !isConnected()){
            sendRequest(task);
        }else {
            nextReq = task;
        }
    }

    public boolean isConnected(){
        return socket!=null && socket.isConnected();
    }

    public void disconnect(){
        Log.w(TAG, "Try disconnect...");
        //Stop interval reading
        if (handler!=null){
            handler.removeCallbacks(runnable);
            handler = null;
        }
        //Stop TCP
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tcp.cancel(false);
        //Clear
        cReq = null;
        nextReq = null;
        err = null;
        cTransId = 0;
        socket = null;
        //Callback
        if (onConnectionListener!=null){
            onConnectionListener.onDisconnected();
        }
    }

    private void sendRequest(@NonNull MbTask task){
        //Create MB Request Message
        byte data[] = new byte[BUFF_MAX];
        int len = 0;
        //Header
        cTransId++;
        if (cTransId>0xFFFF){ cTransId = 1; }
        data[len++] = highByte(cTransId);
        data[len++] = lowByte(cTransId);
        data[len++] = 0;
        data[len++] = 0;
        data[len++] = 0;
        data[len++] = 6; //F#3 & F#6
        //RTU frame
        data[len++] = lowByte(task.slaveId);
        data[len++] = lowByte(task.fn);
        data[len++] = highByte(task.data[0]);
        data[len++] = lowByte(task.data[0]);
        data[len++] = highByte(task.data[1]);
        data[len++] = lowByte(task.data[1]);
        //Resize
        data = Arrays.copyOf(data, len);
        //Save
        cReq = task;
        //Connect TCP, Send bytes on background
        tcp = new TCPBytesClient();
        tcp.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new MbBuffer(data));
    }

    public static byte lowByte(int v){
        return (byte)(v & 0xff);
    }

    public static byte highByte(int v){
        return (byte)(v >> 8);
    }

    public static int u16Value(int hi, int lo){
        hi = (0xFF & hi);
        lo = (0xFF & lo);
        return 0xFFFF & ((hi<<8) | lo);
    }

    public static short s16Value(int hi, int lo){
        hi = (0xFF & hi);
        lo = (0xFF & lo);
        return (short) ((hi<<8) | lo);
    }

    public static int readBit(int value, int bit){
        return (((value) >> (bit)) & 0x01);
    }

    private String getMbError(byte eCode){
        switch (eCode){
            case 0x01: return "Illegal Function";
            case 0x02: return "Illegal Data Address";
            case 0x03: return "Illegal Data Value";
            case 0x04: return "Slave Device Failure";
            case 0x05: return "Acknowledge";
            case 0x06: return "Slave Device Busy";
            case 0x0A: return "Gateway path unavailable";
            case 0x0B: return "Gateway target device failed to respond";
            default: return "Unknown Error code "+eCode;
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //Interval reading
            sendRequest(cReq);
        }
    };

    private void runInterval(){
        //Run next function in task
        if (nextReq!=null){
            sendRequest(nextReq);
            nextReq = null;
        }else {
            //Run interval reading
            if (handler==null){
                handler = new Handler();
            }else {
                handler.removeCallbacks(runnable);
            }
            handler.postDelayed(runnable, scanRate);
        }
    }

    private void checkBytesResponse(MbBuffer data){
        //Log.w(TAG, "End: "+Arrays.toString(data));
        if (data!=null){
            //byte tx[] = data.getTx();
            byte rx[] = data.getData();
            //=========== Verify ==============//
            //int id = u16Value(data[0], data[1]);
            //Log.w(TAG, "TransID = "+id+", Request = "+cTransId);
            if(rx.length<FRAME_MIN){
                //Length
                callbackError("Invalid Length of Bytes Response");
            }/*else if(u16Value(rx[0], rx[1])!=cTransId){ -> Move to check at Waiting bytes response
                //Trans ID
                callbackError("Invalid Transaction ID");
            }*/else if(rx[HEADER_LEN]!=cReq.slaveId) {
                //Slave ID
                callbackError("Invalid Slave ID");
            }else if((rx[HEADER_LEN+1]&0x7F)!=cReq.fn){
                //Function
                callbackError("Invalid Function");
            }else if(readBit(rx[HEADER_LEN+1], 7)>0){
                //Exception
                byte eCode = rx[HEADER_LEN+2];
                onResultListener.onSuccess(cTransId, cReq, new MbTask(cReq.fn,
                        cReq.slaveId, new int[]{(int)eCode}), getMbError(eCode));
            }else {
                //=========== Normal ==============//
                int rD[];
                switch (cReq.fn){
                    case FN_READ_HOLDING:
                        int expByteLen = HEADER_LEN + 3 + (cReq.data[1]<<1); //Header SlaveID + Fn + Byte Count + Data
                        if (rx.length>=expByteLen){
                            rD = new int[cReq.data[1]];
                            for (int x=0; x<cReq.data[1]; x++){
                                rD[x] = s16Value(rx[(2*x)+HEADER_LEN+3], rx[(2*x)+HEADER_LEN+4]);
                            }
                            onResultListener.onSuccess(cTransId, cReq, new MbTask(cReq.fn, cReq.slaveId, rD), null);
                        }else {
                            callbackError("Invalid Frame");
                        }
                        break;
                    case FN_WRITE_SINGLE:
                        if (rx.length>=HEADER_LEN+6){
                            rD = new int[2];
                            rD[0] = u16Value(rx[HEADER_LEN+2], rx[HEADER_LEN+3]); //Address
                            rD[1] = u16Value(rx[HEADER_LEN+4], rx[HEADER_LEN+5]); //Value
                            if(rD[0]==cReq.data[0] && rD[1]==cReq.data[1]){
                                onResultListener.onSuccess(cTransId, cReq, new MbTask(cReq.fn, cReq.slaveId, rD), null);
                            }else {
                                callbackError("Invalid Frame");
                            }
                        }else {
                            callbackError("Invalid Length of Bytes Response");
                        }
                        break;
                }
            }
            //Start interval
            runInterval();
        }else {
            //================ Error ==============//
            callbackError(err);
        }
    }

    private void callbackError(String e){
        onResultListener.onFailed(cTransId, e);
        //Retry
        if (e.equals("Timeout")){ runInterval(); }
    }

    private class TCPBytesClient extends AsyncTask<MbBuffer, String, MbBuffer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MbBuffer doInBackground(MbBuffer... bytes) {
            //Initial
            //MbBuffer buffer = new MbBuffer(bytes[0].getData(), null);
            byte tx[] = bytes[0].getData();
            byte rx[] = new byte[BUFF_MAX];
            //Action
            try {
                //Create socket
                if (socket==null){
                    Log.w(TAG, "Create TCP Socket...");
                    socket = new Socket();
                    //socket.setSoTimeout(timeout*2);   //Response timeout -> This render Invalid Transaction ID
                    socket.setKeepAlive(true);      //Keep alive for reduce delay time
                }
                //Connect
                if (!socket.isConnected()){
                    Log.w(TAG, "Try connect..."+ip+":"+port+" within "+timeout);
                    //Connecting
                    onProgressUpdate("Connecting");
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    //Connected
                    publishProgress("Connected");
                }
                //Send request bytes
                DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
                dOut.write(tx);
                dOut.flush();
                Log.w(TAG, "Send bytes ["+tx.length+"] "+Arrays.toString(tx));
                //Receive response bytes
                //DataInputStream dIn = new DataInputStream(socket.getInputStream());
                //int dLen = dIn.read(rx, 0, BUFF_MAX);
                DataInputStream dIn = new DataInputStream(socket.getInputStream());
                int dLen = 0; long ts = System.currentTimeMillis(); boolean w = true;
                //long lt = 0;
                while (w){
                    //Add bytes
                    int dLen2 = dIn.available();
                    if (dLen2>0){
                        dLen = dIn.read(rx, 0, dLen2);
                        Log.w(TAG, "Received "+dLen2+" bytes");
                        //Waiting TransID matched
                        // if we don't check at here, it will occur "Invalid Transaction ID" error
                        // Due to the DataInputStream(dIn) read a previously byte array response.
                        // This is the testing results with Aircon Home app (Modbus TCP/IP server)
                        // Send bytes [12] [0, 10, 0, 0, 0, 6, 1, 3, 0, 0, 0, 50]
                        // Received bytes [109] [0, 9, 0, 0, 0, 103, 1, 3, 100, -85,...
                        if (dLen>1 && rx[0]==tx[0] && rx[1]==tx[1]){
                            Log.w(TAG, "Transaction ID matched...");
                            break;
                        }
                        //lt = System.currentTimeMillis();
                    }
//                    //Waiting to end frame -> This render Invalid Transaction ID
//                    if (lt>0 && (System.currentTimeMillis()-lt)>3){
//                        //Log.w(TAG, "End frame");
//                        break;
//                    }
                    //Timeout?
                    w = (System.currentTimeMillis() - ts)<timeout;
                }
                //Timeout?
                if(!w){
                    Log.e(TAG, "Timeout");
                    setError("Timeout");
                }else {
                    //Resize
                    rx = Arrays.copyOf(rx, dLen);
                    Log.w(TAG, "Received bytes ["+dLen+"] "+Arrays.toString(rx));
                    //End
                    return new MbBuffer(rx);
                }
            } catch (IOException e) {
                e.printStackTrace();
                String ex = e.getMessage();
                Log.e(TAG, "TCP error: "+ex);
                //Set error
                setError(ex);
//                //Clear
//                if (socket!=null && !socket.isClosed() || socket.isConnected()){
//                    Log.w(TAG, "Clear Socket");
//                    try {
//                        socket.close();
//                    } catch (IOException e1) {
//                        e1.printStackTrace();
//                    }
//                }
            }
            return null;
        }

        private synchronized void setError(String e){
            err = e;
            if (e!=null && e.contains("failed to connect")){
                publishProgress("Failed");
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (onConnectionListener!=null){
                switch (values[0]){
                    case "Connecting": onConnectionListener.onConnecting(ip, port); break;
                    case "Connected": onConnectionListener.onConnected(true); break;
                    case "Failed": onConnectionListener.onConnected(false); break;
                }
            }
        }

        @Override
        protected void onPostExecute(MbBuffer aByte) {
            super.onPostExecute(aByte);
            if (aByte!=null){
                Log.w(TAG, "Success...");
                checkBytesResponse(aByte);
            }else {
                Log.e(TAG, "Error: "+err);
                checkBytesResponse(null);
            }
        }

    }

    public interface OnResultListener{
        void onSuccess(int transId, MbTask request, MbTask response, String mbException);
        void onFailed(int transId, String err);
    }

    public interface OnConnectionListener{
        void onConnecting(String ip, int port);
        void onConnected(boolean connected);
        void onDisconnected();
    }


}
