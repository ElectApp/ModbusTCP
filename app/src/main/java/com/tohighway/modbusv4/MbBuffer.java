package com.tohighway.modbusv4;

public class MbBuffer {

    private byte data[];
    private byte tx[], rx[];

    public MbBuffer(byte data[]){
        this.data = data;
    }

    public MbBuffer(byte tx[], byte rx[]){
        this.tx = tx;
        this.rx = rx;
    }

    public void setRx(byte[] rx) {
        this.rx = rx;
    }

    public void setTx(byte[] tx) {
        this.tx = tx;
    }

    public byte[] getRx() {
        return rx;
    }

    public byte[] getTx() {
        return tx;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

}
