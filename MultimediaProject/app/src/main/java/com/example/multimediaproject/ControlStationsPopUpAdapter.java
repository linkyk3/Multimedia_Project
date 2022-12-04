package com.example.multimediaproject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class ControlStationsPopUpAdapter extends BaseAdapter {
    private Context context;
    private List<String> stationsToCheck;
    private List<String> controlStations;
    private MainActivity mainActivity;

    public ControlStationsPopUpAdapter(Context context, List stationsToCheck, List controlStations, MainActivity mainActivity){
        super();
        this.context = context;
        this.stationsToCheck = stationsToCheck;
        this.controlStations = controlStations;
        this.mainActivity = mainActivity;
    }

    @Override
    public int getCount() {
        return stationsToCheck.size();
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
        view = inflater.inflate(R.layout.popupcontrol_list_view, parent, false);

        TextView tv_stationName = (TextView) view.findViewById(R.id.tvControlLine);
        Button btnYes = (Button) view.findViewById(R.id.btnCLYes);
        Button btnNo = (Button) view.findViewById(R.id.btnCLNo);

        tv_stationName.setText(stationsToCheck.get(i));

        // YES
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.retrieveFromDatabase(new MainActivity.FirestoreCallback() {
                    @Override
                    public void onCallback(List<String> currentControlStationsDB) {
                        //Log.d("MyActivity", "Call Back from Database: Done");
                        if(!currentControlStationsDB.contains(stationsToCheck.get(i))){ // not yet in list
                            Log.d("MyActivity", "Station not yet in database, adding to Database: " + stationsToCheck.get(i));
                            mainActivity.addToDatabase(mainActivity.currentControlStationDoc, stationsToCheck.get(i));
                        }
                        else {
                            Log.d("MyActivity", "Station already in database: " + stationsToCheck.get(i));
                        }
                        // disable both buttons when one of them is pressed
                        btnYes.setEnabled(false);
                        btnNo.setEnabled(false);
                    }
                });
            }
        });
        // NO
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.retrieveFromDatabase(new MainActivity.FirestoreCallback() {
                    @Override
                    public void onCallback(List<String> currentControlStationsDB) {
                        // check if in database, if so -> remove
                        if(currentControlStationsDB.contains(stationsToCheck.get(i))){
                            Log.d("MyActivity", "Removing from database: " + stationsToCheck.get(i));
                            mainActivity.removeFromDatabase(stationsToCheck.get(i));
                        }
                        else{
                            Log.d("MyActivity", "Station not in database: " + stationsToCheck.get(i));
                        }
                        // disable both buttons when one of them is pressed
                        btnYes.setEnabled(false);
                        btnNo.setEnabled(false);
                    }
                });
            }
        });

        return view;
    }
}
