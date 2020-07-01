package com.tohighway.modbusv4;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tohighway.modbusv4.models.Connection;
import com.tohighway.modbusv4.models.ReadWriteDefinition;
import com.zgkxzx.modbus4And.requset.ModbusReq;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Renovate1Activity extends AppCompatActivity {
    //Initial Modbus variable for connection
    private static String ipAddress = "192.168.40.16";
    private int port = 502;
    private int timeOut = 2000; //ms
    //For initial address program for Modbus reading holding register function
    private int startAddress = 0;
    private int numberOfRegister = 10;
    private int slaveID = 1;
    //Status bar
    private TextView activeText;
    private TextView exceptionText;
    private int tx = 0; //Count Transmission request
    private int err = 0; //Count error
    private int scanRate = 1000; //Interval time for updating data on list => ms
    //On data List
    private List<SimpleDisplayItem> list;
    private SimpleDisplayItemAdapter adapter;
    private byte dataFormat = 0;//0 = Signed, 1 = Unsigned, 2 = Binary, 3 = Hex
    //Other
    private String TAG = "Renovate1";
    private Dialog cDialog = null;
    //private boolean connect = false; //Initial connect flag
    private byte []valueFormat; //0 = Signed, 1 = Unsigned, 2 = Binary, 3 = Hex
    private String currentValue; //Memory current value on item when touch
    private ClearEditTextWithDrawableIcon clearValue;
    //Constant Value Format display flag
    public static final byte SIGNED_FORM = 0;
    public static final byte UNSIGNED_FORM = 1;
    public static final byte BINARY_FORM = 2;
    public static final byte HEX_FORM = 3;
    //Modbus
    private ModbusTCPMaster master;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renovate1);

        //Create Control Tab
        createControlTabAndGetListener();

        //Status bar
        activeText = findViewById(R.id.active_status);
        exceptionText = findViewById(R.id.exception_status);
        showMbStatus("No Connection");

        //============= Initial Configuration ==================//
        //Connection
        Connection con = OnDevice.getConnection(this);
        ipAddress = con.ip;
        port = con.port;
        timeOut = con.timeout;
        //Read/Write
        ReadWriteDefinition def = OnDevice.getReadWriteDefinition(this);
        startAddress = def.address;
        numberOfRegister = def.quantity;
        slaveID = def.slaveId;
        scanRate = def.scanRate;
        dataFormat = (byte) def.displayType;

        //Create data list
        RecyclerView dataView = findViewById(R.id.register_list);
        list = new ArrayList<>();
        adapter = new SimpleDisplayItemAdapter(Renovate1Activity.this, list);
        dataView.setLayoutManager(new LinearLayoutManager(Renovate1Activity.this));
        dataView.setAdapter(adapter);

        //Set initial all value data format
        setAllValueFormat(dataFormat);

        //Check writing request
        adapter.setItemClickListener(new SimpleDisplayItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int positionOnList, int addressNumber, String value) {
                //Do writing activity
                writeActivity(positionOnList, addressNumber, value);
            }
        });

        //Initial
        master = new ModbusTCPMaster(new ModbusTCPMaster.OnResultListener() {
            @Override
            public void onSuccess(int transId, MbTask request, MbTask response, String mbException) {
                Log.w(TAG, "MB Response: ["+transId+"] ");
                //Result
                tx++;
                if (mbException!=null){
                    //Has exception
                    err++;
                    switch (response.fn){
                        case ModbusTCPMaster.FN_READ_HOLDING:
                            //Status
                            showMbStatus(mbException);
                            break;
                        case ModbusTCPMaster.FN_WRITE_SINGLE:
                            //Close dialog
                            showErrorDialog("Write failed: "+mbException);
                            //Back to read
                            master.readHoldingRegister(slaveID, startAddress, numberOfRegister);
                            break;
                    }
                }else {
                    //Normal
                    switch (response.fn){
                        case ModbusTCPMaster.FN_READ_HOLDING:
                            if (list.size()==response.data.length){
                                //list.clear();
                                for(int i=0; i<response.data.length; i++){
                                    list.set(i, new SimpleDisplayItem(request.data[0]+i,
                                            getStringValueFormat(response.data[i], valueFormat[i]), valueFormat[i]));
                                }
                                adapter.notifyDataSetChanged();
                            }
                            break;
                        case ModbusTCPMaster.FN_WRITE_SINGLE:
                            showStatusToast("Write success");
                            //Close dialog
                            if (cDialog!=null && cDialog.isShowing()){ cDialog.dismiss(); cDialog = null; }
                            //Update
                            int ix = response.data[0]-startAddress;
                            if (ix>=-1 && ix<list.size()){
                                SimpleDisplayItem item = list.get(ix);
                                item.setValue(getStringValueFormat(response.data[1], valueFormat[ix]));
                                adapter.notifyItemChanged(ix);
                            }
                            //Back to read
                            master.readHoldingRegister(slaveID, startAddress, numberOfRegister);
                            break;
                    }
                    //Status
                    showMbStatus(null);
                }
            }

            @Override
            public void onFailed(int transId, String e) {
                Log.e(TAG, "MB Error: ["+transId+"] "+e);
                //Show Status
                tx++; err++;
                showMbStatus(e);
            }
        });
        //Handler Connection
        master.setOnConnectionListener(new ModbusTCPMaster.OnConnectionListener() {
            @Override
            public void onConnecting(String ip, int port) {
                String s = "Connecting..."+ip+":"+port;
                Log.w(TAG, s);
                showMbStatus(s);
            }

            @Override
            public void onConnected(boolean connected) {
                Log.w(TAG, "Connected "+connected);
                if (connected){
                    showStatusToast("Modbus Connection Success");
                }else {
                    showErrorDialog("Modbus Connection Failed");
                }
            }

            @Override
            public void onDisconnected() {
                Log.w(TAG, "Disconnected");
                showStatusToast("Modbus Disconnection Success");
                showMbStatus("No Connection");
            }
        });

    }

    @Override
    protected void onStop() {
        //Save changed
        OnDevice.saveConnection(this,
                new Connection(ipAddress, port, timeOut));
        OnDevice.saveReadWriteDefinition(this,
                new ReadWriteDefinition(startAddress, numberOfRegister, slaveID, scanRate, dataFormat));
        //Stop
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //Stop MB
        master.disconnect();
        super.onDestroy();
    }

    //Function for Control tab layout
    private void createControlTabAndGetListener(){
        //Create Tab layout without viewpager
        final TabLayout controlTab = findViewById(R.id.control_tab);
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.refresh_icon));
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.setup_icon));
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.connection_icon));
        //Tab layout listener
        controlTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabNumber = controlTab.getSelectedTabPosition();
                switch (tabNumber){
                    case 1: setupActivity(); break;
                    case 2: connectionActivity(); break;
                    default: refreshActivity();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }
            //Do same tab selected
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int tabNumber = controlTab.getSelectedTabPosition();
                switch (tabNumber){
                    case 1: setupActivity(); break;
                    case 2: connectionActivity(); break;
                    default: refreshActivity();
                }
            }
        });
    }

    private void writeActivity(final int index, int addressNumber, String valueText) {
        currentValue = valueText; //Get current value
        //Set dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.renovate_write_layout, null);
        TextInputLayout valueLayout = view.findViewById(R.id.value_layout);
        final EditText valueEnter = view.findViewById(R.id.value_enter);
        final CheckBox resultCheck = view.findViewById(R.id.result_checking);
        Spinner display = view.findViewById(R.id.display_spinner);
        //Set default state
        resultCheck.setChecked(true);
        resultCheck.setVisibility(View.GONE); //Set initial view
        //Set previously state
        display.setSelection(valueFormat[index]);
        //Set hint on Value Layout => When use EditText in TextInputLayout can set hint by program at TextInputLayout
        valueLayout.setHint(getHintTextOnValueEnter(valueFormat[index]));
        valueEnter.setText(valueText);
       // valueEnter.setFocusable(false); //Hide cursor on EnterValue and disable editing
        Log.w(TAG, "Type: "+valueFormat[index]);
        //Set Input type
        switch (valueFormat[index]){
            case UNSIGNED_FORM:
                valueEnter.setInputType(InputType.TYPE_CLASS_NUMBER); break;
            case BINARY_FORM:
                valueEnter.setInputType(InputType.TYPE_CLASS_NUMBER);
                valueEnter.setKeyListener(DigitsKeyListener.getInstance("01 "));
                valueEnter.setFilters(new InputFilter[]{new InputFilter.LengthFilter(19)}); //Limit at 16 bits + space
                valueEnter.addTextChangedListener(new BinaryValueFormattingTextWatcher()); break;
            case HEX_FORM:
                valueEnter.setInputType(InputType.TYPE_CLASS_TEXT);
                //Set character that can type
                valueEnter.setKeyListener(DigitsKeyListener.getInstance("0123456789aAbBcCdDeEfFx"));
                //Set character to all cap when user is typing and limit max character on ValueEnter => 6 Char => 0xFFFF
                //valueEnter.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(6)}); break;
                valueEnter.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});

                valueEnter.addTextChangedListener(new HexValueFormattingTextWatcher()); break;
            default: valueEnter.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
        }

        //Create clear object for clear text on valueEnter
        clearValue = new ClearEditTextWithDrawableIcon(Renovate1Activity.this, valueEnter);
        valueEnter.setOnTouchListener(clearValue);
        clearValue.setOtherCheck(resultCheck); //Set resultCheck box
        //Set dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(Renovate1Activity.this);
        builder.setView(view);
        builder.setTitle("Write/Set Register");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Move to CheckValueEnter class
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Clear flag when dialog dismiss

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        //Get Positive button dialog interface
        Button writeBtu = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        writeBtu.setOnClickListener(new CheckValueEnter(dialog, addressNumber, valueFormat[index], valueEnter, display));
    }


    private String getHintTextOnValueEnter(byte form){
        switch (form){
            case UNSIGNED_FORM: return "Value (0 to 65535)";
            case BINARY_FORM: return "Binary Value (ex: 0111 0001 0101 1111)";
            case HEX_FORM: return "Hex Value (ex: 0xAFC8)";
            default: return "Value (-32768 to 32767)";
        }
    }

    /**
     * Formatting a Hex value: 0x####
     */
    public static class HexValueFormattingTextWatcher implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length()==0){
                s.insert(0, "0");
                s.insert(1, "x");
                if (s.length()==3){
                    s.delete(2, 2);
                }
            }
        }
    }

    /**
     * Formatting a binary value: #### #### #### ####
     */
    public static class BinaryValueFormattingTextWatcher implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            for (int i = 4; i < s.length(); i += 5) {
                if (s.toString().charAt(i) != ' ') {
                    s.insert(i, " ");
                }
            }
        }
    }

    private class CheckValueEnter implements View.OnClickListener{
        private Dialog dialog;
        private int writeAddress;
        private byte oldForm;
        private EditText value;
        private Spinner display;

        private CheckValueEnter(Dialog dialog, int writeAddress, byte oldForm, EditText value, Spinner display){
            this.dialog = dialog;
            this.writeAddress = writeAddress;
            this.oldForm = oldForm;
            this.value = value;
            this.display = display;
        }

        @Override
        public void onClick(View v) {
            //Get String
            String valueText = value.getText().toString();
            //Do only text on EditText different from currentValue
            if (!valueText.equals(currentValue)){
                if (!valueText.equals("")){
                    int writeValue = getUnsignedValue(valueText);
                    if (writeValue>=0 && writeValue<65536){
                        //Action
                        if (master.isConnected()){
                            master.writeSingleRegister(slaveID, writeAddress, writeValue);
                        }else {
                            showStatusToast("Please connect Modbus TCP server and try again");
                        }
                        //Holding
                        if (clearValue.getOtherCheck().isChecked()){
                            cDialog = dialog;
                        }else {
                            //Set register value display
                            valueFormat[writeAddress-startAddress] = (byte) display.getSelectedItemPosition();
                            //Dismiss dialog
                            dialog.dismiss();
                        }
                    }else {
                        showErrorDialog("Value is 'Out of Rang' or 'Invalid Format'");
                    }
                }else {
                    showErrorDialog("Please enter value");
                }
            }else {
                //Set register value display
                valueFormat[writeAddress-startAddress] = (byte) display.getSelectedItemPosition();
                dialog.dismiss();
            }
        }

        private int getUnsignedValue(String valueText){
            int u16Bit = -1; //-1 = On failed
            if (oldForm==UNSIGNED_FORM){
                u16Bit = Integer.valueOf(valueText);
            }else if (oldForm==BINARY_FORM){
                //Convert EditText of Binary format to Unsigned value 16 bits
                u16Bit = getUnsignedValueOfBinary(valueText);
            }else if (oldForm==HEX_FORM){
                //Convert EditText of Hex format to Unsigned value 16 bits
                u16Bit = getUnsignedValueOfHex(valueText.toUpperCase());
            }else {
                //Signed value to Unsigned value 16 bits
                u16Bit = Integer.valueOf(valueText);
                if(u16Bit>=-32768&&u16Bit<=32767){
                    if (u16Bit<0){
                        u16Bit += 65536;
                    }
                }else {
                    u16Bit = 65536; //Limit
                }
            }
            Log.w(TAG, "Write Value: "+u16Bit);
            return u16Bit;
        }

    }


    //Convert Binary format 0000 0000 0000 0000 => 16 bits
    private int getUnsignedValueOfBinary(String binaryValue){
        Log.w(TAG, "Enter binary: "+binaryValue);
        int l = binaryValue.length();
        if (l!=19){
            return -1; //-1 = On failed
        }else {
            int value = 0;
            int multiplier = 65536; //Initial 65536 = 2^16 due to binary 16 bits
            int n; char b;
            //Calculate Decimal of binary
            for (int i=0; i<l; i++){
                b = binaryValue.charAt(i);
                //Filter only bit binary
                if (b!=' '){
                    //Platform for convert binary base to decimal base
                    //Ex. 1011 (binary) = 1x8 + 0x4 + 1x2 + 1x1
                    multiplier = multiplier/2;
                    if (b=='1'){ n = 1; }else { n = 0; }
                    value += n*multiplier;
                }
            }
            Log.w(TAG, "Decimal of binary: "+value);
            /*
            //Cut only binary
            String onlyB = "";
            for (int i=0; i<l+1; i+=5){
                onlyB += binaryValue.substring(i, i+4);
            }
            //Calculate Decimal of binary
            for (int i=0; i<onlyB.length(); i++){
                if (onlyB.charAt(i)=='1'){ n = 1; }
                else { n = 0; }
                multiplier = multiplier/2;
                value += n*multiplier;
                Log.e(TAG, "n = "+n+" M = "+multiplier);
            }
            Log.e(TAG, "Enter Binary: "+onlyB+" Decimal: "+value); */
            return value;
        }
    }

    //Convert Hex format 0xNNNN => 16 bits
    private int getUnsignedValueOfHex(String hexValue){
        int value = -1; //-1 = On failed
        if (hexValue.length()==6){
            int []d = new int[4];
            //Get Decimal base of each hex
            for (int i=0; i<d.length; i++){
                d[i] = getDecimalOfEachHex(hexValue.charAt(i+2)); //Cut only N => 0xNNNN
                if (d[i]==-1){
                    return -1; //Invalid Format
                }
            }
            //Calculate decimal base value
            value = (d[0]*16*16*16)+(d[1]*16*16)+(d[2]*16)+d[3];
        }
        Log.w(TAG, "Hex: "+hexValue+" Decimal: "+value);
        return value;
    }

    private int getDecimalOfEachHex(char hex){
        Log.e(TAG, "Hex: "+hex);
        switch (hex){
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'A': return 10;
            case 'B': return 11;
            case 'C': return 12;
            case 'D': return 13;
            case 'E': return 14;
            case 'F': return 15;
        }
        return -1; //On failed
    }

    private void showMbStatus(String mbException){
        String d = "Tx = "+tx+": "+"Err = "+err+": "+"ID = "+slaveID+": "+"SR = "+scanRate+"ms";
        activeText.setText(d);
        if (mbException!=null){
            exceptionText.setText(mbException);
            exceptionText.setVisibility(View.VISIBLE);
        }else {
            exceptionText.setVisibility(View.GONE);
        }
    }

    private void refreshActivity(){
        master.refresh();
        tx = 0; err = 0;
        if (master.isConnected()){
            showMbStatus(null);
        }else {
            showMbStatus("No Connection");
        }
    }

    private void setupActivity(){
        //Toast.makeText(Renovate1Activity.this, "Setup", Toast.LENGTH_SHORT).show();
        //Convert layout XML to Java object
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.renovate_setup_layout, null);
        //Declare Object in layout
        EditText addressEnter = view.findViewById(R.id.address_add);
        EditText quantityEnter = view.findViewById(R.id.length_add);
        EditText slaveIdEnter = view.findViewById(R.id.slave_add);
        EditText scanRateEnter = view.findViewById(R.id.scan_rate_add);
        Spinner displaySpinner = view.findViewById(R.id.display_spinner);
        //Show previously data on EditText and Spinner
        addressEnter.setText(String.valueOf(startAddress));
        quantityEnter.setText(String.valueOf(numberOfRegister));
        slaveIdEnter.setText(String.valueOf(slaveID));
        scanRateEnter.setText(String.valueOf(scanRate));
        displaySpinner.setSelection(dataFormat);
        /*
        //Set clear text icon
        ClearEditTextWithDrawableIcon clearAddress = new ClearEditTextWithDrawableIcon(Renovate1Activity.this, addressEnter);
        addressEnter.setOnTouchListener(clearAddress);
        ClearEditTextWithDrawableIcon clearQuantity = new ClearEditTextWithDrawableIcon(Renovate1Activity.this, quantityEnter);
        quantityEnter.setOnTouchListener(clearQuantity);
        ClearEditTextWithDrawableIcon clearId = new ClearEditTextWithDrawableIcon(Renovate1Activity.this, slaveIdEnter);
        slaveIdEnter.setOnTouchListener(clearId);
        ClearEditTextWithDrawableIcon clearSR = new ClearEditTextWithDrawableIcon(Renovate1Activity.this, scanRateEnter);
        scanRateEnter.setOnTouchListener(clearSR); */
        //Create Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(Renovate1Activity.this);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setTitle("Read/Write Definition");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Move all to CheckSetupData Class
            }
        });
        builder.setNegativeButton("CANCEL", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        Button oKButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        oKButton.setOnClickListener(new CheckSetupData(dialog, addressEnter, quantityEnter, slaveIdEnter, scanRateEnter, displaySpinner));

    }

    private class CheckSetupData implements View.OnClickListener{
        private Dialog dialog;
        private EditText addressT, quantityT, idT, scanT;
        private Spinner formatSpin;
        private int address, quantity, id, scan;

        private CheckSetupData(Dialog dialog, EditText addressText, EditText quantityText, EditText idText, EditText scanText, Spinner formatSpin){

            this.dialog = dialog;
            this.addressT = addressText;
            this.quantityT = quantityText;
            this.idT = idText;
            this.scanT = scanText;
            this.formatSpin = formatSpin;
        }

        @Override
        public void onClick(View v) {
            address = Integer.valueOf("0"+addressT.getText().toString());
            quantity = Integer.valueOf("0"+quantityT.getText().toString());
            id = Integer.valueOf("0"+idT.getText().toString());
            scan = Integer.valueOf("0"+scanT.getText().toString());
            //Check rang data follow with Modbus Protocol
            if (address<0 || address>65535){
                showErrorDialog("Enter Address between 0 and 65535");
            }else if (quantity<1 || quantity>ModbusTCPMaster.READ_LEN_MAX){
                showErrorDialog("Enter Quantity between 1 and "+String.valueOf(ModbusTCPMaster.READ_LEN_MAX));
            }else if (id<0 || id>255){
                showErrorDialog("Enter Slave ID between 0 and 255");
            }else if (scan<=0){
                showErrorDialog("Enter Scan Rate more than 0");
            }else {
                //Set data to variable for showing on list
                startAddress = address;
                numberOfRegister = quantity;
                slaveID = id;
                scanRate = scan;
                dataFormat = (byte)formatSpin.getSelectedItemPosition(); //Get data format selected
                setAllValueFormat(dataFormat); //Refresh all data value format of register
                refreshActivity(); //Refresh status
                master.setScanRate(scanRate);
                //Update reading
                if (master.isConnected()){
                    master.readHoldingRegister(slaveID, startAddress, numberOfRegister);
                }
                //Close dialog
                dialog.dismiss();
            }
        }

    }

    private void setAllValueFormat(byte form){
        //Resize array
        if (valueFormat==null){
            valueFormat = new byte[numberOfRegister];
        }else if(valueFormat.length!=numberOfRegister){
            valueFormat = Arrays.copyOf(valueFormat, numberOfRegister);
        }
        //Re-create list and save format
        list.clear();
        for (int y=0; y<numberOfRegister; y++){
            valueFormat[y] = form;
            list.add(new SimpleDisplayItem(startAddress+y, getStringValueFormat(0, form), form));
        }
        adapter.notifyDataSetChanged();
    }

    private void connectionActivity(){
        //Toast.makeText(Renovate1Activity.this, "Connection", Toast.LENGTH_SHORT).show();
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.connection, null);
        //Declare object in layout
        EditText ipEnter = view.findViewById(R.id.set_ip_address);
        EditText portEnter = view.findViewById(R.id.port);
        EditText timeoutEnter = view.findViewById(R.id.time_out);
        //Show previously data on EditText
        ipEnter.setText(ipAddress);
        portEnter.setText(String.valueOf(port));
        timeoutEnter.setText(String.valueOf(timeOut));
        //Other
        String pName;
        if (master.isConnected()){
            //Set disable editing on Edits Text
            ipEnter.setFocusable(false);
            portEnter.setFocusable(false);
            timeoutEnter.setFocusable(false);
            pName = "Disconnect";
        }else {
            pName = "Connect";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(Renovate1Activity.this);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setTitle("Connection");
        builder.setPositiveButton(pName, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Move all to SetConnectAndDisconnect class
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        Button pButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        pButton.setOnClickListener(new SetConnectAndDisconnect(dialog, ipEnter, portEnter, timeoutEnter));
    }

    private class SetConnectAndDisconnect implements View.OnClickListener{
        private Dialog dialog;
        private EditText ipT, portT, timeoutT;
        private String ipS = "";
        private int portI, timeoutI;

        private SetConnectAndDisconnect(Dialog dialog, EditText ip, EditText port, EditText timeout){
            this.dialog = dialog;
            this.ipT = ip;
            this.portT = port;
            this.timeoutT = timeout;
        }

        @Override
        public void onClick(View v) {
            if (master.isConnected()){
                //Now Connect status => Do disconnect
                //Disconnect Modbus
                master.disconnect();
                //Dismiss dialog
                dialog.dismiss();
            }else {
                //Now No connect status => Do connect
                ipS = ipT.getText().toString();
                portI = Integer.valueOf("0"+portT.getText().toString());
                timeoutI = Integer.valueOf("0"+timeoutT.getText().toString());
                //Check data
                if (ipS.isEmpty()){
                    showErrorDialog("Invalid IP Address");
                }else if (portI<0 || portI>65535){
                    showErrorDialog("Enter Port between 0 - 65535");
                }else if (timeoutI<0){
                    showErrorDialog("Enter Timeout more than 0");
                }else {
                    //Set data for connection
                    ipAddress = ipS;
                    port = portI;
                    timeOut = timeoutI;
                    //Connect Modbus
                    master.setConnection(ipAddress, port, timeOut);
                    //Start interval reading
                    master.readHoldingRegister(slaveID, startAddress, numberOfRegister);
                    //Dismiss dialog
                    dialog.dismiss();
                }
            }
        }
    }

    public void showErrorDialog(String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(Renovate1Activity.this);
        builder.setMessage(text);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showStatusToast(String text){
        Toast toast = Toast.makeText(Renovate1Activity.this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private String getStringValueFormat(int s16Value, byte format){
        //Log.w(TAG, "Show value = "+s16Value+" in format "+format);
        String out;
        int unsignedValue;
        if (s16Value<0){
            unsignedValue = s16Value + 65536;
        }else {
            unsignedValue = s16Value;
        }
        switch (format){
            case UNSIGNED_FORM: //Unsigned form
                out = String.valueOf(unsignedValue);
                break;
            case BINARY_FORM: //Binary form
                String biActual, sub1, sub2, sub3, sub4;
                //Fixed print out 16 bits
                //If bit 16 = 1 => value must >= 32768
                if (unsignedValue>=32768){
                    biActual = Integer.toBinaryString(32768|unsignedValue);
                }else {
                    biActual = "0"+Integer.toBinaryString(32768|unsignedValue).substring(1);
                }
                sub1 = biActual.substring(0, 4);
                sub2 = biActual.substring(4, 8);
                sub3 = biActual.substring(8, 12);
                sub4 = biActual.substring(12, 16);
                out = sub1+" "+sub2+" "+sub3+" "+sub4; //0000 0000 0000 0000
                break;
            case HEX_FORM: //Hex form
                String hexActual;
                if (s16Value<0){
                    hexActual = Integer.toHexString(32768|unsignedValue);
                }else {
                    hexActual = Integer.toHexString(unsignedValue);
                    switch (hexActual.length()){
                        case 3: hexActual = "0"+hexActual; break;
                        case 2: hexActual = "00"+hexActual; break;
                        case 1: hexActual = "000"+hexActual; break;
                        case 0: hexActual = "0000"; break;
                    }
                }
                out = "0x"+hexActual.toUpperCase();
                break;
            default: //Signed form
                out = String.valueOf(s16Value);
        }
        return out;
    }

}
