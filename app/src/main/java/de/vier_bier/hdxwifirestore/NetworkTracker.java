package de.vier_bier.hdxwifirestore;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;

class NetworkTracker {
    private static final String TAG = "HDXWR-NetworkTracker";
    private final Handler requestHandler = new Handler(Looper.getMainLooper());

    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback cb;
    private WifiManager wm;
    private State state;
    private int netId = -1;
    private int count = 0;

    private String ssid;
    private String pass;

    NetworkTracker(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        cb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (network != null) {
                    Log.d(TAG, "network available: " + network);
                }

                setState(State.CONNECTED);
            }

            @Override
            public void onLost(Network network) {
                if (network != null) {
                    Log.d(TAG, "lost network: " + network);
                }

                if (wm.isWifiEnabled()) {
                    Log.d(TAG, "wifi is still available, removing...");
                    setState(State.REMOVE_NETWORK);
                } else {
                    setState(State.ENABLE_WIFI);
                }

                process();
            }
        };
    }

    void unregister() {
        Log.d(TAG, "unregistering network receiver...");
        cm.unregisterNetworkCallback(cb);
    }

    private synchronized void setState(State newState) {
        if (newState != state) {
            state = newState;
            count = 0;
            Log.d(TAG, "new state: " + state.name());
        }
    }

    private synchronized void process() {
        Log.v(TAG, "process: status = " + state.name() + ", count=" + count);

        State oldState = state;
        switch (state) {
            case REMOVE_NETWORK: {
                if (removeNetwork()) {
                    setState(State.WAIT_NETWORK_REMOVED);
                }
                break;
            }
            case WAIT_NETWORK_REMOVED:
                if (getNetworkId() == -1) {
                    setState(State.DISABLE_WIFI);
                }
                break;
            case DISABLE_WIFI: {
                if (!wm.isWifiEnabled() || disableWifi()) {
                    setState(State.WAIT_WIFI_DISABLED);
                }
                break;
            }
            case WAIT_WIFI_DISABLED: {
                if (!wm.isWifiEnabled()) {
                    setState(State.ENABLE_WIFI);
                }
                break;
            }
            case ENABLE_WIFI: {
                if (wm.isWifiEnabled() || enableWifi()) {
                    setState(State.WAIT_WIFI_ENABLED);
                }
                break;
            }
            case WAIT_WIFI_ENABLED: {
                if (wm.isWifiEnabled()) {
                    setState(State.ADD_NETWORK);
                }
                break;
            }
            case ADD_NETWORK: {
                if (getNetworkId() != -1 || createNetwork() != -1) {
                    setState(State.WAIT_NETWORK_ADDED);
                }
                break;
            }
            case WAIT_NETWORK_ADDED: {
                if (getNetworkId() != -1) {
                    setState(State.ENABLE_NETWORK);
                }
                break;
            }
            case ENABLE_NETWORK: {
                if (enableNetwork()) {
                    setState(State.WAIT_NETWORK_ENABLED);
                }
                break;
            }
            case CONNECTED: return;
        }

        if (state != oldState) {
            process();
        } else if (count <= 15) {
            count++;
            requestHandler.postDelayed(this::process, 2000);
        } else {
            Log.w(TAG, state.name() + " failed!");
            cb.onLost(null);
        }
    }

    private boolean disableWifi() {
        boolean stat = wm.setWifiEnabled(false);
        Log.d(TAG, "disabled wifi: " + stat);
        return stat;
    }

    private boolean enableWifi() {
        boolean stat = wm.setWifiEnabled(true);
        Log.d(TAG, "enabled wifi: " + stat);
        return stat;
    }

    private boolean enableNetwork() {
        boolean stat = wm.enableNetwork(netId, true);
        Log.d(TAG, "enabled network: " + stat);
        return stat;
    }

    private int createNetwork() {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = ssid;
        conf.preSharedKey = pass;
        conf.providerFriendlyName = "created by DX Wifi Restore";

        int netId = wm.addNetwork(conf);
        Log.d(TAG, "added network: id=" + netId);

        if (netId != -1) {
            this.netId = netId;
        }

        return netId;
    }

    private boolean removeNetwork() {
        List<WifiConfiguration> configs = wm.getConfiguredNetworks();
        Log.v(TAG, (configs == null ? 0 : configs.size()) + " configured networks found");

        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (ssid.equals(config.SSID)) {
                    boolean stat = wm.removeNetwork(config.networkId);
                    Log.d(TAG, "removed network: " + stat);
                    return stat;
                }
            }
        }
        return true;
    }


    private int getNetworkId() {
        List<WifiConfiguration> configs = wm.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (ssid.equals(config.SSID)) {
                    return config.networkId;
                }
            }
        }

        return -1;
    }

    void setConnection(String ssid, String pass) {
        this.ssid = "\"" + ssid + "\"";
        this.pass = "\"" + pass + "\"";
        netId = getNetworkId();

        Log.d(TAG, "registering network receiver...");
        cm.registerNetworkCallback(
                new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                cb);
    }

    enum State {
        REMOVE_NETWORK, WAIT_NETWORK_REMOVED, DISABLE_WIFI, WAIT_WIFI_DISABLED, ENABLE_WIFI, WAIT_WIFI_ENABLED,
        ADD_NETWORK, WAIT_NETWORK_ADDED, ENABLE_NETWORK, WAIT_NETWORK_ENABLED, CONNECTED
    }
}
