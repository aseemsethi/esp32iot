package com.aseemsethi.esp32_iot;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder>{

    private ArrayList<String> history;
    //Store Colors here indexed by posotion
    ArrayList<Integer> myColors = new ArrayList<Integer>();
    int colorVar = Color.BLACK;
    int selectedPos = RecyclerView.NO_POSITION;
    int row_index = 0;
    final String TAG = "ESP32IOT Adapter";


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        LinearLayout rootView;//newly added field
        LinearLayout postView;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.row_text);
            rootView=(LinearLayout)v.findViewById(R.id.rootView);
            v.setClickable(true);
        }
    }

    public HistoryAdapter(ArrayList<String> dataSet){
        history = dataSet;
    }

    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create View
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_row, parent, false);
        return new ViewHolder(v);
    }

    public void add(String data, int color){
        colorVar = color;
        myColors.add(color);
        history.add(data);
        this.notifyDataSetChanged();
    }

    public String get(int index){
        return history.get(index);
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        //Log.d(TAG, "Bind Id: " + position);
        //holder.itemView.setBackgroundColor(colorVar);
        //holder.rootView.setBackgroundColor(myColors.get(position));
        holder.mTextView.setTextSize(12);
        holder.mTextView.setTextColor(myColors.get(position));
        holder.mTextView.setText(history.get(position));

        holder.mTextView.setSelected(selectedPos == position);
        //holder.mTextView.setBackgroundColor(selected_position == position ?
        // Color.GREEN : Color.TRANSPARENT);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Selected Id: " + row_index);
                row_index=position;
                notifyDataSetChanged();
            }
        });

        if(row_index==position){
            holder.itemView.setBackgroundColor(Color.parseColor("#bfefff"));
        }
        else
        {
            holder.itemView.setBackgroundColor(Color.parseColor("#f8f6f7"));
        }
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }
    public void clear() {
        int size = history.size();
        history.clear();
        notifyItemRangeRemoved(0, size);
    }

}