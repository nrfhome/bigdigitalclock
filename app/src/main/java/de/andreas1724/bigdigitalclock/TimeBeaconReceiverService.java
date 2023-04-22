package de.andreas1724.bigdigitalclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimeBeaconReceiverService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private ScanCallback mScanCallback;
    private Handler mHandler;
    private Runnable mCancelCallback;

    private static final ParcelUuid SERVICE_UUID =
            new ParcelUuid(UUID.fromString("e043ea26-928a-2ffb-0000-000000000000"));
    private static final ParcelUuid SERVICE_UUID_MASK =
            new ParcelUuid(UUID.fromString("ffffffff-ffff-ffff-0000-000000000000"));

    private static long lastRxRealtime = 0;
    private static long lastRxTimestamp;
    private static long lastResyncAttempt = 0;
    private static long rxCountSinceSync = 0;
    private static Object mLock;

    private static final String TAG = "BigDigitalClock";

    public static void forceInitialScan(Context context) {
        Intent intent = new Intent(context, TimeBeaconReceiverService.class);
        context.startService(intent);
    }

    public static void maybeResync(Context context) {
        long lastUpdate = timeSinceLastUpdate(), retryInterval = 120;
        if (lastUpdate < (60 * 60 * 1000)) {
            // No need to update more than once per hour
            return;
        }
        if (lastUpdate == Long.MAX_VALUE) {
            // Shorten the interval if this is the initial sync
            // We'll give the BT stack a few seconds to quiesce between sync attempts
            retryInterval = 15;
        }
        if (timeSinceLastResyncAttempt() > (retryInterval * 1000)) {
            Intent intent = new Intent(context, TimeBeaconReceiverService.class);
            context.startService(intent);
        }
    }

    public static boolean isRecentUpdateAvailable() {
        // If there hasn't been an update in 3 hours, regard the timestamp
        // as stale since the clocks may have drifted
        return timeSinceLastUpdate() <= (3 * 60 * 60 * 1000);
    }

    private static long timeSinceLastResyncAttempt() {
        synchronized (mLock) {
            if (lastResyncAttempt == 0) {
                return Long.MAX_VALUE;
            }
            return SystemClock.elapsedRealtime() - lastResyncAttempt;
        }
    }

    private static long timeSinceLastUpdate() {
        synchronized (mLock) {
            if (lastRxRealtime == 0) {
                return Long.MAX_VALUE;
            }
            return SystemClock.elapsedRealtime() - lastRxRealtime;
        }
    }

    public static long currentTimeMillis() {
        synchronized (mLock) {
            if (lastRxTimestamp == 0) {
                return 0;
            }
            return lastRxTimestamp + SystemClock.elapsedRealtime() - lastRxRealtime;
        }
    }

    public static long getRxCountSinceSync() {
        synchronized (mLock) {
            return rxCountSinceSync;
        }
    }

    private void onScanResult(int callbackType, ScanResult result) {
        Log.d(TAG, "onScanResult " + result);

        ScanRecord rec = result.getScanRecord();
        rxCountSinceSync++;
        List<ParcelUuid> uuids = rec.getServiceUuids();
        if (uuids == null) {
            return;
        }
        ParcelUuid uuid = rec.getServiceUuids().get(0);
        if (uuid == null) {
            return;
        }
        // Only needed if filtering is broken
        if (uuid.getUuid().getMostSignificantBits() != SERVICE_UUID.getUuid().getMostSignificantBits()) {
            return;
        }
        long ts = uuid.getUuid().getLeastSignificantBits();

        synchronized (mLock) {
            lastRxTimestamp = ts;
            lastRxRealtime = result.getTimestampNanos() / 1000000;
            rxCountSinceSync = 0;
        }
        stopScan();
    }

    private void startScan() {
        synchronized (mLock) {
            lastResyncAttempt = SystemClock.elapsedRealtime();
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            Log.e(TAG, "Missing BLUETOOTH permission");
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Log.e(TAG, "Missing ACCESS_FINE_LOCATION permission");
            return;
        }

        // Filter for the SERVICE_UUID, match on any payload bytes
        ScanFilter scanFilter0 = new ScanFilter.Builder()
                .setServiceUuid(SERVICE_UUID, SERVICE_UUID_MASK)
                .build();
        List<ScanFilter> scanFilters = new ArrayList<>();
        // Broken on: Nexus 5, Nexus 7 (6.0.1)
        //scanFilters.add(scanFilter0);

        // Optimize for low latency to minimize skew
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        BluetoothLeScanner bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (bleScanner != null) {
            Log.d(TAG, "startScan");
            bleScanner.startScan(scanFilters, scanSettings, mScanCallback);
        } else {
            Log.e(TAG, "startScan failed: bluetooth may be disabled");
        }
        mHandler.postDelayed(mCancelCallback, 10000);
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        BluetoothLeScanner bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (bleScanner != null) {
            Log.d(TAG, "stopScan");
            bleScanner.stopScan(mScanCallback);
        } else {
            Log.e(TAG, "stopScan failed: bluetooth may be disabled");
        }
        mHandler.removeCallbacks(mCancelCallback);
    }

    @Override
    public void onCreate() {
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                TimeBeaconReceiverService.this.onScanResult(callbackType, result);
            }
        };
        mCancelCallback = new Runnable() {
            @Override
            public void run() {
                TimeBeaconReceiverService.this.stopScan();
                TimeBeaconReceiverService.this.stopSelf();
            }
        };
        mHandler = new Handler(Looper.getMainLooper());
        mLock = new Object();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        startScan();
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        stopScan();
    }

    public class LocalBinder extends Binder {
        TimeBeaconReceiverService getService() {
            return TimeBeaconReceiverService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
