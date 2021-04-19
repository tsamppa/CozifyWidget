package com.cozify.cozifywidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class CozifyAppWidgetConnectedActivity extends AppCompatActivity {

    private CozifyApiReal cozifyAPI = new CozifyApiReal(this);
    TextView textViewStatus;
    Button buttonOpenTroubleshooter;
    String selectedHubKey;
    String selectedHubId;
    JSONObject hubNamesJson;
    JSONObject hubkeysJson;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cozify_app_widget_connected);
        cozifyAPI.loadState(0);

        getHubKeys();

        findViewById(R.id.button_open_troubleshooter).setEnabled(false);
        Spinner hubsSpinner = findViewById(R.id.spinner_hubs_for_troubleshooter);
        hubsSpinner.setEnabled(false);
        hubsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Spinner spinner = findViewById(R.id.spinner_hubs_for_troubleshooter);
                Object selectedItem = spinner.getSelectedItem();
                if (selectedItem != null) {
                    String selectedHubName = selectedItem.toString();
                    try {
                        selectedHubKey = hubNamesJson.getString(selectedHubName);
                        if (selectedHubKey != null) {
                            // Open troubleshooter for delected hubKey
                            // Set hubkey as extra for troubleshooter activity
                            setStatus("Selected Hub:" + selectedHubName);
                            findViewById(R.id.button_open_troubleshooter).setEnabled(true);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                findViewById(R.id.button_open_troubleshooter).setEnabled(false);
            }

        });

    }

    static int[] addElement(int[] a, int e) {
        a  = Arrays.copyOf(a, a.length + 1);
        a[a.length - 1] = e;
        return a;
    }

    private void updateAllWidgets() {
        Intent intent = new Intent(this, CozifyAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CozifyAppWidget.class));
        int[] ids2 = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this.getPackageName(), CozifyAppWidgetDouble.class.getName()));
        for (int id: ids2) {
            ids = addElement(ids, id);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void getHubKeys() {
        findViewById(R.id.spinner_hubs_for_troubleshooter).setEnabled(false);
        findViewById(R.id.button_open_troubleshooter).setEnabled(false);
        setStatus("Loading list of Hubs..");
        cozifyAPI.getHubKeys(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    hubkeysJson = resultJson;
                    hubNamesJson = new JSONObject();
                    ArrayList<String> hubs = new ArrayList<>();
                    Iterator<?> keys = hubkeysJson.keys();
                    try {
                        while (keys.hasNext()) {
                            String hubId = (String) keys.next();
                            String hubKey = hubkeysJson.get(hubId).toString();
                            String hubName = CozifyCloudToken.parseHubNameFromToken(hubKey);
                            hubNamesJson.put(hubName, hubKey);
                            hubs.add(hubName);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        setStatus("Exception: " + e.getMessage());
                    }

                    // Populate hubs to selection list
                    Spinner items = findViewById(R.id.spinner_hubs_for_troubleshooter);
                    setSpinnerItems(items, hubs);
                    setStatus("Select Hub:");
                    findViewById(R.id.spinner_hubs_for_troubleshooter).setEnabled(true);

                } else {
                    setStatus("Cozify Hubs not found: " + status);
                }
            }
        });
    }

    void setStatus(String statusMessage) {
        if (textViewStatus == null) {
            textViewStatus = findViewById(R.id.troubleshooter_hub_select_status);
        }
        if (textViewStatus != null) {
            textViewStatus.setText(statusMessage);
        }
    }


    public void onButtonSetupComplete(View view) {
        updateAllWidgets();
        Intent resultValue = new Intent();
        setResult(RESULT_OK, resultValue);
        finish();
    }

    public void onButtonOpenTroubleshooter(View view) {
        Intent i = new Intent(this, TroubleshooterRootActivity.class);
        i.putExtra("hubKey", selectedHubKey);
        i.putExtra("hubId", selectedHubId);
        startActivity(i);
    }

    void setSpinnerItems(Spinner spinner, ArrayList<String> items) {
        final Context context = CozifyAppWidgetConnectedActivity.this;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public void onButtonLogout(View view) {
        cozifyAPI.setCloudToken(null);
        Intent i = new Intent(this, CozifyWidgetSetupActivity.class);
        startActivity(i);
    }
}