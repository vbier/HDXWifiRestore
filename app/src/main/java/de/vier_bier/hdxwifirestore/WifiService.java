package de.vier_bier.hdxwifirestore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WifiService extends Service {
    private static final String TAG = "HDXWR-WifiService";
    private static final int NOTIFICATION_ID = 2;
    private static final String CHANNEL_ID = "de.vier_bier.hdxwifirestore.wifiservice";

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    private NetworkTracker mTracker;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTracker = new NetworkTracker(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        mTracker.unregister();
        mTracker = null;

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            if (ACTION_START_FOREGROUND_SERVICE.equals(action)) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

                String ssid = prefs.getString("SSID", "");
                String pass = prefs.getString("Password", "");

                Log.v(TAG, "starting to watch " + (pass != null ? "password-protected " : "")
                        + "WIFI connection to " + ssid + "...");

                if (mTracker != null) {
                    mTracker.setConnection(ssid, pass);
                } else {
                    Log.v(TAG, "no network tracker available. IGNORED!");
                }

                startForegroundService();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void startForegroundService() {
        NotificationManager nMN = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "HDX Wifi Restore", NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            nMN.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_restore_black_24dp);
        builder.setContentTitle("HDX Wifi Restore");
        builder.setContentText("Click to open configuration");
        builder.setSound(null);
        builder.setOngoing(true);

        Intent action1Intent = new Intent(getApplicationContext(), MainActivity.class);
        action1Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent action1PendingIntent = PendingIntent.getActivity(this, 0, action1Intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(action1PendingIntent);

        startForeground(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "foreground service started.");
    }
}
