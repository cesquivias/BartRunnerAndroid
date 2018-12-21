package com.dougkeen.bart.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.Ticker;
import com.dougkeen.bart.controls.Ticker.TickSubscriber;
import com.dougkeen.bart.data.StationPairDepartureAdapter;
import com.dougkeen.bart.data.StationPairDepartureSynchronizer;
import com.dougkeen.bart.model.Alert;
import com.dougkeen.bart.model.Alert.AlertList;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.model.StationPairDeparture;
import com.dougkeen.bart.networktasks.AlertsClient;
import com.dougkeen.bart.networktasks.ElevatorClient;
import com.dougkeen.bart.networktasks.GetRouteFareTask;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.rest.spring.annotations.RestService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@EActivity(R.layout.main)
public class RoutesListActivity extends AppCompatActivity
        implements TickSubscriber, StationPairDepartureSynchronizer.OnEtdChangeListener,
        StationPairDepartureAdapter.OnStationPairClickListener {
    private static final String NO_DELAYS_REPORTED = "No delays reported";

    private static final TimeZone PACIFIC_TIME = TimeZone
            .getTimeZone("America/Los_Angeles");

    private static final String TAG = "RoutesListActivity";

    @InstanceState
    StationPairDeparture mCurrentlySelectedStationPair;

    @InstanceState
    String mCurrentAlerts;

    private ActionMode mActionMode;

    private List<StationPairDeparture> stationPairDepartures;
    private StationPairDepartureAdapter stationPairDepartureAdapter;
    private StationPairDepartureSynchronizer synchronizer;

    @App
    BartRunnerApplication app;

    @RestService
    AlertsClient alertsClient;

    @RestService
    ElevatorClient elevatorClient;

    @ViewById(android.R.id.list)
    RecyclerView listView;

    @ViewById(R.id.quickLookupButton)
    Button quickLookupButton;

    @ViewById(R.id.alertMessages)
    TextView alertMessages;

    @ViewById(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;

    @Click(R.id.quickLookupButton)
    void quickLookupButtonClick() {
        DialogFragment dialog = new QuickRouteDialogFragment();
        dialog.show(getSupportFragmentManager(), QuickRouteDialogFragment.TAG);
    }


    @Override
    public void onStationPairClicked(StationPairDeparture item) {
        Intent intent = new Intent(RoutesListActivity.this,
                ViewDeparturesActivity.class);
        intent.putExtra(Constants.STATION_PAIR_EXTRA, item.getStationPair());
        startActivity(intent);
    }

    @Override
    public void onStationPairLongClicked(StationPairDeparture stationPairDeparture) {
        if (mActionMode != null) {
            mActionMode.finish();
        }

        mCurrentlySelectedStationPair = stationPairDeparture;

        startContextualActionMode();
    }

//    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
//        @Override
//        public void drop(int from, int to) {
//            if (from == to)
//                return;
//
//            StationPair item = mRoutesAdapter.getItem(from);
//
//            mRoutesAdapter.move(item, to);
//            mRoutesAdapter.notifyDataSetChanged();
//        }
//    };
//
//    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
//        @Override
//        public void remove(final int which) {
//            final StationPair stationPair = mRoutesAdapter.getItem(which);
//            mRoutesAdapter.remove(stationPair);
//            mRoutesAdapter.notifyDataSetChanged();
//            showRouteDeletedSnackbar(which, stationPair);
//        }
//    };

    @AfterViews
    void afterViews() {
        setTitle(R.string.favorite_routes);

        List<StationPair> favorites = app.getFavorites();
        stationPairDepartures = new ArrayList<>(favorites.size());
        for (StationPair pair : favorites) {
            stationPairDepartures.add(new StationPairDeparture(pair));
        }
        stationPairDepartureAdapter = new StationPairDepartureAdapter(stationPairDepartures);
        listView.setAdapter(stationPairDepartureAdapter);
        listView.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        listView.addItemDecoration(itemDecoration);
        stationPairDepartureAdapter.setOnItemClickListener(this);

//        listView.setEmptyView(findViewById(android.R.id.empty));

//        listView.setDropListener(onDrop);
//        listView.setRemoveListener(onRemove);


        if (mCurrentAlerts != null) {
            showAlertMessage(mCurrentAlerts);
        }

        synchronizer = new StationPairDepartureSynchronizer(stationPairDepartures);
        synchronizer.setEtdChangeListener(this);
        synchronizer.open(this);
        refreshFares();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasActionMode")) {
                startContextualActionMode();
            }
        }

        Ticker.getInstance().addSubscriber(this, getApplicationContext());
    }

    @Override
    public void onDepartureUpdate(StationPairDeparture stationPairDeparture) {
        stationPairDepartureAdapter.notifyDataSetChanged();
    }

    void addFavorite(StationPair pair) {
        StationPairDeparture pairDeparture = new StationPairDeparture(pair);
        stationPairDepartures.add(pairDeparture);
        stationPairDepartureAdapter.notifyItemInserted(stationPairDepartures.size());
        synchronizer.add(pairDeparture);
    }

    private void refreshFares() {
        for (int i = stationPairDepartures.size() - 1; i >= 0; i--) {
            final StationPairDeparture stationPairDeparture = stationPairDepartures.get(i);
            final StationPair stationPair = stationPairDeparture.getStationPair();

            Calendar now = Calendar.getInstance();
            Calendar lastUpdate = Calendar.getInstance();
            lastUpdate.setTimeInMillis(stationPair.getFareLastUpdated());

            now.setTimeZone(PACIFIC_TIME);
            lastUpdate.setTimeZone(PACIFIC_TIME);

            // Update every day
            if (now.get(Calendar.DAY_OF_YEAR) != lastUpdate.get(Calendar.DAY_OF_YEAR)
                    || now.get(Calendar.YEAR) != lastUpdate.get(Calendar.YEAR)) {
                GetRouteFareTask fareTask = new GetRouteFareTask() {
                    @Override
                    public void onResult(String fare) {
                        stationPair.setFare(fare);
                        stationPair.setFareLastUpdated(System.currentTimeMillis());
//                        getListAdapter().notifyDataSetChanged();
                        // TODO notify more granularly
                        stationPairDepartureAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Exception exception) {
                        // Ignore... we can do this later
                    }
                };
                fareTask.execute(new GetRouteFareTask.Params(stationPair
                        .getOrigin(), stationPair.getDestination()));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasActionMode", mActionMode != null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Ticker.getInstance().startTicking(this);
        startEtdListeners();
    }

    private void startEtdListeners() {
        synchronizer.setUpEtdListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        synchronizer.clearEtdListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Ticker.getInstance().stopTicking(this);
        List<StationPair> favorites = new ArrayList<>(stationPairDepartures.size());
        for (StationPairDeparture stationPairDeparture : stationPairDepartures) {
            favorites.add(stationPairDeparture.getStationPair());
        }
        app.setFavorites(favorites);
        app.saveFavorites();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mRoutesAdapter != null) {
//            mRoutesAdapter.close();
//        }
        synchronizer.close(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Ticker.getInstance().startTicking(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.routes_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private MenuItem elevatorMenuItem;
    private View origElevatorActionView;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_favorite_menu_button) {
            new AddRouteDialogFragment().show(getSupportFragmentManager(),
                    AddRouteDialogFragment.TAG);
            return true;
        } else if (itemId == R.id.view_system_map_button) {
            startActivity(new Intent(this, ViewMapActivity.class));
            return true;
        } else if (itemId == R.id.elevator_button) {
            elevatorMenuItem = item;
            fetchElevatorInfo();
            origElevatorActionView = MenuItemCompat.getActionView(elevatorMenuItem);
            MenuItemCompat.setActionView(elevatorMenuItem, R.layout.progress_spinner);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Background
    void fetchAlerts() {
        Log.d(TAG, "Fetching alerts");
        AlertList alertList;
        try {
            alertList = alertsClient.getAlerts();
        } catch (Exception e) {
            // Try again later
            Log.w(TAG, "Could not fetch alerts", e);
            return;
        }
        if (alertList.hasAlerts()) {
            StringBuilder alertText = new StringBuilder();
            boolean firstAlert = true;
            for (Alert alert : alertList.getAlerts()) {
                if (!firstAlert) {
                    alertText.append("\n\n");
                }
                alertText.append(alert.getPostedTime()).append("\n");
                alertText.append(alert.getDescription());
                firstAlert = false;
            }
            showAlertMessage(alertText.toString());
        } else if (alertList.areNoDelaysReported()) {
            showAlertMessage(NO_DELAYS_REPORTED);
        } else {
            hideAlertMessage();
        }
    }

    @UiThread
    void hideAlertMessage() {
        mCurrentAlerts = null;
        alertMessages.setVisibility(View.GONE);
    }

    @UiThread
    void showAlertMessage(String messageText) {
        if (messageText == null) {
            hideAlertMessage();
            return;
        } else if (messageText.equals(NO_DELAYS_REPORTED)) {
            alertMessages.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_allgood, 0, 0, 0);
        } else {
            alertMessages.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_warn, 0, 0, 0);
        }
        mCurrentAlerts = messageText;
        alertMessages.setText(messageText);
        alertMessages.setVisibility(View.VISIBLE);
    }

    @Background
    void fetchElevatorInfo() {
        String elevatorMessage = elevatorClient.getElevatorMessage();
        if (elevatorMessage != null) {
            showElevatorMessage(elevatorMessage);
        }
        resetElevatorMenuGraphic();
    }

    @UiThread
    void resetElevatorMenuGraphic() {
        ActivityCompat.invalidateOptionsMenu(this);
        MenuItemCompat.setActionView(elevatorMenuItem, origElevatorActionView);
    }

    @UiThread
    void showElevatorMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle("Elevator status");
        builder.show();
    }

    private void startContextualActionMode() {
        mActionMode = startSupportActionMode(new RouteActionMode());
        mActionMode.setTitle(mCurrentlySelectedStationPair.getStationPair().getOrigin().name);
        mActionMode.setSubtitle("to "
                + mCurrentlySelectedStationPair.getStationPair().getDestination().name);
    }

//    private void showRouteDeletedSnackbar(final int which, final StationPair stationPair) {
//        Snackbar.make(coordinatorLayout, R.string.snackbar_route_deleted, Snackbar.LENGTH_LONG)
//                .setAction(R.string.undo, new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        mRoutesAdapter.insert(stationPair, which);
//                        mRoutesAdapter.notifyDataSetChanged();
//                    }
//                })
//                .show();
//    }

    private final class RouteActionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.route_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.view) {
                Intent intent = new Intent(RoutesListActivity.this,
                        ViewDeparturesActivity.class);
                intent.putExtra(Constants.STATION_PAIR_EXTRA,
                        mCurrentlySelectedStationPair);
                startActivity(intent);
                mode.finish();
                return true;
            } else if (item.getItemId() == R.id.delete) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(
                        RoutesListActivity.this);
                builder.setCancelable(false);
                builder.setMessage("Are you sure you want to delete this route?");
                builder.setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                int position = stationPairDepartures.indexOf(mCurrentlySelectedStationPair);
                                stationPairDepartures.remove(position);
                                stationPairDepartureAdapter.notifyItemRemoved(position);
                                mCurrentlySelectedStationPair = null;
                                mActionMode.finish();
                                dialog.dismiss();
                            }
                        });
                builder.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
                builder.show();
                return false;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }

    }

    @Override
    public int getTickInterval() {
        return 90;
    }

    @Override
    public void onTick(long mTickCount) {
        fetchAlerts();
    }
}
