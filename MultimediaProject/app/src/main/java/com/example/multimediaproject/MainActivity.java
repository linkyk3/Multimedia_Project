package com.example.multimediaproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;


import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    // Macros
    private static final String TAG = "MainActivity";
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 5;
    private static final int PERMISSION_FINE_lOCATION = 99;
    private static final int DISTANCE_RADIUS = 500;
    private static final int CONTROL_RADIUS = 500;
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static final int ERROR_DIALOG_REQUEST = 9001;

    // UI elements
    TextView textField;
    Button btnMaps;
    Button btnControleSubmit;
    Button btnControl;
    AutoCompleteTextView controlStationInput;

    //--- Location ---//
    // Config file for all settings related to FusedLocationProviderContent
    LocationRequest locationRequest;
    // Google API for location services
    FusedLocationProviderClient fusedLocationProviderClient;
    // Necessary for a function
    LocationCallback locationCallBack;
    // Location updater counter
    private int locationUpdateCounter = 0;
    // Current long and lat
    public double currentLongitude;
    public double currentLatitude;

    // Lists
    private final List<StationSample> stationData = new ArrayList<>(); // list with the CSV stops data
    private final List<NearbyStations> nearbyStations = new ArrayList<>(); // list with the nearby stations -> see DISTANCE_RADIUS
    private final List<String> controlStationsCurrent = new ArrayList<>(); // list with the stations where a control is happening (current controls)
    private final List<String> controlStationsToCheck = new ArrayList<>(); // list with the nearby stations that have to be checked if there is a control -> see CONTROL_RADIUS

    // Arrays
    private String[] autoCompleteStations;

    // List Views and Adapters
    // Nearby Stations
    private ListView nearbyStationsListView;
    private NearbyStationsAdapter nearbyStationsAdapter;
    // Current Controls
    private ListView controlStationsListView;
    private ControlStationsAdapter controlStationsAdapter;
    // Controls To Check -> Pop Up Window
    private ListView controlsToCheckListView;
    private ControlStationsPopUpAdapter controlsToCheckAdapter;

    // Firestone database: used for the control stations
    public FirebaseFirestore firestoreDB = FirebaseFirestore.getInstance();
    public Map<String, Object> currentControlStationDoc = new HashMap<>();
    public String collectionName = "Current Control Stations";
    public String document = "Station Name";
    private List<String> dataDB = new ArrayList<>();
    private List<String> prevControlStations = new ArrayList<>();
    private boolean callBackDone = false;
    private boolean firstCall = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View rootView = findViewById(android.R.id.content);

        // Read CSV file
        readStationData();

        //---UI elements---//
        btnMaps = findViewById(R.id.btnMaps);
        btnControleSubmit = findViewById(R.id.btnControleSubmit);
        btnControl = findViewById(R.id.btnControls);

        // Auto Complete Text View
        controlStationInput = findViewById(R.id.TextViewControlStation);
        autoCompleteStations = new String[stationData.size()];
        getStationStringList();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, autoCompleteStations);
        controlStationInput.setAdapter(adapter);

        //---Location---//
        // set all properties of LocationRequest
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // event that is triggered whenever the update interval is met
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // save the location
                updateUI(locationResult.getLastLocation());
            }
        };

        // First call this function so that the locationRequest object is made an permission is checked
        updateGPS();

        // Start constant location update when app is launched
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        startLocationUpdates();
        updateGPS();

        updateControlLV();

        //--- UI Listeners ---//
        // MAPS
        btnMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentMaps = new Intent(MainActivity.this, MapsActivity.class);
                intentMaps.putExtra("Current Longitude", Double.toString(currentLongitude));
                intentMaps.putExtra("Current Latitude", Double.toString(currentLatitude));
                startActivity(intentMaps);
            }
        });
        // SUBMIT
        btnControleSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get input from UI
                String stationName = controlStationInput.getText().toString();
                // Get the latetst control station list from the firestone database
                retrieveFromDatabase(new FirestoreCallback() {
                    @Override
                    public void onCallback(List<String> currentControlStationsDB) {
                        //Log.d("MyActivity", "Call Back from Database: Done");
                        if(!currentControlStationsDB.contains(stationName)){ // not yet in list
                            Log.d("MyActivity", "Station not yet in database, adding to Database: " + stationName);
                            addToDatabase(currentControlStationDoc, stationName);
                            updateControlLV();
                            controlStationInput.setText("");
                        }
                        else{
                            Log.d("MyActivity", "Station already in database: " + stationName);
                        }
                    }
                });
            }
        });
        // CONTROL
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MyActivity", "Checking controls at nearby stations");
                // Check the nearby stations that have to be controlled
                checkNearbyControlStations();
                // Launch pop up window
                BottomSheetDialog popUpControlDialog = new BottomSheetDialog(
                        MainActivity.this, R.style.PopUpWindowTheme
                );

                View popUpControlView = LayoutInflater.from(getApplicationContext())
                        .inflate(
                                R.layout.popupcontrol_window,
                                (LinearLayout)findViewById(R.id.popUpControlWindow)
                        );

                popUpControlDialog.setContentView(popUpControlView);
                popUpControlDialog.show();

                controlsToCheckListView = (ListView) popUpControlView.findViewById(R.id.listViewControlsToCheck);
                controlsToCheckAdapter = new ControlStationsPopUpAdapter(getApplicationContext(), controlStationsToCheck, controlStationsCurrent, MainActivity.this);
                controlsToCheckListView.setAdapter(controlsToCheckAdapter);
                printStringList(controlStationsCurrent);

                // Update the GPS/UI when pop up dialog is closed
                popUpControlDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        updateGPS();
                        updateControlLV();
                    }
                });
            }
        });

    } // end Oncreate

    //--- Location/GPS Functions ---//
    private void startLocationUpdates() {
        Toast.makeText(getApplicationContext(), "Location is being tracked", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case PERMISSION_FINE_lOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void updateGPS() {
        // get permissions from the user to track GPS
        // get current location from the fused client
        // update UI
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        // If permission is granted from the user
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // user provided the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // pass the current long and lat to corresponding vars
                    currentLongitude = location.getLongitude();
                    currentLatitude = location.getLatitude();
                    //currentLongitude = 50.837398;
                    //currentLatitude = 4.407608;
                    // we got permission, put values of location in to the UI
                    updateUI(location);
                    // check nearby stations everytime location is updated
                    checkNearbyStations(location);
                }
            });
        }
        else {
            // permission not granted yet
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // check OS version
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_lOCATION);
            }
        }
        
    }

    //--- Google Maps Functions ---//
    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version...");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServicesOK: an error occured but fixable");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    //--- UI Functions ---//
    private void updateUI(Location location) {
        Log.d("MyActivity", "Updating UI...");
        locationUpdateCounter++;
        Log.d("MyActivity", "Location Update Counter: " + locationUpdateCounter);
        // Update List Views
        //printNearbyStationList();

        // Set List Views
        // Nearby Station List View
        //Log.d("MyActivity", "Setting Nearby Station List View...");
        nearbyStationsListView = (ListView) findViewById(R.id.listViewNearbyStations);
        nearbyStationsAdapter = new NearbyStationsAdapter(this, nearbyStations);
        nearbyStationsListView.setAdapter(nearbyStationsAdapter);
        nearbyStationsAdapter.notifyDataSetChanged();

        // Update Control list view every x times the location updates
        // Calling DB takes time -> every time the location updates is to fast
        if (locationUpdateCounter % 10 == 0){
            updateControlLV();
        }

    }

    //--- Data Functions ---//
    // Read CSV file and put into a list of created object
    private void readStationData() {
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

                //Log.d("MyActivity", "Just created:" + sample.toString());
            }
        } catch (IOException e) {
            Log.wtf("MyActivity", "Error reading data file on line" + line, e);
            e.printStackTrace();
        }
    }

    //--- List Functions ---//
    // Check the nearby stations -> adds them to the corresponding list
    private void checkNearbyStations(Location currentLocation){
        // First clear previous List
        nearbyStations.clear();
        // Loop over List with station objects
        for (int i = 0; i < stationData.size(); i++){
            // Create Location object and add data from stationData List -> use build in .distanceTo function
            Location stationLocation = new Location(stationData.get(i).getStation());
            stationLocation.setLatitude(stationData.get(i).getLatitude());
            stationLocation.setLongitude(stationData.get(i).getLongitude());
            currentLocation.setLatitude(currentLatitude); // test location
            currentLocation.setLongitude(currentLongitude); // test location
            // Calculate distance
            double distance = currentLocation.distanceTo(stationLocation);
            //Log.d("MyActivity", "Distance:" + distance);
            // Check if distance is in radius (m)
            if (distance < DISTANCE_RADIUS) {
                // Create nearbyStation object
                NearbyStations nearbyStation = new NearbyStations();
                nearbyStation.setLatitude(stationData.get(i).getLatitude());
                nearbyStation.setLongitude(stationData.get(i).getLongitude());
                nearbyStation.setStation(stationData.get(i).getStation());
                nearbyStation.setDistance(distance);
                // Add it to the list
                nearbyStations.add(nearbyStation);
                //textField.setText("\nStation Name: " + nearbyStation.getStation() + ", " + "Distance: "+ df.format(nearbyStation.getDistance()) + "m") ;
                Log.d("MyActivity", "Just added station: " + nearbyStation.toString());
            }
        }
    }

    // Create the drop down list from the stations in the CSV file
    private void getStationStringList(){
        for (int i = 0; i < stationData.size(); i++){
            autoCompleteStations[i] = stationData.get(i).getStation();
        }
    }

    // Check if user is within close proximity from a station and ask if there is a control -> pop up window
    private void checkNearbyControlStations(){
        controlStationsToCheck.clear();
        Log.d("MyActivity", "Size: " + nearbyStations.size());
        for(int i = 0; i < nearbyStations.size(); i++){
            Log.d("MyActivity", "Distance: " + nearbyStations.get(i).getDistance());
            if(nearbyStations.get(i).getDistance() <= CONTROL_RADIUS){
                Log.d("MyActivity", "Within Control Radius: " + nearbyStations.get(i).getStation());
                // add it to the nearby control stations list to be checked
                controlStationsToCheck.add(nearbyStations.get(i).getStation());
            }
        }
    }

    // --- Debug Functions --- //
    private void printNearbyStationList(){
        Log.d("MyActivity", "Printing nearby stations... ");
        for (int i = 0; i < nearbyStations.size(); i++){
            Log.d("MyActivity", "In nearby stations list: " + nearbyStations.get(i).getStation());
        }
    }

    private void printStringList(List<String> stationList){
        Log.d("MyActivity", "Printing String List... ");
        for (int i = 0; i < stationList.size(); i++){
            Log.d("MyActivity", "Station in List: " + stationList.get(i));
        }
    }

    //--- Firebase Functions ---//
    // Custom call back -> needed because the Listener functions work asynchronous
    public interface FirestoreCallback{
        void onCallback(List<String> currentControlStations);
    }
    // Context function
    private Context getContext(){
        return (Context)this;
    }
    // Add a station to the database
    public void addToDatabase(Map<String, Object> collection, String dataElement){
        collection.put(document, dataElement);
        firestoreDB.collection(collectionName).add(collection)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("MyActivity", "Added to database: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("MyActivity", "Error adding to database", e);
                    }
                });
    }

    // Delete a specified item from the database -> already checked if in database
    public void removeFromDatabase(String dataElement){
        // first get the corresponding documentID to the dataElement
        firestoreDB.collection(collectionName)
                .whereEqualTo(document, dataElement)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && !task.getResult().isEmpty()){
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            String documentID = documentSnapshot.getId();
                            // delete corresponding document
                            firestoreDB.collection(collectionName)
                                    .document(documentID)
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d("MyActivity", "Removed from database: " + documentID);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("MyActivity", "Error occurred when removing document");
                                        }
                                    });
                        }
                    }
                });
    }

    // Get all items from database corresponding to the document name (Station Name)
    public void retrieveFromDatabase(FirestoreCallback firestoreCallback){
        dataDB.clear();
        callBackDone = false;
        Log.d("MyActivity", "Retrieving latest list from database...");
        firestoreDB.collection(collectionName).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                String dataElement = documentSnapshot.getString(document);
                                dataDB.add(dataElement);
                            }
                            //printStringList(dataDB);
                            firestoreCallback.onCallback(dataDB);
                        }
                    }
                });
    }

    //Get latest list from db and show in control list view -> function that calls the function
    private void updateControlLV(){
        // Control Station List View
        Log.d("MyActivity", "Setting Current Control Stations List View...");
        // Get latest control stations from the firestone database and set the listview
        retrieveFromDatabase(new FirestoreCallback() {
            @Override
            public void onCallback(List<String> currentControlStationsDB) {
                //Log.d("MyActivity", "Call Back from Database: Done");
                controlStationsListView = (ListView) findViewById(R.id.listViewControls);
                controlStationsAdapter = new ControlStationsAdapter(getContext(), currentControlStationsDB);
                controlStationsListView.setAdapter(controlStationsAdapter);
                controlStationsAdapter.notifyDataSetChanged();
            }
        });
    }

    //--- Random Functions ---//
}