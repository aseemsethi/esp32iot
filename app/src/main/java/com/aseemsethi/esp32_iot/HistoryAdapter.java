package com.aseemsethi.esp32_iot;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder>{

    private ArrayList<String> history;
    //Store Colors here indexed by posotion
    ArrayList<Integer> myColors = new ArrayList<Integer>();
    int colorVar = Color.BLACK;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        LinearLayout rootView;//newly added field
        LinearLayout postView;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.row_text);
            rootView=(LinearLayout)v.findViewById(R.id.rootView);
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

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        System.out.println("Bind: " + position);
        //holder.itemView.setBackgroundColor(colorVar);
        //holder.rootView.setBackgroundColor(myColors.get(position));
        holder.mTextView.setTextColor(myColors.get(position));
        holder.mTextView.setText(history.get(position));

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

}