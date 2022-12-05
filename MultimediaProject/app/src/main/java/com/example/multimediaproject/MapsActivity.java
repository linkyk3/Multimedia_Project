package com.example.multimediaproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        Log.d(TAG, "onMapReady: map is ready");

        if (gMap != null) {
            moveCamera(DEFAULT_ZOOM);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            gMap.setMyLocationEnabled(true);
            gMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

    }
    private static final String TAG = "MapsActivity";
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap gMap;

    private LatLng currentLatLng;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Log.d(TAG, "Maps Created");

        initMap();

    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        // Intent
        Intent intent = getIntent();
        double currentLongitude = Double.parseDouble(intent.getStringExtra("Current Longitude"));
        double currentLatitude = Double.parseDouble(intent.getStringExtra("Current Latitude"));
        Log.d(TAG, "Current Longitude: " + currentLongitude);
        Log.d(TAG, "Current Latitude: " + currentLatitude);
        // Parse to LatLng type
        currentLatLng = new LatLng(currentLatitude, currentLongitude);
    }

    private void moveCamera(float zoom){
        Log.d(TAG, "moveCamera: moving camera to current location...");
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoom));
    }

}