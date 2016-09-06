package com.naumchevski.iotcontrol.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.koushikdutta.async.http.WebSocket;
import com.naumchevski.iotcontrol.R;
import com.naumchevski.iotcontrol.model.CounterItem;
import com.naumchevski.iotcontrol.model.LocationItem;

public class InfoCounterActivity extends Activity {

    private EditText nameText;
    private EditText controlIdText;
    private EditText shareIdText;
    private EditText valueText;

    Button saveChangesButton;
    Button deleteChannelButton;
    Button cancelButton;
    CheckBox locationCheckBox;

    private boolean hasLocationRestriction = false;
    private LocationItem locationItem;

    private Boolean changeLocationRestriction = false;
    private Boolean deleteChannel = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_counter);

        nameText = (EditText) this.findViewById(R.id.nameText);
        controlIdText = (EditText) this.findViewById(R.id.controlIdText);
        shareIdText = (EditText) this.findViewById(R.id.shareIdText);
        valueText = (EditText) this.findViewById(R.id.valueText);

        Intent intent = getIntent();
        CounterItem item = (CounterItem) intent.getSerializableExtra("counterItem");

        nameText.setText(getResources().getString(R.string.Name) + ": " + item.getName());
        controlIdText.setText(getResources().getString(R.string.control_id) + ": " + item.getControlId());
        shareIdText.setText(getResources().getString(R.string.share_id) + ": " + item.getShareId());
        valueText.setText(getResources().getString(R.string.value) + ": " + item.getValue());

        cancelButton = (Button) this.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveChangesButton = (Button) this.findViewById(R.id.saveChangesButton);
        saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationItem != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("locationItem", locationItem);
                    resultIntent.putExtra("counterItemPosition", getIntent().getIntExtra("counterItemPosition", -1));
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("counterItemPosition", getIntent().getIntExtra("counterItemPosition", -1));
                    resultIntent.putExtra("deleteLocationRestriction", true);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            }
        });

        deleteChannelButton = (Button) this.findViewById(R.id.deleteChannelButton);
        deleteChannelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteConfirmationDialog();
                if (deleteChannel) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("counterItemPosition", getIntent().getIntExtra("counterItemPosition", -1));
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            }
        });

        locationCheckBox = (CheckBox) findViewById(R.id.locationRestrictionCheckBox);
        locationCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    Intent intent = new Intent(InfoCounterActivity.this, MapActivity.class);
                    InfoCounterActivity.this.startActivityForResult(intent, 0);
                } else {
                    if (hasLocationRestriction) {
                        saveChangesButton.setVisibility(View.VISIBLE);
                        deleteChannelButton.setVisibility(View.GONE);
                    }
                }
            }
        });
        hasLocationRestriction = item.getLocationItem() != null;
        locationCheckBox.setChecked(hasLocationRestriction);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            locationItem = (LocationItem) data.getSerializableExtra("locationItem");
            if (locationItem != null) {
                saveChangesButton.setVisibility(View.VISIBLE);
                deleteChannelButton.setVisibility(View.GONE);
                locationCheckBox.setChecked(true);
            }
        } else {
            locationCheckBox.setChecked(false);
        }
    }

    @Override
    public void onBackPressed() {

    }

    private void locationConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Confirmation")
                .setMessage("Save changes?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeLocationRestriction = true;
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeLocationRestriction = false;
                    }
                })
                .show();
    }

    private void deleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Confirmation")
                .setMessage("Delete channel?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteChannel = true;
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteChannel = false;
                    }
                })
                .show();
    }
}
