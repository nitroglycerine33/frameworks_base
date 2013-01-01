/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import java.util.ArrayList;
import java.util.HashMap;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.systemui.quicksettings.AirplaneModeTile;
import com.android.systemui.quicksettings.AlarmTile;
import com.android.systemui.quicksettings.AutoRotateTile;
import com.android.systemui.quicksettings.BatteryTile;
import com.android.systemui.quicksettings.BluetoothTile;
import com.android.systemui.quicksettings.BrightnessTile;
import com.android.systemui.quicksettings.BugReportTile;
import com.android.systemui.quicksettings.NfcTile;
import com.android.systemui.quicksettings.ScreenTimeoutTile;
import com.android.systemui.quicksettings.TorchTile;
import com.android.systemui.quicksettings.GPSTile;
import com.android.systemui.quicksettings.InputMethodTile;
import com.android.systemui.quicksettings.MobileNetworkTile;
import com.android.systemui.quicksettings.MobileDataTile;
import com.android.systemui.quicksettings.MobileNetworkTypeTile;
import com.android.systemui.quicksettings.PreferencesTile;
import com.android.systemui.quicksettings.ProfileTile;
import com.android.systemui.quicksettings.QuickSettingsTile;
import com.android.systemui.quicksettings.RingerModeTile;
import com.android.systemui.quicksettings.SleepScreenTile;
import com.android.systemui.quicksettings.SyncTile;
import com.android.systemui.quicksettings.ToggleLockscreenTile;
import com.android.systemui.quicksettings.UserTile;
import com.android.systemui.quicksettings.WiFiDisplayTile;
import com.android.systemui.quicksettings.WiFiTile;
import com.android.systemui.quicksettings.WifiAPTile;
import com.android.systemui.quicksettings.RebootTile;
import com.android.systemui.quicksettings.FavoriteContactTile;
import com.android.systemui.quicksettings.FChargeTile;
import com.android.systemui.statusbar.powerwidget.PowerButton;

public class QuickSettingsController {
    private static String TAG = "QuickSettingsController";

    // Stores the broadcast receivers and content observers
    // quick tiles register for.
    public HashMap<String, ArrayList<QuickSettingsTile>> mReceiverMap
        = new HashMap<String, ArrayList<QuickSettingsTile>>();
    public HashMap<Uri, ArrayList<QuickSettingsTile>> mObserverMap
        = new HashMap<Uri, ArrayList<QuickSettingsTile>>();

    /**
     * START OF DATA MATCHING BLOCK
     *
     * THE FOLLOWING DATA MUST BE KEPT UP-TO-DATE WITH THE DATA IN
     * com.android.settings.cyanogenmod.QuickSettingsUtil IN THE
     * Settings PACKAGE.
     */
    public static final String TILE_USER = "toggleUser";
    public static final String TILE_BATTERY = "toggleBattery";
    public static final String TILE_SETTINGS = "toggleSettings";
    public static final String TILE_WIFI = "toggleWifi";
    public static final String TILE_GPS = "toggleGPS";
    public static final String TILE_BLUETOOTH = "toggleBluetooth";
    public static final String TILE_BRIGHTNESS = "toggleBrightness";
    public static final String TILE_RINGER = "toggleSound";
    public static final String TILE_SYNC = "toggleSync";
    public static final String TILE_WIFIAP = "toggleWifiAp";
    public static final String TILE_SCREENTIMEOUT = "toggleScreenTimeout";
    public static final String TILE_MOBILEDATA = "toggleMobileData";
    public static final String TILE_MOBILENETWORK = "toggleMobileNetwork";
    public static final String TILE_LOCKSCREEN = "toggleLockScreen";
    public static final String TILE_NETWORKMODE = "toggleNetworkMode";
    public static final String TILE_AUTOROTATE = "toggleAutoRotate";
    public static final String TILE_AIRPLANE = "toggleAirplane";
    public static final String TILE_TORCH = "toggleFlashlight";  // Keep old string for compatibility
    public static final String TILE_FAVCONTACT = "toggleFavoriteContact";
    public static final String TILE_SLEEP = "toggleSleepMode";
    public static final String TILE_LTE = "toggleLte";
    public static final String TILE_WIMAX = "toggleWimax";
    public static final String TILE_PROFILE = "toggleProfile";
    public static final String TILE_REBOOT = "toggleReboot";
    public static final String TILE_NFC = "toggleNfc";
    public static final String TILE_FCHARGE = "toggleFCharge";

