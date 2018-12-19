package com.dougkeen.bart.model;

/**
 * A simple class to combine the data of {@link StationPair} with the first
 * {@link Departure} used in list to show the station and departure information.
 */
public class StationPairDeparture {
    private final StationPair stationPair;
    private Departure firstDeparture;

    public StationPairDeparture(StationPair stationPair) {
        this.stationPair = stationPair;
        firstDeparture = null;
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
}
