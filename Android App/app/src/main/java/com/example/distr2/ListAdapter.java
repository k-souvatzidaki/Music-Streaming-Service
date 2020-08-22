package com.example.distr2;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

//an adapter to view String lists in RecyclerView
public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private ArrayList<String> list_items;
    private OnClickListener listener;

    //constructor
    public ListAdapter(ArrayList<String> list_items, OnClickListener listener) {
        this.list_items = list_items;
        this.listener = listener;
    }

    //the view of a single list item
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // each data item is just a string in this case
        public ViewGroup listItem;
        public TextView text_view;
        OnClickListener listener;

        public ViewHolder(ViewGroup v,OnClickListener listener) {
            super(v);
            listItem = v;
            text_view = listItem.findViewById(R.id.list_item);
            this.listener = listener;

            v.setOnClickListener(this);
        }

        //click listener for the list item
        //onItemClick is implemented inside the activity of the list
        @Override
        public void onClick(View v) {
            listener.onItemClick(getAdapterPosition());
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        //create a new view for the list
        ViewGroup v = (ViewGroup) LayoutInflater.from(viewGroup.getContext())
                      .inflate(R.layout.list_item, viewGroup, false);
        return new ViewHolder(v,listener);
    }


    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        //set the text of the list item
        String currentArtist = list_items.get(i);
        viewHolder.text_view.setText(currentArtist);
    }


    @Override
    public int getItemCount() {
        return list_items.size();
    }


    //click listener interface implemented by the activities
    public interface OnClickListener {
        void onItemClick(int pos);
    }

}