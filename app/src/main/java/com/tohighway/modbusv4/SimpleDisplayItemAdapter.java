package com.tohighway.modbusv4;

import android.content.Context;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;

public class SimpleDisplayItemAdapter extends RecyclerView.Adapter<SimpleDisplayItemAdapter.ItemHolder>  {
    //Public Class => Can access from external class
    //Private Class => Only internal class

    private List<SimpleDisplayItem> mItem;
    private Context mContext;
    private OnItemClickListener mListener;
    //Constant Value Format display flag
    public static final byte SIGNED_FORM = 0;
    public static final byte UNSIGNED_FORM = 1;
    public static final byte BINARY_FORM = 2;
    public static final byte HEX_FORM = 3;

    //Interface for Item Click Listener
    interface OnItemClickListener{
        void onItemClick(View view, int positionOnList, int addressNumber, String value);
    }
    //Get item clicked from external class
    public void setItemClickListener (OnItemClickListener listener){
        mListener = listener;
    }
    //ViewHolder Class
    public static class ItemHolder extends RecyclerView.ViewHolder {
        public TextView mNumber;
        public TextView mValue;
        public TextView mType;

        private ItemHolder(View view){
            super(view);
            mNumber = view.findViewById(R.id.number);
            mValue = view.findViewById(R.id.value);
            mType = view.findViewById(R.id.display_type);
        }
    }
    //Construction Class
    public SimpleDisplayItemAdapter(Context context, List<SimpleDisplayItem> list){
        mItem = list;
        mContext = context;
    }
    //Instance of Class
    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.simple_item_layout, parent, false);
        final ItemHolder vHolder = new ItemHolder(view);
        //Catch view is clicked
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make sure if mListener is set from external class
                if (mListener != null){
                    int pos = vHolder.getAdapterPosition();
                   // int address = Integer.valueOf(vHolder.mNumber.getText().toString());
                    int address = mItem.get(pos).getNumber();
                    String value = mItem.get(pos).getValue();
                    //int value = Integer.valueOf(vHolder.mValue.getText().toString());
                    //Make sure if view has position on RecycleView
                    if (pos != RecyclerView.NO_POSITION){
                        //Set data of item is clicked
                        mListener.onItemClick(v, pos, address, value);

                    }
                }

            }
        });
        return vHolder;
    }
    //Set data to view with Instance class => ItemHolder
    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        SimpleDisplayItem register = mItem.get(position);

        String numberOut = String.format("%05d", register.getNumber()); //Set number digits
        holder.mNumber.setText(numberOut);
        holder.mValue.setText(register.getValue());

        //Set background color of each type: Signed => Pink, Unsigned => Sky blue, Binary => Green, Hex => Orange
        byte type = register.getType();
        String byteText;
        int color;
        switch (type){
            case UNSIGNED_FORM: byteText = "Unsigned";
                color = Color.parseColor("#0040ff"); break;
            case BINARY_FORM: byteText = "Binary";
                color = Color.parseColor("#00ff00"); break;
            case HEX_FORM: byteText = "Hex";
                color = Color.parseColor("#ff8000"); break;
            default: byteText = "Signed";
                color = Color.parseColor("#ff00ff"); break;
        }
        holder.mType.setText(byteText);
       // holder.mType.setBackgroundColor(color);
    }
    //Set number of item on list
    @Override
    public int getItemCount() {
        return mItem.size();
    }

}
