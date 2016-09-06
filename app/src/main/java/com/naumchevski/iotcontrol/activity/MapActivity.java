package com.naumchevski.iotcontrol.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.naumchevski.iotcontrol.R;
import com.naumchevski.iotcontrol.model.LocationItem;
import com.naumchevski.iotcontrol.util.LocationService;

import java.util.List;

public class MapActivity extends Activity {

    private LocationService locationService;
    private MapView map;
    private GraphicsLayer gLayer;
    private Point mappoint;
    private int gId = 0;
    private double latitudeVal = 0;
    private double logitudeVal = 0;

    private EditText mSearchEditText;
    private String mMapViewState;
    private GraphicsLayer mLocationLayer;
    private static ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        locationService = LocationService.getLocationManager(getApplicationContext());

        // Setup and show progress dialog
        mProgressDialog = new ProgressDialog(this) {
            @Override
            public void onBackPressed() {
                // Back key pressed - just dismiss the dialog
                mProgressDialog.dismiss();
            }
        };

        mSearchEditText = (EditText) findViewById(R.id.searchEditText);
        mSearchEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    onSearchButtonClicked(mSearchEditText);
                    return true;
                }

                return false;
            }
        });

        map = (MapView) findViewById(R.id.map);
        gLayer = new GraphicsLayer();

        map.addLayer(new ArcGISTiledMapServiceLayer(
                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer"));

        map.addLayer(gLayer);

        mLocationLayer = new GraphicsLayer();
        map.addLayer(mLocationLayer);

        // set logo and enable wrap around
        map.setEsriLogoVisible(true);
        map.enableWrapAround(true);

        map.setOnStatusChangedListener(new OnStatusChangedListener() {

            public void onStatusChanged(Object source, STATUS status) {
                if (source == map && status == STATUS.INITIALIZED) {
                    LocationDisplayManager ldm = map.getLocationDisplayManager();
                    ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    ldm.start();

                    if (mMapViewState == null) {
                        Log.i("Location",
                                "map.setOnStatusChangedListener() status="
                                        + status.toString());
                    } else {
                        map.restoreState(mMapViewState);
                    }
                }

            }

        });

        map.setOnSingleTapListener(new OnSingleTapListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSingleTap(final float x, final float y) {
                if (mappoint != null) {
                    gLayer.removeGraphic(gId);
                }
                mappoint = map.toMapPoint(x, y);

                Log.i("Location", String.format("My longitude = %f, latitude = %f",
                        locationService.getLongitude(),
                        locationService.getLatitude()));


                SpatialReference sp = SpatialReference.create(4326);
                Point latLogPt = (Point) GeometryEngine.project(mappoint, map.getSpatialReference(), sp);

                latitudeVal = latLogPt.getY();
                logitudeVal =latLogPt.getX();
                Log.i("Location", String.format("Map longitude = %f, latitude = %f",
                        logitudeVal,
                        latitudeVal));

                Graphic g = new Graphic(mappoint, new SimpleMarkerSymbol(
                        Color.GREEN, 50, SimpleMarkerSymbol.STYLE.CIRCLE));

                gId = gLayer.addGraphic(g);
            }
        });

        Button selectLocationButton = (Button) this.findViewById(R.id.select_location_button);
        selectLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (logitudeVal == 0 && latitudeVal == 0) {
                    showAlert("First please select location on map");
                } else {
                    LocationItem locationItem = new LocationItem((float) logitudeVal, (float) latitudeVal);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("locationItem", locationItem);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            }
        });

        Button cancelButton = (Button) this.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void onSearchButtonClicked(View view) {
        // Hide virtual keyboard
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

        // obtain address and execute locator task
        String address = mSearchEditText.getText().toString();
        executeLocatorTask(address);
    }

    private void executeLocatorTask(String address) {
        // Create Locator parameters from single line address string
        LocatorFindParameters findParams = new LocatorFindParameters(address);

        // Use the center of the current map extent as the find location point
        findParams.setLocation(map.getCenter(),
                map.getSpatialReference());

        // Calculate distance for find operation
        Envelope mapExtent = new Envelope();
        map.getExtent().queryEnvelope(mapExtent);
        // assume map is in meters, other units wont work, double current
        // envelope
        double distance = (mapExtent != null && mapExtent.getWidth() > 0) ? mapExtent
                .getWidth() * 2 : 10000;
        findParams.setDistance(distance);
        findParams.setMaxLocations(2);

        // Set address spatial reference to match map
        findParams.setOutSR(map.getSpatialReference());

        // Execute async task to find the address
        new LocatorAsyncTask().execute(findParams);
    }

    private class LocatorAsyncTask extends
            AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
        private Exception mException;

        public LocatorAsyncTask() {
        }

        @Override
        protected void onPreExecute() {
            // Display progress dialog on UI thread
            mProgressDialog.setMessage(getString(R.string.address_search));
            mProgressDialog.show();
        }

        @Override
        protected List<LocatorGeocodeResult> doInBackground(
                LocatorFindParameters... params) {
            // Perform routing request on background thread
            mException = null;
            List<LocatorGeocodeResult> results = null;

            // Create locator using default online geocoding service and tell it
            // to find the given address
            Locator locator = Locator.createOnlineLocator();
            try {
                results = locator.find(params[0]);
            } catch (Exception e) {
                mException = e;
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<LocatorGeocodeResult> result) {
            // Display results on UI thread
            mProgressDialog.dismiss();
            if (mException != null) {
                Log.w("Location", "LocatorSyncTask failed with:");
                mException.printStackTrace();
                Toast.makeText(MapActivity.this,
                        getString(R.string.addressSearchFailed),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (result.size() == 0) {
                Toast.makeText(MapActivity.this,
                        getString(R.string.noResultsFound), Toast.LENGTH_LONG)
                        .show();
            } else {
                // Use first result in the list
                LocatorGeocodeResult geocodeResult = result.get(0);

                // get return geometry from geocode result
                Point resultPoint = geocodeResult.getLocation();
                // create marker symbol to represent location
                SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(
                        Color.RED, 16, SimpleMarkerSymbol.STYLE.CROSS);
                // create graphic object for resulting location
                Graphic resultLocGraphic = new Graphic(resultPoint,
                        resultSymbol);
                // add graphic to location layer
                mLocationLayer.addGraphic(resultLocGraphic);

                // create text symbol for return address
                String address = geocodeResult.getAddress();
                TextSymbol resultAddress = new TextSymbol(20, address,
                        Color.BLACK);
                // create offset for text
                resultAddress.setOffsetX(-4 * address.length());
                resultAddress.setOffsetY(10);
                // create a graphic object for address text
                Graphic resultText = new Graphic(resultPoint, resultAddress);
                // add address text graphic to location graphics layer
                mLocationLayer.addGraphic(resultText);

                // Zoom map to geocode result location
                map.zoomToResolution(geocodeResult.getLocation(), 2);
            }
        }

    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage(message)
                .setNegativeButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMapViewState = map.retainState();
        map.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start the MapView running again
        if (map != null) {
            map.unpause();
            if (mMapViewState != null) {
                map.restoreState(mMapViewState);
            }
        }
    }
}
