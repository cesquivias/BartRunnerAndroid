package com.dougkeen.bart.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A simple class to combine the data of {@link StationPair} with the first
 * {@link Departure} used in list to show the station and departure information.
 */
public class StationPairDeparture implements Parcelable {
    private final StationPair stationPair;
    private Departure firstDeparture;

    public StationPairDeparture(StationPair stationPair) {
        this.stationPair = stationPair;
        firstDeparture = null;
    }

    public StationPairDeparture(Parcel in) {
        stationPair = in.readParcelable(getClass().getClassLoader());
        firstDeparture = in.readParcelable(getClass().getClassLoader());
    }

    public StationPair getStationPair() {
        return stationPair;
    }

    public Departure getFirstDeparture() {
        return firstDeparture;
    }

    public void setFirstDeparture(Departure firstDeparture) {
        this.firstDeparture = firstDeparture;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(stationPair, flags);
        dest.writeParcelable(firstDeparture, flags);
    }

    public static final Parcelable.Creator<StationPairDeparture> CREATOR = new Parcelable.Creator<StationPairDeparture>() {
        public StationPairDeparture createFromParcel(Parcel in) {
            return new StationPairDeparture(in);
        }

        public StationPairDeparture[] newArray(int size) {
            return new StationPairDeparture[size];
        }
    };
}
