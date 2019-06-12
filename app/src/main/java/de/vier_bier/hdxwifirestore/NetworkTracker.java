package de.vier_bier.hdxwifirestore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

public class NetworkTracker extends BroadcastReceiver {
    private static final String TAG = "HDXWR-NetworkTracker";
    private ConnectivityManager cm;
    private WifiManager wm;

    private String ssid;
    private String pass;

    public NetworkTracker(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        Log.d(TAG, "registering network receiver...");
        context.registerReceiver(this, intentFilter);
    }

    public void unregister(Context context) {
        Log.d(TAG, "unregistering network receiver...");
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ssid == null) return;

        try {
            checkWifi();
        } catch (InterruptedException e) {
            Log.w(TAG, "interrupted while waiting for wifi", e);
        }
    }

    private synchronized void checkWifi() throws InterruptedException {
        if (isConnected()) {
            Log.d(TAG, "network connected");
        } else {
            Log.d(TAG, "network disconnected. waiting ten seconds for wifi to come up...");
            Thread.sleep(10000);

            if (isConnected()) {
                Log.d(TAG, "network connected after waiting period.");
                return;
            }

            if (!wm.isWifiEnabled()) {
                Log.d(TAG, "wifi enabled: " + enableWifi());
            }

            Log.d(TAG, "network removed: " + removeNetwork());
            int netId = createNetwork();
            Log.d(TAG, "network added: " + netId);

            if (netId != -1 && wm.getConnectionInfo().getNetworkId() == -1) {
                Log.d(TAG, "network enabled: " + wm.enableNetwork(netId, true));
            }
        }
    }

    private boolean isConnected() {
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnected();
    }

    private int createNetwork() {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = ssid;
        conf.preSharedKey = pass;
        conf.providerFriendlyName = "created by DX Wifi Restore";

        Log.d(TAG, "adding network...");
        return wm.addNetwork(conf);
    }

    private boolean enableWifi() {
        return wm.setWifiEnabled(true);
    }

    private boolean removeNetwork() {
        List<WifiConfiguration> configs = wm.getConfiguredNetworks();
        Log.d(TAG, (configs == null ? 0 : configs.size()) + " configured networks found");

        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (ssid.equals(config.SSID)) {
                    Log.d(TAG, "removing network...");
                    return wm.removeNetwork(config.networkId);
                }
            }
        }
        return false;
    }

    public void setConnection(String ssid, String pass) {
        this.ssid = "\"" + ssid + "\"";
        this.pass = "\"" + pass + "\"";

        try {
            checkWifi();
        } catch (InterruptedException e) {
            Log.w(TAG, "interrupted while waiting for wifi", e);
        }
    }
}
