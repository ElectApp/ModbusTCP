package com.tohighway.modbusv4;

public class SimpleDisplayItem {
    private int number;
    private String value;
    private byte type;

    public SimpleDisplayItem(int number, String value, byte type){
        this.number = number;
        this.value = value;
        this.type = type;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public byte getType(){ return type; }

    public void setType(byte type) { this.type = type; }
}
