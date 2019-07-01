package de.vier_bier.hdxwifirestore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = new Intent(getBaseContext(), WifiService.class);
        stopService(i);

        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> startService());

        final Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(v -> {
            finishAndRemoveTask();
            System.exit(0);
        });

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        EditText ssidT = findViewById(R.id.editText3);
        ssidT.setText(prefs.getString("SSID", ""));

        EditText passT = findViewById(R.id.editText4);
        passT.setText(prefs.getString("Password", ""));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startService() {
        EditText ssidT = findViewById(R.id.editText3);
        if (validateSSID(ssidT.getText().toString())) {
            EditText passT = findViewById(R.id.editText4);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor1 = prefs.edit();
            editor1.putString("SSID", ssidT.getText().toString());
            editor1.putString("Password", passT.getText().toString());
            editor1.apply();

            Intent i = new Intent(getBaseContext(), WifiService.class);
            i.setAction(WifiService.ACTION_START_FOREGROUND_SERVICE);
            startService(i);

            finishAndRemoveTask();
        }
    }

    private boolean validateSSID(String ssid) {
        TextView msgV = findViewById(R.id.textView5);
        msgV.setText(" ");

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configs = wm.getConfiguredNetworks();

        if (configs != null) {
            final String ssid2 = "\"" + ssid + "\"";
            for (WifiConfiguration config : configs) {
                if (ssid2.equals(config.SSID) && !"created by DX Wifi Restore".equals(config.providerFriendlyName)) {
                    msgV.setText("Network with this SSID already exists. Please delete before pressing Start!");
                    return false;
                }
            }
        }

        return true;
    }
}
