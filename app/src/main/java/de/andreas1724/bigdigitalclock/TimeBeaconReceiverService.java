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

    private BluetoothLeScanner mBleScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;
    private Runnable mCancelCallback;

    private static final ParcelUuid SERVICE_UUID =
            new ParcelUuid(UUID.fromString("e043ea26-928a-2ffb-6117-f98bb98e036c"));

    private static long lastRxRealtime = 0;
    private static long lastRxTimestamp;
    private static long lastResyncAttempt = 0;
    private static Object mLock;

    private static final String TAG = "BigDigitalClock";

    public static void forceInitialScan(Context context) {
        Intent intent = new Intent(context, TimeBeaconReceiverService.class);
        context.startService(intent);
    }

    public static void maybeResync(Context context) {
        if (timeSinceLastUpdate() >= (60 * 60 * 1000) && timeSinceLastResyncAttempt() > (60 * 1000)) {
            Intent intent = new Intent(context, TimeBeaconReceiverService.class);
            context.startService(intent);
        }
    }

    public static boolean isRecentUpdateAvailable() {
        synchronized (mLock) {
            return timeSinceLastUpdate() <= (3 * 60 * 60 * 1000);
        }
    }

    private static long timeSinceLastResyncAttempt() {
        synchronized (mLock) {
            return SystemClock.elapsedRealtime() - lastResyncAttempt;
        }
    }

    private static long timeSinceLastUpdate() {
        synchronized (mLock) {
            return SystemClock.elapsedRealtime() - lastRxRealtime;
        }
    }

    public static long currentTimeMillis() {
        synchronized (mLock) {
            return lastRxTimestamp + SystemClock.elapsedRealtime() - lastRxRealtime;
        }
    }

    private void onScanResult(int callbackType, ScanResult result) {
        ScanRecord rec = result.getScanRecord();
        byte[] tsBytes = rec.getServiceData().get(SERVICE_UUID);
        long ts = 0;
        for (int i = 7; i >= 0; i--) {
            ts <<= 8;
            ts |= ((int)tsBytes[i] & 0xff);
        }
        Log.d(TAG, "onScanResult " + ts + " <- " + result);

        synchronized (mLock) {
            lastRxTimestamp = ts;
            lastRxRealtime = result.getTimestampNanos() / 1000000;
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
        byte[] zeroes = { 0 };
        ScanFilter scanFilter0 = new ScanFilter.Builder()
                .setServiceData(SERVICE_UUID, zeroes, zeroes)
                .build();
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(scanFilter0);

        // Optimize for low latency to minimize skew
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        Log.d(TAG, "startScan");
        mBleScanner.startScan(scanFilters, scanSettings, mScanCallback);
        mHandler.postDelayed(mCancelCallback, 10000);
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        Log.d(TAG, "stopScan()");
        mBleScanner.stopScan(mScanCallback);
        mHandler.removeCallbacks(mCancelCallback);
        this.stopSelf();
    }

    @Override
    public void onCreate() {
        mBleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
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
        mBleScanner.stopScan(mScanCallback);
        mHandler.removeCallbacks(mCancelCallback);
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