    private static final String TILE_DELIMITER = "|";
    private static final String TILES_DEFAULT = TILE_USER
            + TILE_DELIMITER + TILE_BRIGHTNESS
            + TILE_DELIMITER + TILE_SETTINGS
            + TILE_DELIMITER + TILE_WIFI
            + TILE_DELIMITER + TILE_MOBILENETWORK
            + TILE_DELIMITER + TILE_BATTERY
            + TILE_DELIMITER + TILE_AIRPLANE
            + TILE_DELIMITER + TILE_BLUETOOTH;
    /**
     * END OF DATA MATCHING BLOCK
     */

    private final Context mContext;
    public PanelBar mBar;
    private final ViewGroup mContainerView;
    private final Handler mHandler;
    private BroadcastReceiver mReceiver;
    private ContentObserver mObserver;
    private final ArrayList<Integer> mQuickSettings;
    public PhoneStatusBar mStatusBarService;

    // Constants for use in switch statement
    public static final int WIFI_TILE = 0;
    public static final int MOBILE_NETWORK_TILE = 1;
    public static final int AIRPLANE_MODE_TILE = 2;
    public static final int BLUETOOTH_TILE = 3;
    public static final int RINGER_TILE = 4;
    public static final int SLEEP_TILE = 5;
    public static final int TOGGLE_LOCKSCREEN_TILE = 6;
    public static final int GPS_TILE = 7;
    public static final int AUTO_ROTATION_TILE = 8;
    public static final int BRIGHTNESS_TILE = 9;
    public static final int MOBILE_NETWORK_TYPE_TILE = 10;
    public static final int SETTINGS_TILE = 11;
    public static final int BATTERY_TILE = 12;
    public static final int IME_TILE = 13;
    public static final int ALARM_TILE = 14;
    public static final int BUG_REPORT_TILE = 15;
    public static final int WIFI_DISPLAY_TILE = 16;
    public static final int TORCH_TILE = 17;
    public static final int WIFIAP_TILE = 18;
    public static final int PROFILE_TILE = 19;
    public static final int MOBILE_DATA_TILE = 20;
    public static final int REBOOT_TILE = 21;
    public static final int SYNC_TILE = 22;
    public static final int NFC_TILE = 23;
    public static final int SCREENTIMEOUT_TILE = 24;
    public static final int FAV_CONTACT_TILE = 25;
    public static final int FCHARGE_TILE = 26;
    public static final int USER_TILE = 99;

    private InputMethodTile IMETile;

    public QuickSettingsController(Context context, QuickSettingsContainerView container, PhoneStatusBar statusBarService) {
        mContext = context;
        mContainerView = container;
        mHandler = new Handler();
        mStatusBarService = statusBarService;
        mQuickSettings = new ArrayList<Integer>();
    }

