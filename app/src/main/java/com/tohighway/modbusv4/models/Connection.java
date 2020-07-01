package com.tohighway.modbusv4.models;

public class Connection {

    public String ip;
    public int port, timeout;

    public Connection(String ip, int port, int timeout){
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
    }

}
