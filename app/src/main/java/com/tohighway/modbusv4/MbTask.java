package com.tohighway.modbusv4;

public class MbTask {

    public int fn;
    public int slaveId;
    public int data[];

    public MbTask(int fn, int slaveId, int data[]){
        this.fn = fn;
        this.slaveId = slaveId;
        this.data = data;
    }


}
