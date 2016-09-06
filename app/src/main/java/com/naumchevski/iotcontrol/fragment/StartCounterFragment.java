package com.naumchevski.iotcontrol.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.google.gson.Gson;
import com.naumchevski.iotcontrol.R;
import com.naumchevski.iotcontrol.model.CounterDTO;
import com.naumchevski.iotcontrol.model.CounterItem;
import com.naumchevski.iotcontrol.util.HTTPUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import info.hoang8f.android.segmented.SegmentedGroup;

public class StartCounterFragment extends Fragment {

    private EditText nameField;
    private int controlType;

    public StartCounterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_start_counter, container, false);

        nameField = (EditText) rootView.findViewById(R.id.nameField);
        SegmentedGroup segmentedControl = (SegmentedGroup) rootView.findViewById(R.id.segmentedControl);
        segmentedControl.setTintColor(Color.GRAY);
        final RadioButton switchTypeButton = (RadioButton) rootView.findViewById(R.id.segmentedControlButton1);
        final RadioButton seekBarTypeButton = (RadioButton) rootView.findViewById(R.id.segmentedControlButton2);

        Button startChannelButton = (Button) rootView.findViewById(R.id.startChannelButton);
        startChannelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(nameField.getText())) {
                    showAlert("Please provide a name for the counter.");
                    return;
                }

                if (!switchTypeButton.isChecked() && !seekBarTypeButton.isChecked()) {
                    showAlert("Please select a type of the counter.");
                    return;
                }

                if (switchTypeButton.isChecked()) {
                    controlType = CounterItem.COUNTER_ITEM_SWITCH;
                }
                if (seekBarTypeButton.isChecked()) {
                    controlType = CounterItem.COUNTER_ITEM_SEEK_BAR;
                }

                String createChannelUrl = "http://naumchevski.com/channel/create";
                ConnectivityManager connMgr = (ConnectivityManager)
                        getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new CreateChannelTask().execute(createChannelUrl, nameField.getText().toString());
                } else {
                    showAlert("No network connection available.");
                }
            }
        });

        Button cancelButton = (Button) rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }

            ;
        });

        return rootView;
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
                showAlert("Cannot start new channel right now, please try again later");
                return;
            }

            Gson gson = new Gson();
            Reader reader = new InputStreamReader(source);
            CounterDTO counter = gson.fromJson(reader, CounterDTO.class);

            CounterItem counterItem = new CounterItem(
                    nameField.getText().toString(),
                    controlType,
                    counter.getControlId(),
                    counter.getShareId());

            Intent resultIntent = new Intent();
            resultIntent.putExtra("counterItem", counterItem);

            getActivity().setResult(Activity.RESULT_OK, resultIntent);
            getActivity().finish();
        }
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Error")
                .setMessage(message)
                .setNegativeButton("OK", null)
                .show();
    }

    private boolean createCounter(String name) {
        return true;
    }
}