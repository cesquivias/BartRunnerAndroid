package com.dougkeen.bart.data;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.model.StationPairDeparture;
import com.dougkeen.bart.services.EtdService;
import com.dougkeen.bart.services.EtdService_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains the listeners to {@link com.dougkeen.bart.services.EtdService} to
 * make sure the latest departure information gets passed along. Properly
 * connects and disconnects listeners based on whether the service is connected
 * or disconnected.
 */
public class StationPairDepartureSynchronizer implements ServiceConnection {
    private final List<StationPairDeparture> stationPairDepartures;
    private boolean mBound = false;
    private EtdService mEtdService;
    private Map<StationPairDeparture, EtdListener> etdListeners;
    private OnEtdChangeListener etdChangeListener;

    public StationPairDepartureSynchronizer(List<StationPairDeparture> stationPairDepartures) {
        this.stationPairDepartures = stationPairDepartures;
        etdListeners = new HashMap<>();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mEtdService = null;
        mBound = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mEtdService = ((EtdService.EtdServiceBinder) service).getService();
        mBound = true;
        setUpEtdListeners();
    }

    private void setUpEtdListeners() {
        if (mBound && mEtdService != null && !areEtdListenersActive()) {
            for (int i = stationPairDepartures.size() - 1; i >= 0; i--) {
                final StationPairDeparture item = stationPairDepartures.get(i);
                etdListeners.put(item, new EtdListener(item, mEtdService));
            }
        }
    }

    public void clearEtdListeners() {
        if (mBound && mEtdService != null) {
            for (EtdListener listener : etdListeners.values()) {
                listener.close(mEtdService);
            }
            etdListeners.clear();
        }
    }

    public void open(Activity activity) {
        activity.bindService(EtdService_.intent(activity).get(),
                this, Context.BIND_AUTO_CREATE);
    }

    public void close(Activity activity) {
        if (mBound) {
            activity.unbindService(this);
        }
    }

    public boolean areEtdListenersActive() {
        return !etdListeners.isEmpty();
    }

    public void add(StationPairDeparture object) {
        if (mEtdService != null && mBound) {
            etdListeners.put(object, new EtdListener(object, mEtdService));
        }
    }

    public void remove(StationPairDeparture object) {
        if (etdListeners.containsKey(object) && mEtdService != null & mBound) {
            etdListeners.get(object).close(mEtdService);
            etdListeners.remove(object);
        }
    }

    public void setEtdChangeListener(OnEtdChangeListener etdChangeListener) {
        this.etdChangeListener = etdChangeListener;
    }

    public interface OnEtdChangeListener {
        void onDepartureUpdate(StationPairDeparture stationPairDeparture);
    }

    private class EtdListener implements EtdService.EtdServiceListener {

        private final StationPairDeparture stationPairDeparture;

        protected EtdListener(StationPairDeparture stationPairDeparture, EtdService etdService) {
            super();
            this.stationPairDeparture = stationPairDeparture;
            etdService.registerListener(this, true);
        }

        protected void close(EtdService etdService) {
            etdService.unregisterListener(this);
        }

        @Override
        public void onETDChanged(List<Departure> departures) {
            for (Departure departure : departures) {
                if (!departure.hasDeparted()) {
                    if (!departure.equals(stationPairDeparture.getFirstDeparture())) {
                        stationPairDeparture.setFirstDeparture(departure);
                        if (etdChangeListener != null) {
                            etdChangeListener.onDepartureUpdate(stationPairDeparture);
                        }
                    }
                    return;
                }
            }
        }

        @Override
        public void onError(String errorMessage) {
        }

        @Override
        public void onRequestStarted() {
        }

        @Override
        public void onRequestEnded() {
        }

        @Override
        public StationPair getStationPair() {
            return stationPairDeparture.getStationPair();
        }
    }
}
