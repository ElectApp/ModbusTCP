package com.tohighway.modbusv4.models;

public class ReadWriteDefinition {

    public int address, quantity, slaveId, scanRate, displayType;

    public ReadWriteDefinition(int address, int quantity, int slaveId, int scanRate, int displayType){
        this.address = address;
        this.quantity = quantity;
        this.slaveId = slaveId;
        this.scanRate = scanRate;
        this.displayType = displayType;
    }



}
