package com.example.multimediaproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        Log.d(TAG, "onMapReady: map is ready");

        if (gMap != null) {
            //moveCamera(DEFAULT_ZOOM);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // Own Location
            gMap.setMyLocationEnabled(true);
            gMap.getUiSettings().setMyLocationButtonEnabled(false);
            moveCamera(DEFAULT_ZOOM);

            // Station Markers
            for(int i = 0; i < stationData.size(); i++){
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng latLng = new LatLng(stationData.get(i).getLatitude(), stationData.get(i).getLongitude());
                //Log.d(TAG, "LatLng: " + latLng);
                markerOptions.position(latLng);
                markerOptions.title(stationData.get(i).getStation());
                markerOptions.icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_outline_tram_24));
                gMap.addMarker(markerOptions);
            }
        }
    }
    private static final String TAG = "MapsActivity";
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap gMap;
    private LatLng currentLatLng;
    private List<StationSample> stationData = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Log.d(TAG, "Maps Created");

        initMap();

        readStationData();

    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        // Intent
        Intent intent = getIntent();
        double currentLongitude = Double.parseDouble(intent.getStringExtra("Current Longitude"));
        double currentLatitude = Double.parseDouble(intent.getStringExtra("Current Latitude"));
        //Bundle bundle = intent.getExtras();
        //stationData = bundle.getParcelableArrayList("Station Data");
        Log.d(TAG, "Current Longitude: " + currentLongitude);
        Log.d(TAG, "Current Latitude: " + currentLatitude);
        // Parse to LatLng type
        currentLatLng = new LatLng(currentLatitude, currentLongitude);
    }

    private void moveCamera(float zoom){
        Log.d(TAG, "moveCamera: moving camera to current location...");
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoom));
    }

    private void readStationData() {
        Log.d(TAG, "Reading Station Data");
        InputStream inputStream = getResources().openRawResource(R.raw.stops_data);
        BufferedReader lineReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        String line = "";
        int i = 0;
        try {
            while ((line = lineReader.readLine()) != null){
                // Skip header
                if(i == 0){
                    i++;
                    continue;
                }
                // Split by ';'
                String[] tokens = line.split(",");
                // Read the data
                StationSample sample = new StationSample();
                sample.setLatitude(Double.parseDouble(tokens[0]));
                sample.setLongitude(Double.parseDouble(tokens[1]));
                sample.setStation(tokens[2]);
                stationData.add(sample);
            }
        } catch (IOException e) {
            Log.wtf(TAG, "Error reading data file on line" + line, e);
            e.printStackTrace();
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId){
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

}