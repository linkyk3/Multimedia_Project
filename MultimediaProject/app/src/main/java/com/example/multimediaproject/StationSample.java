package com.example.multimediaproject;

import android.os.Parcel;
import android.os.Parcelable;

public class StationSample implements Parcelable {
    private double longitude;
    private double latitude;
    private String station;

    public StationSample(Parcel parcel) {
        longitude = parcel.readDouble();
        latitude = parcel.readDouble();
        station = parcel.readString();
    }

    public StationSample(){

    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    @Override
    public String toString() {
        return "StationSample{" +
                "longitude=" + longitude +
                ", latitude=" + latitude +
                ", station='" + station + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(longitude);
        parcel.writeDouble(latitude);
        parcel.writeString(station);
    }

    public static final Parcelable.Creator<StationSample> CREATOR = new Parcelable.Creator<StationSample>(){

        @Override
        public StationSample createFromParcel(Parcel parcel) {
            return new StationSample(parcel);
        }

        @Override
        public StationSample[] newArray(int i) {
            return new StationSample[i];
        }
    };
}
