package com.naumchevski.iotcontrol.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.naumchevski.iotcontrol.R;
import com.naumchevski.iotcontrol.model.CounterItem;
import com.naumchevski.iotcontrol.util.HTTPUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ConfigDeviceActivity extends Activity {

    private EditText nameText;
    EditText channelTypeText;
    EditText controlIdText;
    EditText shareIdText;
    EditText hostText;
    EditText ssidText;
    EditText passwordText;

    private CounterItem reservedItem;

    // FIXME: 5/6/2016 move default host in properties file
    private static final String DEFAULT_HOST = "naumchevski.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_device);

        nameText = (EditText) this.findViewById(R.id.nameText);
        channelTypeText = (EditText) this.findViewById(R.id.channelTypeText);
        controlIdText = (EditText) this.findViewById(R.id.controlIdText);
        shareIdText = (EditText) this.findViewById(R.id.shareIdText);
        hostText = (EditText) this.findViewById(R.id.hostText);
        ssidText = (EditText) this.findViewById(R.id.ssidText);
        passwordText = (EditText) this.findViewById(R.id.passwordText);

        Intent intent = getIntent();
        reservedItem = (CounterItem) intent.getSerializableExtra("reservedCounterItem");

        nameText.setText(reservedItem.getName());
        String channelTypeRes = getResources().getString(R.string.type) + ": "
                + (reservedItem.getItemType() == CounterItem.COUNTER_ITEM_SEEK_BAR ?
                "Slider" : "Switch");
        channelTypeText.setText(channelTypeRes);
        final String controlIdRes = getResources().getString(R.string.control_id) + ": "
                + reservedItem.getControlId();
        controlIdText.setText(controlIdRes);
        String sharedIdRes = getResources().getString(R.string.share_id) + ": "
                + reservedItem.getShareId();
        shareIdText.setText(sharedIdRes);
        hostText.setText(DEFAULT_HOST);

        Button cancelButton = (Button) this.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button attachChannelButton = (Button) this.findViewById(R.id.attachChannelButton);
        attachChannelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String host = "http://192.168.4.1/";
                String query = "s?";
                try {
                    query += String.format("ssid=%s&password=%s&cid=%s&host=%s",
                        URLEncoder.encode(ssidText.getText().toString(), "utf-8"),
                        URLEncoder.encode(passwordText.getText().toString(), "utf-8"),
                        URLEncoder.encode(reservedItem.getControlId().toString(), "utf-8"),
                        URLEncoder.encode(hostText.getText().toString(), "utf-8")
                    );
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                new ConfigDeviceTask().execute(host + query);
            }
        });

    }

    private class ConfigDeviceTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... urls) {

            try {
                InputStream source = HTTPUtil.getChannelInfo(urls[0]);
                try {

                    BufferedReader r = new BufferedReader(new InputStreamReader(source));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    String output = total.toString();
                    System.out.println(output);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return  true;
            } catch (IOException e) {
                Log.e("", "Unable to retrieve web page. URL may be invalid.");
                return false;
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Boolean done) {
            if (done) {
                CounterItem counterItem = new CounterItem(
                        nameText.getText().toString(),
                        reservedItem.getItemType(),
                        reservedItem.getControlId(),
                        reservedItem.getShareId());

                Intent resultIntent = new Intent();
                resultIntent.putExtra("counterItem", counterItem);

                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                showAlert("Device is not responding.");
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


}
