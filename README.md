# HDXWifiRestore

Small utility app for people using a Kindle Fire HDX with LineageOS. The devices tend to disconnect Wifi from time to time, 
leaving the device unconnected.

This app monitors the connection and removes and recreates the wifi configuration, triggering a reconnect. 
In order for this to work, you have to enter the Wifi SSID and password into HDXWifiRestore and remove the existing Wifi configuration in the OS settings.

I did not find a way to get the reconnect to work by using an existing Wifi connection.

**The app will store the connection details unencrypted in the preferences for subsequent starts. This should be safe as long as your device is not rooted.**

Devices from SDK version 25 are supported, which means it will run on Android 7.1+ (Nougat).

The following permissions are needed:
- android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE, android.permission.ACCESS_WIFI_STATE: for monitoring the connection
- android.permission.CHANGE_WIFI_STATE: for removing and recreating the Wifi connection