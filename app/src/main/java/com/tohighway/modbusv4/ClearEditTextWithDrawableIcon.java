package com.tohighway.modbusv4;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class ClearEditTextWithDrawableIcon extends AppCompatEditText implements View.OnTouchListener {
    private EditText edit;
    private boolean clear = false;
    private CheckBox otherCheck;
    private String TAG = "Touch";

    public ClearEditTextWithDrawableIcon(Context context, EditText edit){
        super(context);
        this.edit = edit;

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.e(TAG, "Touch on EditText...");
        if (event.getAction()!=MotionEvent.ACTION_UP){
            return false;
        }
        //Set cursor
        edit.setCursorVisible(true);
        //Set visible of otherCheck
        if (otherCheck!=null){
            otherCheck.setVisibility(VISIBLE);
        }
        setIconVisibility(true); //Show clear icon
        //Icon Position: [0] => Left, [1] => Top, [2] => Right, [3] => Bottom
        Drawable[] drawables = edit.getCompoundDrawables();
        //Final check icon
        if (drawables[2]==null){return false;}
        //Get icon position on EditText layout
        int drwPos = edit.getWidth() - edit.getPaddingRight() - drawables[2].getIntrinsicWidth();
        //Clear text on EditText
        if (event.getX()>=drwPos){
            edit.getText().clear(); //Clear text on EditText
            clear = true; //Set flag
            //setIconVisibility(false); //Remove clear icon
        }
        return false;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public boolean getClear(){
        return clear;
    }

    public void setClear(boolean status){
        clear = status;
    }

    public void setOtherCheck(CheckBox checkBox){
        otherCheck = checkBox;
    }

    public CheckBox getOtherCheck(){
        return otherCheck;
    }

    public void setIconVisibility(boolean status){
        if (status){
            //Show Clear icon
            edit.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.clear_icon, 0);
        }else {
            //Remove Clear icon
            edit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

}
