package com.naumchevski.iotcontrol.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.koushikdutta.async.http.WebSocket;
import com.naumchevski.iotcontrol.R;

import java.util.List;

import com.naumchevski.iotcontrol.model.CounterItem;
import com.naumchevski.iotcontrol.util.LocationService;

public class CounterAdapter extends ArrayAdapter<CounterItem> {

    private static final int MAX_RANGE = 100;

    private LayoutInflater inflater;
    private WebSocket webSocket;
    private LocationService locationService;

    private CompoundButton.OnCheckedChangeListener switchChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int position = (int) buttonView.getTag();
            CounterItem item = getItem(position);
            String value = isChecked ? CounterItem.MAX_VALUE_STRING : CounterItem.MIN_VALUE_STRING;
            sendMessage(item.getControlId() + "=" + value);
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int position = (int) seekBar.getTag();
            CounterItem item = getItem(position);
            sendMessage(item.getControlId() + "=" + seekBar.getProgress());
        }
    };

    public CounterAdapter(Context context,
                          int resource, List<CounterItem> objects,
                          WebSocket webSocket,
                          LocationService locationService) {
        super(context, resource, objects);
        this.webSocket = webSocket;
        this.locationService = locationService;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.counter_item, null);
        CounterItem item = getItem(position);

        TextView textView = (TextView) view.findViewById(R.id.cursorTextView);
        textView.setText(item.getName());

        Switch enableSwitch = (Switch) view.findViewById(R.id.enableSwitch);
        enableSwitch.setTag(position);
        enableSwitch.setEnabled(webSocket != null);

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setTag(position);
        seekBar.setEnabled(webSocket != null);

        switch (item.getItemType()) {
            case CounterItem.COUNTER_ITEM_SWITCH:
                seekBar.setVisibility(View.GONE);
                enableSwitch.setVisibility(View.VISIBLE);
                enableSwitch.setChecked(item.getValue() == CounterItem.MAX_VALUE_INT);
                if (item.getLocationItem() != null) {
                    if (!locationService.checkInRange(item.getLocationItem(), MAX_RANGE)) {
                        enableSwitch.setEnabled(false);
                    }
                }
                break;
            case CounterItem.COUNTER_ITEM_SEEK_BAR:
                seekBar.setVisibility(View.VISIBLE);
                seekBar.setProgress(item.getValue());
                enableSwitch.setVisibility(View.GONE);
                if (item.getLocationItem() != null) {
                    if (!locationService.checkInRange(item.getLocationItem(), MAX_RANGE)) {
                        seekBar.setEnabled(false);
                    }
                }
                break;
        }

        enableSwitch.setOnCheckedChangeListener(switchChangedListener);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        view.setFocusable(false);
        return view;
    }

    private void sendMessage(String message) {
        if (webSocket == null) {
            return;
        }
        Log.i("CounterAdapter", "message: " + message);
        webSocket.send(message);
    }
}