    void loadTiles() {
        // Read the stored list of tiles
        ContentResolver resolver = mContext.getContentResolver();
        String tiles = Settings.System.getString(resolver, Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            Log.i(TAG, "Default tiles being loaded");
            tiles = TILES_DEFAULT;
        }

        Log.i(TAG, "Tiles list: " + tiles);

        // Clear the list
        mQuickSettings.clear();

        // Split out the tile names and add to the list
        for (String tile : tiles.split("\\|")) {
            if (tile.equals(TILE_USER)) {
                mQuickSettings.add(USER_TILE);
            } else if (tile.equals(TILE_BATTERY)) {
                mQuickSettings.add(BATTERY_TILE);
            } else if (tile.equals(TILE_SETTINGS)) {
                mQuickSettings.add(SETTINGS_TILE);
            } else if (tile.equals(TILE_WIFI)) {
                mQuickSettings.add(WIFI_TILE);
            } else if (tile.equals(TILE_GPS)) {
                mQuickSettings.add(GPS_TILE);
            } else if (tile.equals(TILE_BLUETOOTH)) {
                if(deviceSupportsBluetooth()) {
                    mQuickSettings.add(BLUETOOTH_TILE);
                }
            } else if (tile.equals(TILE_BRIGHTNESS)) {
                mQuickSettings.add(BRIGHTNESS_TILE);
            } else if (tile.equals(TILE_RINGER)) {
                mQuickSettings.add(RINGER_TILE);
            } else if (tile.equals(TILE_SYNC)) {
                mQuickSettings.add(SYNC_TILE);
            } else if (tile.equals(TILE_WIFIAP)) {
                if(deviceSupportsTelephony()) {
                    mQuickSettings.add(WIFIAP_TILE);
                }
            } else if (tile.equals(TILE_SCREENTIMEOUT)) {
                mQuickSettings.add(SCREENTIMEOUT_TILE);
            } else if (tile.equals(TILE_MOBILENETWORK)) {
                if(deviceSupportsTelephony()) {
                    mQuickSettings.add(MOBILE_NETWORK_TILE);
                }
            } else if (tile.equals(TILE_LOCKSCREEN)) {
                mQuickSettings.add(TOGGLE_LOCKSCREEN_TILE);
            } else if (tile.equals(TILE_NETWORKMODE)) {
                if(deviceSupportsTelephony()) {
                    mQuickSettings.add(MOBILE_NETWORK_TYPE_TILE);
                }
            } else if (tile.equals(TILE_AUTOROTATE)) {
                mQuickSettings.add(AUTO_ROTATION_TILE);
            } else if (tile.equals(TILE_AIRPLANE)) {
                mQuickSettings.add(AIRPLANE_MODE_TILE);
            } else if (tile.equals(TILE_TORCH)) {
                mQuickSettings.add(TORCH_TILE);
            } else if (tile.equals(TILE_SLEEP)) {
                mQuickSettings.add(SLEEP_TILE);
            } else if (tile.equals(TILE_PROFILE)) {
                if (systemProfilesEnabled(resolver)) {
                    mQuickSettings.add(PROFILE_TILE);
                }
            } else if (tile.equals(TILE_NFC)) {
                // User cannot add the NFC tile if the device does not support it
                // No need to check again here
                mQuickSettings.add(NFC_TILE);
            } else if (tile.equals(TILE_WIMAX)) {
                // Not available yet
            } else if (tile.equals(TILE_LTE)) {
                // Not available yet
            } else if (tile.equals(TILE_MOBILEDATA)) {
                if(deviceSupportsTelephony()) {
                    mQuickSettings.add(MOBILE_DATA_TILE);
                }
            } else if (tile.equals(TILE_REBOOT)) {
                mQuickSettings.add(REBOOT_TILE);
            } else if (tile.equals(TILE_FAVCONTACT)) {
                mQuickSettings.add(FAV_CONTACT_TILE);
            } else if (tile.equals(TILE_FCHARGE)) {
                mQuickSettings.add(FCHARGE_TILE);
            }
        }

        // Load the dynamic tiles
        // These toggles must be the last ones added to the view, as they will show
        // only when they are needed
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1) {
            mQuickSettings.add(ALARM_TILE);
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT, 1) == 1) {
            mQuickSettings.add(BUG_REPORT_TILE);
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_WIFI, 1) == 1) {
            mQuickSettings.add(WIFI_DISPLAY_TILE);
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1) {
            mQuickSettings.add(IME_TILE);
        }
    }

    void setupQuickSettings() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        // Clear out old receiver
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        mReceiver = new QSBroadcastReceiver();
        mReceiverMap.clear();
        ContentResolver resolver = mContext.getContentResolver();
        // Clear out old observer
        if (mObserver != null) {
            resolver.unregisterContentObserver(mObserver);
        }
        mObserver = new QuickSettingsObserver(mHandler);
        mObserverMap.clear();
        addQuickSettings(inflater);
        setupBroadcastReceiver();
        setupContentObserver();
    }

    void setupContentObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        for (Uri uri : mObserverMap.keySet()) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
    }

    private class QuickSettingsObserver extends ContentObserver {
        public QuickSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = mContext.getContentResolver();
            for (QuickSettingsTile tile : mObserverMap.get(uri)) {
                tile.onChangeUri(resolver, uri);
            }
        }
    }

    void setupBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        for (String action : mReceiverMap.keySet()) {
            filter.addAction(action);
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    private void registerInMap(Object item, QuickSettingsTile tile, HashMap map) {
        if (map.keySet().contains(item)) {
            ArrayList list = (ArrayList) map.get(item);
            if (!list.contains(tile)) {
                list.add(tile);
            }
        } else {
            ArrayList<QuickSettingsTile> list = new ArrayList<QuickSettingsTile>();
            list.add(tile);
            map.put(item, list);
        }
    }

    public void registerAction(Object action, QuickSettingsTile tile) {
        registerInMap(action, tile, mReceiverMap);
    }

    public void registerObservedContent(Uri uri, QuickSettingsTile tile) {
        registerInMap(uri, tile, mObserverMap);
    }

    private class QSBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                for (QuickSettingsTile t : mReceiverMap.get(action)) {
                    t.onReceive(context, intent);
                }
            }
        }
    };

    boolean deviceSupportsTelephony() {
        PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    boolean deviceSupportsBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    boolean systemProfilesEnabled(ContentResolver resolver) {
        return (Settings.System.getInt(resolver, Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1);
    }

    void setBar(PanelBar bar) {
        mBar = bar;
    }

    void addQuickSettings(LayoutInflater inflater){
        // Load the user configured tiles
        loadTiles();
        FavoriteContactTile.instanceCount = 0;
        // Now add the actual tiles from the loaded list
        for (Integer entry: mQuickSettings) {
            QuickSettingsTile qs = null;
            switch (entry) {
            case WIFI_TILE:
                qs = WiFiTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case MOBILE_NETWORK_TILE:
                qs = MobileNetworkTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case AIRPLANE_MODE_TILE:
                qs = AirplaneModeTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case BLUETOOTH_TILE:
                qs = BluetoothTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case RINGER_TILE:
                qs = RingerModeTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case SLEEP_TILE:
                qs = SleepScreenTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case TOGGLE_LOCKSCREEN_TILE:
                qs = ToggleLockscreenTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case GPS_TILE:
                qs = GPSTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case AUTO_ROTATION_TILE:
                qs = AutoRotateTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case BRIGHTNESS_TILE:
                qs = BrightnessTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case MOBILE_NETWORK_TYPE_TILE:
                qs = MobileNetworkTypeTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case ALARM_TILE:
                qs = AlarmTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case BUG_REPORT_TILE:
                qs = BugReportTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case WIFI_DISPLAY_TILE:
                qs = WiFiDisplayTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case SETTINGS_TILE:
                qs = PreferencesTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case BATTERY_TILE:
                qs = BatteryTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case IME_TILE:
                qs = InputMethodTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                IMETile = (InputMethodTile) qs;
                break;
            case USER_TILE:
                qs = UserTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case TORCH_TILE:
                qs = TorchTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case FAV_CONTACT_TILE:
                qs = FavoriteContactTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case WIFIAP_TILE:
                qs = WifiAPTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case PROFILE_TILE:
                qs = ProfileTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case MOBILE_DATA_TILE:
                qs = MobileDataTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case REBOOT_TILE:
                qs = RebootTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case SYNC_TILE:
                qs = SyncTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case NFC_TILE:
                qs = NfcTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case SCREENTIMEOUT_TILE:
                qs = ScreenTimeoutTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            case FCHARGE_TILE:
                qs = FChargeTile.getInstance(mContext, inflater,
                        (QuickSettingsContainerView) mContainerView, this, mHandler);
                break;
            }
            if (qs != null) {
                qs.setupQuickSettingsTile();
            }
        }
    }

    public void setService(PhoneStatusBar phoneStatusBar) {
        mStatusBarService = phoneStatusBar;
    }

    public void setImeWindowStatus(boolean visible) {
        if (IMETile != null) {
            IMETile.toggleVisibility(visible);
        }
    }

    public void updateResources() {}
}