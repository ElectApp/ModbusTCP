package com.tohighway.modbusv4;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zgkxzx.modbus4And.requset.ModbusParam;
import com.zgkxzx.modbus4And.requset.ModbusReq;
import com.zgkxzx.modbus4And.requset.OnRequestBack;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    //For connection dialog
    EditText ipAddress;
    EditText port;
    EditText timeOut;
    Button connectionBtn;
    private String ipSet;
    private String portSet;
    private String timeOutSet;
    TextView statusTitle;
    private boolean initialSuccess;
    private boolean initialFlag;
    //For internal storage
    String ipFileName = "ipStorage";
    String portFileName = "portStorage";
    String timeOutFileName = "timeOutStorage";
    //File setup name
    String []fileSetup = new String[]{"Address", "Length", "Slave", "ScanRate"};
    //For setup dialog
    EditText address;
    EditText length;
    EditText slave;
    EditText scanRate;
    Button setupBtn;
    private String addressSet;
    private String lengthSet;
    private String slaveSet;
    private String scanRateSet;
    TextView addressText;
    TextView lengthText;
    TextView slaveText;
    TextView scanRateText;
    private static boolean setupFlag;
    //For interface register data
    CountDownTimerForUpdate updateTimer;
    private long totalCountdownTime = 3600000; //set total count down timer (ms unit) ex. set to 60 minute
    private long intervalTime; //interval time(ms unit) for onTick()
    ListView dataList;
    ArrayList<String> items;
    ArrayAdapter<String> adapter;
    private short[] readBuffer;
    private boolean readSuccess;
    EditText value;
    private String valueSet;
    private boolean writeSuccess;
    private int writeAddress;
    private short countFail;
    private boolean reWriteFlag;
    private boolean disconnect = false;
    //For control working
    Button connectBtn;
    Button disconnectBtn;
    //For progress dialog
    private ProgressDialog progressDialog;

    String TAG = "Modbus";
    String TAG_P = "Main1";
    String TAG_C = "ClickList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Connection
        statusTitle = (TextView)findViewById(R.id.status);
        connectionBtn = (Button)findViewById(R.id.connection_btn);
        connectionBtn.setOnClickListener(mConnection);
        //Setup
        setupBtn = (Button)findViewById(R.id.setup_btn);
        setupBtn.setOnClickListener(mSetup);
        addressText = (TextView)findViewById(R.id.address_set);
        lengthText = (TextView)findViewById(R.id.length_set);
        slaveText = (TextView)findViewById(R.id.textView2);
        scanRateText = (TextView)findViewById(R.id.scan_rate_set);
        //Control
        connectBtn = (Button)findViewById(R.id.connect_btn);
        disconnectBtn = (Button)findViewById(R.id.disconnect_btn);
        connectBtn.setOnClickListener(mConnect);
        disconnectBtn.setOnClickListener(mDisconnect);
        //Data Interface with user
        dataList = (ListView)findViewById(R.id.data_list);
        //Transfer to arrayList
        items = new ArrayList<>();
        adapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_activated_1, items);
        dataList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dataList.setAdapter(adapter);

        //Read setup from internal storage
        addressSet = readInternalStorage(fileSetup[0]);
        lengthSet = readInternalStorage(fileSetup[1]);
        slaveSet = readInternalStorage(fileSetup[2]);
        scanRateSet = readInternalStorage(fileSetup[3]);

        if (addressSet.equals("")||lengthSet.equals("")||slaveSet.equals("")||scanRateSet.equals("")){
            addressSet = "0";
            lengthSet = "10";
            slaveSet = "1";
            scanRateSet = "1000";
        }

        //Setup data Text
        addressText.setText(addressSet);
        lengthText.setText(lengthSet);
        slaveText.setText(slaveSet);
        scanRateText.setText(scanRateSet);

    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "Destroy");
        if (!disconnect && updateTimer != null){
            disconnectModbusTCP();
        }
        super.onDestroy();
    }

    private View.OnClickListener mConnection = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            LayoutInflater inflater = getLayoutInflater();

            View popView = inflater.inflate(R.layout.connection, null);
            builder.setView(popView);

            ipAddress = (EditText)popView.findViewById(R.id.set_ip_address);
            port = (EditText)popView.findViewById(R.id.port);
            timeOut = (EditText)popView.findViewById(R.id.time_out);

            //Read data from Storage
            ipAddress.setText(readInternalStorage(ipFileName));
            port.setText(readInternalStorage(portFileName));
            timeOut.setText(readInternalStorage(timeOutFileName));
            //Set dialog can't cancelable
            builder.setCancelable(false);
            builder.setTitle("Connection");
            //Work when you press "Add"
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Move program to CustomConnectListener class
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            AlertDialog alertDialog = builder.create();
            //Add animation with dialog before show
           // alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimations_SmileWindow;
            alertDialog.show();
            Button addButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            //Check data before close dialog when you press "Search"
            addButton.setOnClickListener(new CustomConnectionListener(alertDialog));

        }
    };

    /* Function: Check data when you click Connect on Dialog */
    private class CustomConnectionListener implements View.OnClickListener {
        private final Dialog dialog;

        public CustomConnectionListener(Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            ipSet = ipAddress.getText().toString();
            portSet = port.getText().toString();
            timeOutSet = timeOut.getText().toString();

            Log.e(TAG_P, "IP: "+ipSet+" Port: "+portSet);

            if (!ipSet.equals("") && !portSet.equals("") && !timeOutSet.equals("")){
                //Save to internal storage
                writeInternalStorage(ipFileName, ipSet);
                writeInternalStorage(portFileName, portSet);
                writeInternalStorage(timeOutFileName, timeOutSet);

                initialFlag = true; //Set flag
                setVisibilityButton(1, View.VISIBLE); //Set Connect Button to Visible for connect
                dialog.dismiss(); //Close dialog
            }else {
                Toast.makeText(MainActivity.this, "Please enter all data box", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Function for initial modbus TCP
    private void modbusInit(String ip, int port, int time) {

        ModbusReq.getInstance().setParam(new ModbusParam()
                .setHost(ip)
                .setPort(port)
                .setEncapsulated(false)
                .setKeepAlive(true)
                .setTimeout(time)
                .setRetries(2))
                .init(new OnRequestBack<String>() {
                    @Override
                    public void onSuccess(String s) {
                        //Set flag initial
                        initialSuccess = true;
                        Log.d(TAG, "onSuccess " + s);
                    }

                    @Override
                    public void onFailed(String msg) {
                        initialSuccess = false;
                        Log.d(TAG, "onFailed " + msg);
                    }
                });

    }

    private View.OnClickListener mSetup = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            LayoutInflater inflater = getLayoutInflater();

            View popView = inflater.inflate(R.layout.setup_layout, null);
            builder.setView(popView);

            address = (EditText)popView.findViewById(R.id.address_add);
            length = (EditText)popView.findViewById(R.id.length_add);
            slave = (EditText)popView.findViewById(R.id.slave_add);
            scanRate = (EditText)popView.findViewById(R.id.scan_rate_add);

            //Show previously setup on box enter
            address.setText(addressSet);
            length.setText(lengthSet);
            slave.setText(slaveSet);
            scanRate.setText(scanRateSet);

            //Set dialog can't cancelable
            builder.setCancelable(false);
            builder.setTitle("Definition");
            builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Move program to CustomSetupListener class
                }
            });

            builder.setNegativeButton("Cancel", null);

            AlertDialog alertDialog = builder.create();
            //Add animation with dialog before show
            // alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimations_SmileWindow;
            alertDialog.show();
            Button addButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            //Check data before close dialog when you press "Search"
            addButton.setOnClickListener(new CustomSetupListener(alertDialog));

        }
    };

    /* Function: Check data when you click Setup on Dialog */
    private class CustomSetupListener implements View.OnClickListener {
        private final Dialog dialog;

        public CustomSetupListener(Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            //Get from user enter
            addressSet = address.getText().toString();
            lengthSet = length.getText().toString();
            slaveSet = slave.getText().toString();
            scanRateSet = scanRate.getText().toString();

            Log.e(TAG_P, "Address: "+addressSet+" Length: "+lengthSet+" Slave: "+slaveSet);
            //Check data if you don't check format data, it may bug
            if (!addressSet.equals("") && !lengthSet.equals("") && !slaveSet.equals("") && !scanRateSet.equals("")){
                //Setup data
                addressText.setText(addressSet);
                lengthText.setText(lengthSet);
                slaveText.setText(slaveSet);
                scanRateText.setText(scanRateSet);
                //Save to internal storage
                writeInternalStorage(fileSetup[0], addressSet);
                writeInternalStorage(fileSetup[1], lengthSet);
                writeInternalStorage(fileSetup[2], slaveSet);
                writeInternalStorage(fileSetup[3], scanRateSet);
                dialog.dismiss();
            }else {
                Toast.makeText(MainActivity.this, "Please enter all data box", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void readHoldingRegisters(int id, int startAddress, final int length){

        ModbusReq.getInstance().readHoldingRegisters(new OnRequestBack<short[]>() {
            @Override
            public void onSuccess(short[] data) {
                //Copy data to buffer
                readBuffer = Arrays.copyOf(data, data.length);
                readSuccess = true;
                Log.d(TAG, "readHoldingRegisters onSuccess " + Arrays.toString(data));

            }

            @Override
            public void onFailed(String msg) {
                readSuccess = false;
                Log.e(TAG, "readHoldingRegisters onFailed " + msg);
            }
        }, id, startAddress, length);

    }

    private View.OnClickListener mConnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (initialFlag){
                //Initial Modbus
                modbusInit(ipSet, Integer.valueOf(portSet), Integer.valueOf(timeOutSet));
                //Create Progress Dialog
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Modbus TCP connecting"); //Set progress message
                progressDialog.show(); //Show progress dialog when waite modbus initial flag
                //Delay for waiting flag from modbus initial
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something delay finish
                        progressDialog.dismiss(); //Close progress dialog
                        if (initialSuccess){
                            //Set status
                            setVisibilityButton(0, View.INVISIBLE); //Set Connection Button to Invisible when initial complete
                            statusTitle.setText("IP Address: "+ipSet+" Port: "+portSet+" Connected");
                            //Set timer for update data on list
                            intervalTime = Long.valueOf(scanRateSet); //Set interval time for onTick() by scan rate (ms)
                            updateTimer = new CountDownTimerForUpdate(totalCountdownTime, intervalTime);
                            //Start timer for update list
                            updateTimer.start();
                        }else {
                            setVisibilityButton(0, View.VISIBLE); //Set Connection Button to Visible for set again
                            statusTitle.setText("IP Address: "+ipSet+" Port: "+portSet+" Fail");
                        }

                    }
                }, 1000);
            }
        }
    };

    private View.OnClickListener mDisconnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            disconnectModbusTCP(); //Disconnect
        }
    };

    private void disconnectModbusTCP(){
        //TO DO disconnect Modbus
        ModbusReq.getInstance().destory();
        Toast.makeText(MainActivity.this, "Disconnect Modbus TCP", Toast.LENGTH_SHORT).show();
        statusTitle.setText("IP Address: "+ipSet+" Port: "+portSet+" No Connected");

        setVisibilityButton(1, View.VISIBLE); //Set Connect Button is Visible
        setVisibilityButton(0, View.VISIBLE);

        disconnect = true; //Set flag
        updateTimer.cancel(); //Stop timer
        setVisibilityButton(2, View.INVISIBLE); //Set Disconnect button is invisible
    }

    /*Class for update data on list*/
    public class CountDownTimerForUpdate extends CountDownTimer {

        public CountDownTimerForUpdate(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        //onTick() run when count match intervalTime
        @Override
        public void onTick(long millisUntilFinished) {
            //Read Holding Register
            readHoldingRegisters(Integer.valueOf(slaveSet), Integer.valueOf(addressSet), Integer.valueOf(lengthSet));
           //Check for re-write again
            if (reWriteFlag && readBuffer[writeAddress-Integer.valueOf(addressSet)] != Short.valueOf(valueSet)){
                writeSingleRegister(Integer.valueOf(slaveSet), writeAddress, Integer.valueOf(valueSet));
                //Create Progress Dialog
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Re-writing to Register"); //Set progress message
                progressDialog.show(); //Show progress dialog when waite writSuccess flag
                //Delay for write finish
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss(); //Close progress dialog
                        // Do something delay finish
                        if (writeSuccess){ writeSuccess = false; } //Clear flag
                        reWriteFlag = true; //Set flag
                    }
                }, 1000);
            }else{
                reWriteFlag = false; //Clear flag
            }

            //Detect click on data list for write request
            dataList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.e(TAG_C, "Position: "+position);

                    reWriteFlag = false; //Clear flag

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    LayoutInflater inflater = getLayoutInflater();

                    View popView = inflater.inflate(R.layout.write_layout, null);
                    builder.setView(popView);

                    value = (EditText)popView.findViewById(R.id.value_add);

                    writeAddress = Integer.valueOf(addressSet) + position;


                    //Set dialog can't cancelable
                    builder.setCancelable(false);
                    builder.setTitle("Writing to Address: " + writeAddress); //Set title on dialog
                    //Work when you press "Add"
                    builder.setPositiveButton("Write", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Move program to CustomSetupListener class
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    AlertDialog alertDialog = builder.create();
                    //Add animation with dialog before show
                    // alertDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimations_SmileWindow;
                    alertDialog.show();
                    Button addButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    //Check data before close dialog when you press "Write"
                    addButton.setOnClickListener(new CustomWriteListener(alertDialog));

                }
            });

            //Update list
            updateDataOnList();

        }

        @Override
        public void onFinish() {
            //Do when finish
            Toast.makeText(MainActivity.this, "Please Connect Again!", Toast.LENGTH_LONG).show();
            disconnectModbusTCP(); //Disconnect Modbus TCP
        }
    }
    private class CustomWriteListener implements View.OnClickListener {
        private final Dialog dialog;

        public CustomWriteListener(Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {

            valueSet = value.getText().toString();

            //Check data if you don't check format data, it may bug
            if (!valueSet.equals("")){
                //Write data
                Log.e(TAG_C, "Write value: "+valueSet);
                writeSingleRegister(Integer.valueOf(slaveSet), writeAddress, Integer.valueOf(valueSet));
                //Create Progress Dialog
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Writing to Register"); //Set progress message
                progressDialog.show(); //Show progress dialog when waite writSuccess flag
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something delay finish
                        progressDialog.dismiss(); //Close progress dialog
                        if (!writeSuccess){
                            Toast.makeText(MainActivity.this, "Write failed!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Write completed!", Toast.LENGTH_SHORT).show();
                            writeSuccess = false; //Clear flag
                            updateDataOnList(); //Update List
                        }
                        reWriteFlag = true; //Set re-write flag
                    }
                }, 1000);
                dialog.dismiss();
            }else {
                Toast.makeText(MainActivity.this, "Please enter data box", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void writeSingleRegister(int id, int address, int data){
        ModbusReq.getInstance().writeRegister(new OnRequestBack<String>() {
            @Override
            public void onSuccess(String s) {
                writeSuccess = true;
                Log.e(TAG, "writeRegister onSuccess " + s);
            }

            @Override
            public void onFailed(String msg) {
                writeSuccess = false;
                Log.e(TAG, "writeRegister onFailed " + msg);
            }
        }, id, address, data);
    }

    public void updateDataOnList(){
        if (readSuccess){
            setVisibilityButton(1, View.INVISIBLE); //Set Connect Button to invisible when read complete
            setVisibilityButton(2, View.VISIBLE); //Set Disconnect Button to visible
            //Toast.makeText(MainActivity.this,"Read Complete: "+Arrays.toString(readBuffer), Toast.LENGTH_SHORT).show();
            //Clear item before add
            items.clear();
            for (int k=0; k<readBuffer.length; k++){
                items.add("Address: "+(Integer.valueOf(addressSet)+k)+" = "+String.valueOf(readBuffer[k]));
            }
            //Update data on adapter
            adapter.notifyDataSetChanged();

        }else{
            countFail++;
            if (countFail > 5){
                countFail = 0; //Reset count
                Toast.makeText(MainActivity.this, "Please check wifi signal or reset Module!",
                        Toast.LENGTH_LONG).show();
                setVisibilityButton(0, View.INVISIBLE); //Set Disconnect button is invisible
                setVisibilityButton(1, View.VISIBLE); //Set Connect button is visible
            }
        }
    }

    public void setVisibilityButton(int set, int viewId){
        switch (set){
            case 1: connectBtn.setVisibility(viewId); break;
            case 2: disconnectBtn.setVisibility(viewId); break;
            default: connectionBtn.setVisibility(viewId);
        }
    }

    /*
     * Function: Write Text to Internal Storage
     * Input: file name and data (String type) */
    private void writeInternalStorage(String fileName, String data){
        try {
            FileOutputStream fOut = openFileOutput(fileName,MODE_WORLD_READABLE);
            fOut.write(data.getBytes());
            fOut.close();
          //  Toast.makeText(getBaseContext(),"file saved",Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /* Function: Read Text from Internal Storage
     * Input: file name
     * @return: text */
    private String readInternalStorage(String fileName){
        String temp = "";
        try {
            FileInputStream fin = openFileInput(fileName);
            int c;
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
         //   Toast.makeText(getBaseContext(),"file read",Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return temp;
    }

}
