package com.tohighway.modbusv4;

import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Renovate2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renovate2);

        //This titleBar active with inflate menu from onCreateOptionsMenu,
        // menu CallBack listener will be onOptionsItemSelected (@Override method)
        Toolbar titleBar = findViewById(R.id.title_bar);
        setSupportActionBar(titleBar);
        getSupportActionBar().setTitle("Modbus TCP Activity");
        //Create Control Tab
        createControlTabAndGetListener();

        TextView exceptionText = findViewById(R.id.exception_status);
        exceptionText.setVisibility(View.GONE);

        testList();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    //Function for Control tab layout
    private void createControlTabAndGetListener(){
        //Create Tab layout without viewpager
        final TabLayout controlTab = findViewById(R.id.control_tab);
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.new_icon));
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.open_icon));
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.save_icon));
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.refresh_icon));
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.setup_icon));
        controlTab.addTab(controlTab.newTab().setIcon(R.drawable.connection_icon));
        //Tab layout listener
        controlTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabNumber = controlTab.getSelectedTabPosition();
                switch (tabNumber){
                    case 0: newSheetActivity(); break;
                    case 1: openSheetActivity(); break;
                    case 2: saveSheetActivity(); break;
                    case 3: refreshActivity(); break;
                    case 4: setupActivity(); break;
                    case 5: connectionActivity(); break;
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
                    case 0: newSheetActivity(); break;
                    case 1: openSheetActivity(); break;
                    case 2: saveSheetActivity(); break;
                    case 3: refreshActivity(); break;
                    case 4: setupActivity(); break;
                    case 5: connectionActivity(); break;
                }
            }
        });
    }

    private void newSheetActivity(){

        Toast.makeText(Renovate2Activity.this, "New sheet", Toast.LENGTH_SHORT).show();
    }

    private void openSheetActivity(){
        Toast.makeText(Renovate2Activity.this, "Open sheet", Toast.LENGTH_SHORT).show();
    }

    private void saveSheetActivity(){
        Toast.makeText(Renovate2Activity.this, "Save", Toast.LENGTH_SHORT).show();
    }

    private void refreshActivity(){
        Toast.makeText(Renovate2Activity.this, "Refresh", Toast.LENGTH_SHORT).show();
    }

    private void setupActivity(){
        Toast.makeText(Renovate2Activity.this, "Setup", Toast.LENGTH_SHORT).show();
    }

    private void connectionActivity(){
        Toast.makeText(Renovate2Activity.this, "Connection", Toast.LENGTH_SHORT).show();
    }

    private void deleteSheetActivity(){
        Toast.makeText(Renovate2Activity.this, "Delete sheet", Toast.LENGTH_SHORT).show();
    }


    private void testList(){
        RecyclerView mRecyclerView = findViewById(R.id.register_list);
        //mRecyclerView.setHasFixedSize(true);

        List<Register> registerList = new ArrayList<>();
        for (int i=0; i<100; i++){
            registerList.add(new Register(i, "Register name "+String.valueOf(i), i));
        }

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new RegisterItemAdapter(this, registerList));

    }
}
