package com.tohighway.modbusv4;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class RegisterItemAdapter extends RecyclerView.Adapter<RegisterItemAdapter.RegisterHolder> {

    private List<Register> mRegister;
    private Context mContext;

    public static class RegisterHolder extends RecyclerView.ViewHolder {
        public TextView mNumber;
        public TextView mName;
        public TextView mValue;

        public RegisterHolder(View view){
            super(view);
            mNumber = view.findViewById(R.id.address_number);
            mName = view.findViewById(R.id.register_name);
            mValue = view.findViewById(R.id.register_value);
        }

    }

    public RegisterItemAdapter(Context context, List<Register>list){
        mRegister = list;
        mContext = context;
    }

    @Override
    public RegisterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.register_item_layout, parent, false);
        return new RegisterHolder(view);
    }

    @Override
    public void onBindViewHolder(RegisterHolder holder, int position) {
        Register register = mRegister.get(position);
        holder.mNumber.setText(String.valueOf(register.getNumber()));
        holder.mName.setText(register.getName());
        holder.mValue.setText(String.valueOf(register.getValue()));
    }

    @Override
    public int getItemCount() {
        return mRegister.size();
    }

}
