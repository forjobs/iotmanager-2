package com.naumchevski.iotcontrol.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.naumchevski.iotcontrol.R;
import com.naumchevski.iotcontrol.adapter.CounterAdapter;
import com.naumchevski.iotcontrol.model.CounterDTO;
import com.naumchevski.iotcontrol.model.CounterItem;
import com.naumchevski.iotcontrol.model.LocationItem;
import com.naumchevski.iotcontrol.type.VoiceActionType;
import com.naumchevski.iotcontrol.util.CounterUtil;
import com.naumchevski.iotcontrol.util.Foreground;
import com.naumchevski.iotcontrol.util.HTTPUtil;
import com.naumchevski.iotcontrol.util.LocationService;
import com.naumchevski.iotcontrol.util.VoiceCmdUtil;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ListView listView;
    private List<CounterItem> counterItems;
    private List<CounterItem> reservedCounterItems;
    private WebSocket webSocket;
    private int selectedItem = 0;
    private float currentDistance = 0;
    private String nextSendMessage = null;

    private final static String LOGTAG = "MainActivity";
    private static final int SPEECH_REQUEST_CODE = 1234;
    private static final int DEVICE_REQUEST_CODE = 1235;

    private SensorManager mSensorManager;
    private Sensor mProximity;

    private Timer timer;
    private TimerTask timerTask;
    private final Handler handler = new Handler();

    private Timer voiceTimer;
    private TimerTask voiceTimerTask;
    private final Handler voiceHandler = new Handler();

    LocationService locationService;

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        locationService = LocationService.getLocationManager(getApplicationContext());

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        listView = (ListView) MainActivity.this.findViewById(R.id.listView);

        listView.setLongClickable(true);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                Intent intent = new Intent(MainActivity.this, InfoCounterActivity.class);
                intent.putExtra("counterItem", counterItems.get(pos));
                intent.putExtra("counterItemPosition", pos);
                MainActivity.this.startActivityForResult(intent, 0);
                return true;
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ManageCounterActivity.class);
                MainActivity.this.startActivityForResult(intent, 0);
            }
        });
        fab.setLongClickable(true);
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (webSocket == null) {
                    new AttachChannelTask().execute("http://192.168.4.1/");

                }
                return true;
            }
        });

        FloatingActionButton fabVoiceCmd = (FloatingActionButton) findViewById(R.id.voice_cmd);
        fabVoiceCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displaySpeechRecognizer();
            }
        });

        Foreground.init(getApplication());
        Foreground.get(this).addListener(new Foreground.Listener() {
            public void onBecameForeground() {
                connectSocket();
            }

            public void onBecameBackground() {
                disconnectSocket();
            }
        });

        loadCounters();
        updateUI();
        connectSocket();

        loadReservedCounters();
    }

    private class AttachChannelTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String output = null;
            // params comes from the execute() call: params[0] is the url.
            try {
                InputStream source = HTTPUtil.getChannelInfo(urls[0]);
                try {

                    BufferedReader r = new BufferedReader(new InputStreamReader(source));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    output = total.toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Log.e("", "Unable to retrieve web page. URL may be invalid.");
                return null;
            }
            return output;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String output) {
            if(StringUtils.isBlank(output)) {
                showAlert("Error(2): Device not found. Please check the device and try again.");
                return;
            }

            String name = "";
            int type = -1;
            if (StringUtils.isNotEmpty(output)) {
                String[] split = output.split(";");
                if (split.length == 2) {
                    name = split[0];
                    type = Integer.valueOf(split[1].replaceAll("([^\\d])", ""));
                }
            }

            if(StringUtils.isBlank(name) || type == -1) {
                showAlert("Device not found. Please check the device and try again.");
                return;
            }

            if (reservedCounterItems.size() > 0) {
                Intent intent = new Intent(MainActivity.this, ConfigDeviceActivity.class);
                CounterItem item = reservedCounterItems.get(0);
                item.setName(name);
                item.setItemType(type);
                intent.putExtra("reservedCounterItem", item);
                MainActivity.this.startActivityForResult(intent, DEVICE_REQUEST_CODE);
            } else {
                showAlert("Please restart application.");
            }
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        currentDistance = distance;
        if (distance == 0) {
            startTimer();
        } else {
            //stop the timer, if it's not already null
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    displaySpeechRecognizer();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    displaySpeechRecognizer();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            handleVoiceCommand(spokenText);
        } else if (requestCode == DEVICE_REQUEST_CODE && resultCode == RESULT_OK) {
            CounterItem counterItem = (CounterItem) data.getSerializableExtra("counterItem");
            counterItems.add(counterItem);
            reservedCounterItems.remove(0);
            saveReservedCounters();
            saveCounters();
            disconnectSocket();
            connectSocket();
            showAlert("New device has been configured. Please RESTART your device.");
        } else if (resultCode == Activity.RESULT_OK) {
            if (data.getSerializableExtra("counterItem") != null) {
                CounterItem counterItem = (CounterItem) data.getSerializableExtra("counterItem");
                counterItems.add(counterItem);
                saveCounters();
                disconnectSocket();
                connectSocket();
            }
            if (data.getSerializableExtra("locationItem") != null) {
                int position = (int) data.getIntExtra("counterItemPosition", -1);
                LocationItem locationItem = (LocationItem) data.getSerializableExtra("locationItem");
                if (locationItem != null) {
                    CounterItem counterItem = counterItems.get(position);
                    if (counterItem != null) {
                        counterItem.setLocationItem(locationItem);
                        saveCounters();
                        updateUI();
                    }
                }
            } else if (data.getBooleanExtra("deleteLocationRestriction", false)) {
                int position = (int) data.getIntExtra("counterItemPosition", -1);
                CounterItem counterItem = counterItems.get(position);
                if (counterItem != null) {
                    counterItem.setLocationItem(null);
                    saveCounters();
                    updateUI();
                }
            } else if (data.getIntExtra("counterItemPosition", -1) != -1) {
                int position = (int) data.getIntExtra("counterItemPosition", -1);
                counterItems.remove(position);
                saveCounters();
                disconnectSocket();
                connectSocket();
            }

        }
    }

    private void handleVoiceCommand(String spokenText) {
        int counterIndex = VoiceCmdUtil.getCounterIndexFromVoiceCmd(spokenText);
        VoiceActionType voiceActionType = VoiceCmdUtil.getCounterActionFromVoiceCmd(spokenText);

        if (counterIndex == -1 || voiceActionType == VoiceActionType.NO_SUPPORTED) {
            showAlert("Voice command '" + spokenText + "' not supported. Please try with 'one turn on' or 'turn off two'");
            return;
        } else {
            if (counterItems.size() -1  < counterIndex) {
                showAlert("Channel cannot be selected");
                return;
            }
            String msg = counterItems.get(counterIndex).getControlId() + "=" +
                    (VoiceActionType.TURN_ON.equals(voiceActionType)
                            ? CounterItem.MAX_VALUE_STRING : CounterItem.MIN_VALUE_STRING);
            nextSendMessage = msg;
            startVoiceTimer();
            return;
        }
    }

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }


    private void connectSocket() {
        if (webSocket != null) {
            return;
        }
        AsyncHttpClient.getDefaultInstance().websocket(CounterUtil.webSocletURL(counterItems), null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, final WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }

                if (webSocket == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Web socket not created", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                Log.i(LOGTAG, "Websocket created");
                MainActivity.this.webSocket = webSocket;

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String string) {
                        System.out.println("I got a string: " + string);
                        try {
                            JSONObject jObject = new JSONObject(string);
                            Iterator<String> iterator = jObject.keys();

                            for (CounterItem item : counterItems) {
                                String id = item.getControlId();
                                item.setValue(jObject.getInt(id));
                            }
                            updateUI();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                webSocket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        ex.printStackTrace();
                        Toast.makeText(MainActivity.this, "setEndCallback", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void disconnectSocket() {
        if (webSocket == null) {
            return;
        }
        webSocket.close();
        webSocket = null;
        Log.i(LOGTAG, "Websocket destroyed");
        updateUI();
    }

    private void loadCounters() {
        try {
            FileInputStream fis = this.openFileInput("counterItems");
            ObjectInputStream is = new ObjectInputStream(fis);
            counterItems = (ArrayList<CounterItem>) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,
                    "Error while reading the counter items with error: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();

            counterItems = new ArrayList<>();
        }
    }

    private void loadReservedCounters() {
        String createChannelUrl = "http://naumchevski.com/channel/create";
        ConnectivityManager connMgr = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        try {
            FileInputStream fis = this.openFileInput("reservedCounterItems");
            ObjectInputStream is = new ObjectInputStream(fis);
            reservedCounterItems = (ArrayList<CounterItem>) is.readObject();
            is.close();
            fis.close();

            if (reservedCounterItems != null && reservedCounterItems.size() == 0) {
                if (networkInfo != null && networkInfo.isConnected()) {
                    for (int i = 0; i < 10; i++) {
                        new CreateChannelTask().execute(createChannelUrl,
                                "channel " + String.valueOf(new Random().nextInt()));
                    }
                } else {
                    Log.i("","No network connection available.");
                }
            }
        } catch (Exception e) {
            reservedCounterItems = new ArrayList<>();
            if (networkInfo != null && networkInfo.isConnected()) {
                for (int i = 0; i < 10; i++) {
                    new CreateChannelTask().execute(createChannelUrl,
                            "channel " + String.valueOf(new Random().nextInt()));
                }
            } else {
                Log.i("","No network connection available.");
            }
        }

        new android.os.Handler().postDelayed(
                new Runnable() { public void run() {
                    saveReservedCounters();
                }}, 10000);
    }

    private void saveCounters() {
        try {
            FileOutputStream fos = this.openFileOutput("counterItems", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(counterItems);
            os.close();
            fos.close();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,
                    "Error while writing the counter items with error: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void saveReservedCounters() {
        try {
            FileOutputStream fos = this.openFileOutput("reservedCounterItems", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(reservedCounterItems);
            os.close();
            fos.close();
            Log.i("","Reserved counters saved.");
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,
                    "Error while writing the reserved counter items with error: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CounterAdapter adapter = new CounterAdapter(MainActivity.this,
                        0,
                        counterItems,
                        webSocket,
                        locationService);
                listView.setAdapter(adapter);
            }
        });
    }

    private void sendMessage(String message) {
        if (webSocket == null) {
            return;
        }
        Log.i("CounterAdapter", "message: " + message);
        webSocket.send(message);
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage(message)
                .setNegativeButton("OK", null)
                .show();
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 5000);
    }

    public void startVoiceTimer() {
        //set a new Timer
        voiceTimer = new Timer();

        //initialize the TimerTask's job
        initializeVoiceTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        voiceTimer.schedule(voiceTimerTask, 1000, 1000);
    }

    public void initializeVoiceTimerTask() {

        voiceTimerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                voiceHandler.post(new Runnable() {
                    public void run() {
                        if (webSocket != null) {
                            if (nextSendMessage != null) {
                                sendMessage(nextSendMessage);
                                nextSendMessage = null;
                            }
                        }
                    }
                });
            }
        };
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        if (currentDistance == 0 && selectedItem > -1 && selectedItem < counterItems.size()) {
                            int val = counterItems.get(selectedItem).getValue();
                            sendMessage(counterItems.get(selectedItem).getControlId() + "=" +
                                    (val == CounterItem.MIN_VALUE_INT
                                            ? CounterItem.MAX_VALUE_STRING : CounterItem.MIN_VALUE_STRING));
                            selectedItem = -1;
                        }
                    }
                });
            }
        };
    }

    private class CreateChannelTask extends AsyncTask<String, Void, InputStream> {
        @Override
        protected InputStream doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return HTTPUtil.createChannel(urls[0], urls[1]);
            } catch (IOException e) {
                Log.e("", "Unable to retrieve web page. URL may be invalid.");
                return null;
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(InputStream source) {
            if (source == null) {
                Log.e("", "Source not set.");
                return;
            }

            Gson gson = new Gson();
            Reader reader = new InputStreamReader(source);
            CounterDTO counter = gson.fromJson(reader, CounterDTO.class);
            reservedCounterItems.add(new CounterItem(
                    counter.getName(),
                    CounterItem.COUNTER_ITEM_SWITCH,
                    counter.getControlId(),
                    counter.getShareId()
            ));
            Log.i("","Reserved counters added = " + counter.getControlId());
        }
    }

    private void checkPermissions() {
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }

        if(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
    }
}

