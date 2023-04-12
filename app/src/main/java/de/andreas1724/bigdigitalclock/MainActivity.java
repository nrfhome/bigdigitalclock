package de.andreas1724.bigdigitalclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity implements View
        .OnSystemUiVisibilityChangeListener, View.OnClickListener {

    private DigitalClock digitalClock = null;
    private boolean isSeconds = false;
    private boolean isScreensaverMode = false;
    private long nextStepTime;
    private int foregroundColor, backgroundColor;
    private boolean isBleMode = false;
    private boolean errorColor = false;
    private boolean hideAlarm = false;
    private Runnable bleInitialPoll;
    private FrameLayout layout;
    private FloatingActionButton fab;
    private static MyStringKeys keys;
    private static final int FULLSCREEN_OPTIONS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                //| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                //| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                //| View.SYSTEM_UI_FLAG_FULLSCREEN
            ;

    private ClockReceiver clockReceiver = null;

    private boolean runningOnBattery = false;
    private BroadcastReceiver batteryReceiver;

    public static String[] actionsForReceiver;

    private final String TAG = "BigDigitalClock";

    public static final String[] ACTIONS_LOLLIPOP = {
            Intent.ACTION_TIME_CHANGED,
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED
    };

    private Handler handler = new Handler();
    private Runnable autoHide = new Runnable() {
        @Override
        public void run() {
            setFabState(false);
        }
    };

    private void setFabState(boolean show) {
        if (show) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            fab.show();
            handler.postDelayed(autoHide, 3000);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(FULLSCREEN_OPTIONS);
            fab.hide();
            handler.removeCallbacks(autoHide);
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        // if Navigation Bar appears:
        if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
            getWindow().getDecorView().setFitsSystemWindows(true);
        } else {
            getWindow().getDecorView().setFitsSystemWindows(false);
        }
    }

    @Override
    public void onClick(View view) {
        if (fab.isShown()) {
            setFabState(false);
        } else {
            setFabState(true);
        }
    }

    private class ClockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            init();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.hide();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPreferenceSite();
            }
        });

        layout = (FrameLayout) findViewById(R.id.layout);

        if (keys == null) {
            keys = MyStringKeys.getInstance(this);
        }
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        if (digitalClock == null) {
            digitalClock = new DigitalClock(this);
            layout.addView(digitalClock);
        }

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        digitalClock.setOnClickListener(this);

        actionsForReceiver = ACTIONS_LOLLIPOP;

        // To prevent "Font size too large to fit in cache". Switch off hardware acceleration:
        digitalClock.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(FULLSCREEN_OPTIONS);
        }
    }

    private void openPreferenceSite() {
        fab.hide();
        Intent intent = new Intent(this, ClockPreferenceActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            finish();
            //System.exit(0);
        }
    }


    private void registerBroadcastReceivers() {
        if (clockReceiver == null) {
            clockReceiver = new ClockReceiver();
        }
        for (String action: actionsForReceiver) {
            IntentFilter filter = new IntentFilter(action);
            registerReceiver(clockReceiver, filter);
        }

        if (batteryReceiver == null) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                    runningOnBattery = plugged == 0;
                    Log.d(TAG, "Running on battery: " + runningOnBattery);
                    updateScreenOnFlag();
                }
            };
            registerReceiver(batteryReceiver, filter);
        }
    }

    private void unregisterBroadcastReceivers() {
        if (clockReceiver != null) {
            unregisterReceiver(clockReceiver);
        }
        clockReceiver = null;

        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
        batteryReceiver = null;
    }

    private void setClockOrientation() {
        String orientationValue = PreferenceManager.getDefaultSharedPreferences(this).
                getString(keys.ROTATION, "");
        int[] orientations = {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        };
        int index = Integer.valueOf(orientationValue);
        //noinspection ResourceType
        setRequestedOrientation(orientations[index]);
    }

    private void setColors() {
        findViewById(R.id.layout).setBackgroundColor(backgroundColor);
        digitalClock.setColors(foregroundColor);
    }

    private void updateScreenOnFlag() {
        String keepScreenOn = PreferenceManager.getDefaultSharedPreferences(this).
                getString(keys.KEEP_SCREEN_ON, "ac");

        boolean flag = !runningOnBattery;
        if (keepScreenOn.equals("always")) {
            flag = true;
        } else if (keepScreenOn.equals("never")) {
            flag = false;
        }

        if (flag) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void init() {
        /*
           1) The clock area is made up of three parts: actual time + alarm time + am/pm.
              Determine the text size for the actual time, where the actual time will be maximized
              horizontally. Calculate the height of the whole clock area.
           2) If the height of the clock area exceeds the screens height, we have to scale down
              the text size of the actual time. As the other components of the clock depends on
              the text size of the actual time, they will be scaled down as well.
           3) Center the clock horizontally and vertically.
         */
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        updateScreenOnFlag();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        isSeconds = p.getBoolean(keys.SECONDS, false);

        if (p.getBoolean(keys.NIGHT_MODE, false)) {
            foregroundColor = p.getInt(keys.FOREGROUND_COLOR_NIGHT, 0);
            backgroundColor = p.getInt(keys.BACKGROUND_COLOR_NIGHT, 0);
        } else {
            foregroundColor = p.getInt(keys.FOREGROUND_COLOR_NORMAL, 0);
            backgroundColor = p.getInt(keys.BACKGROUND_COLOR_NORMAL, 0);
        }

        digitalClock.showSeconds(isSeconds);

        isScreensaverMode = p.getBoolean(keys.MOVE_CLOCK, false);
        digitalClock.setScreensaverMode(isScreensaverMode);

        hideAlarm = p.getBoolean(keys.HIDE_ALARM, false);

        digitalClock.init();

        if (!isBleMode) {
            showActualTime();
        } else {
            digitalClock.setTime(0, false);
            digitalClock.postInvalidate();

            bleInitialPoll = new Runnable() {
                @Override
                public void run() {
                    if (TimeBeaconReceiverService.isRecentUpdateAvailable()) {
                        errorColor = false;
                        showActualTime();
                    } else {
                        digitalClock.setRxCount(TimeBeaconReceiverService.getRxCountSinceSync());
                        digitalClock.postInvalidate();

                        TimeBeaconReceiverService.maybeResync(MainActivity.this);
                        handler.postDelayed(bleInitialPoll, 250);
                    }
                }
            };
            TimeBeaconReceiverService.forceInitialScan(this);
            handler.postDelayed(bleInitialPoll, 1000);
        }

        showNextAlarm();
        getWindow().getDecorView().setSystemUiVisibility(FULLSCREEN_OPTIONS);
    }

    private Runnable runShowActualTime = new Runnable() {
        @Override
        public void run() {
            showActualTime();
        }
    };

    private void showActualTime() {
        final long milliseconds;
        if (isBleMode) {
            milliseconds = TimeBeaconReceiverService.currentTimeMillis();
            TimeBeaconReceiverService.maybeResync(this);
            if (TimeBeaconReceiverService.isRecentUpdateAvailable()) {
                if (errorColor) {
                    setColors();
                    errorColor = false;
                }
            } else {
                if (!errorColor) {
                    // Turn the clock red if we can't receive beacons anymore
                    digitalClock.setColors(0xffff0000);
                    errorColor = true;
                }
            }
        } else {
            milliseconds = System.currentTimeMillis();
        }

        int msec = (int) (milliseconds % 60000), nextRefresh;
        if (isSeconds) {
            nextRefresh = 1000 - msec % 1000;
        } else if (isScreensaverMode) {
            nextRefresh = 4000 - msec % 4000;
        } else {
            nextRefresh = 60000 - msec;
        }
        handler.postDelayed(runShowActualTime, nextRefresh);

        digitalClock.setTime(milliseconds, isBleMode);
        if (isScreensaverMode && milliseconds > nextStepTime) {
            digitalClock.moveOneStep();
            nextStepTime = milliseconds + 4000;
        }
        digitalClock.postInvalidate();
    }

    private void showNextAlarm() {
        long alarmMilliseconds;
        try {
            alarmMilliseconds = TimeTool.getNextAlarmMilliseconds(this);
        } catch (Exception e) {
            alarmMilliseconds = -1;
        }
        digitalClock.setAlarm(alarmMilliseconds, hideAlarm);
    }

    protected void onStart() {
        super.onStart();

        isBleMode = PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean(keys.BLE, false);

        if (isBleMode) {
            String[] permissionList = { Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION };
            requestPermissions(permissionList, 0);
        }
    }

    @Override
    protected void onDestroy() {
        layout.removeAllViews();
        unregisterBroadcastReceivers();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        fab.hide();
        unregisterBroadcastReceivers();
        handler.removeCallbacks(autoHide);
        handler.removeCallbacks(bleInitialPoll);
        handler.removeCallbacks(runShowActualTime);

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setClockOrientation();
        registerBroadcastReceivers();
        init();
        setColors();
    }

}

