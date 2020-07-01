package com.tohighway.modbusv4;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.tohighway.modbusv4.models.Connection;
import com.tohighway.modbusv4.models.ReadWriteDefinition;

public class OnDevice {
    //File name
    private static final String READ_WRITE_FILE = "read_write_file";
    private static final String CONNECT_FILE = "connection_file";
    //Key
    private static final String IP_ADDRESS = "ipAddress";
    private static final String PORT = "Port";
    private static final String TIMEOUT = "Timeout";
    private static final String ADDRESS = "Address";
    private static final String QUANTITY = "Quantity";
    private static final String SLAVE_ID = "SlaveId";
    private static final String SCAN_RATE = "ScanRate";
    private static final String DISPLAY_TYPE = "DisplayType";


    private static SharedPreferences getShared(Context context, String filename){
        return context.getSharedPreferences(CONNECT_FILE, Context.MODE_PRIVATE);
    }

    public static void saveConnection(@NonNull Context context, @NonNull Connection connection){
        SharedPreferences.Editor editor = getShared(context, CONNECT_FILE).edit();
        editor.putString(IP_ADDRESS, connection.ip);
        editor.putInt(PORT, connection.port);
        editor.putInt(TIMEOUT, connection.timeout);
        editor.apply();
    }

    public static Connection getConnection(@NonNull Context context){
        SharedPreferences preferences = getShared(context, CONNECT_FILE);
        String ip = preferences.getString(IP_ADDRESS, "192.168.43.1");
        int port = preferences.getInt(PORT, 5020);
        int timeout = preferences.getInt(TIMEOUT, 1000);
        return new Connection(ip, port, timeout);
    }

    public static void saveReadWriteDefinition(@NonNull Context context, @NonNull ReadWriteDefinition def){
        SharedPreferences.Editor editor = getShared(context, READ_WRITE_FILE).edit();
        editor.putInt(ADDRESS, def.address);
        editor.putInt(QUANTITY, def.quantity);
        editor.putInt(SLAVE_ID, def.slaveId);
        editor.putInt(SCAN_RATE, def.scanRate);
        editor.putInt(DISPLAY_TYPE, def.displayType);
        editor.apply();
    }

    public static ReadWriteDefinition getReadWriteDefinition(@NonNull Context context){
        SharedPreferences preferences = getShared(context, READ_WRITE_FILE);
        int addr = preferences.getInt(ADDRESS, 0);
        int len = preferences.getInt(QUANTITY, 10);
        int id = preferences.getInt(SLAVE_ID, 1);
        int sr = preferences.getInt(SCAN_RATE, 1000);
        int dt = preferences.getInt(DISPLAY_TYPE, 0);
        return new ReadWriteDefinition(addr, len, id, sr, dt);
    }


}
