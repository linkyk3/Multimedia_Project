package com.example.multimediaproject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class ControlStationsAdapter extends BaseAdapter{
    private Context context;
    private List<String> controlStations;

    public ControlStationsAdapter(Context context, List controlStations){
        super();
        this.context = context;
        this.controlStations = controlStations;
    }

    @Override
    public int getCount() {
        return controlStations.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        LayoutInflater  inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.controlstations_list_view, parent, false);

        TextView tv_stationName = (TextView) view.findViewById(R.id.tvControlStationName);
        tv_stationName.setText(controlStations.get(i));
        return view;
    }
}
