package de.andreas1724.bigdigitalclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements View
        .OnSystemUiVisibilityChangeListener, View.OnClickListener {

    private DigitalClock digitalClock = null;
    private boolean isSeconds = false;
    private boolean isScreensaverMode = false;
    private boolean isBleMode = false;
    private boolean hideAlarm = false;
    private Runnable bleInitialPoll;
    private FrameLayout layout;
    private FloatingActionButton fab;
    private static MyStringKeys keys;
    private static ScheduledThreadPoolExecutor timeCountExecutor;
    private static ScheduledFuture timer;
    private static final int FULLSCREEN_OPTIONS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                //| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                //| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                //| View.SYSTEM_UI_FLAG_FULLSCREEN
            ;

    private ClockReceiver receiver = null;

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


    private void registerClockReceiver() {
        if (receiver == null) {
            receiver = new ClockReceiver();
        }
        for (String action: actionsForReceiver) {
            IntentFilter filter = new IntentFilter(action);
            registerReceiver(receiver, filter);
        }
    }

    private void unregisterClockReceiver() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        receiver = null;
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
        boolean isNightModus = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                keys.NIGHT_MODE, false);
        int foregroundColor, backgroundColor;
        if (isNightModus) {
            foregroundColor = PreferenceManager.getDefaultSharedPreferences(this).getInt
                    (keys.FOREGROUND_COLOR_NIGHT, 0);
            backgroundColor = PreferenceManager.getDefaultSharedPreferences(this).getInt
                    (keys.BACKGROUND_COLOR_NIGHT, 0);
        } else {
            foregroundColor = PreferenceManager.getDefaultSharedPreferences(this).getInt
                    (keys.FOREGROUND_COLOR_NORMAL, 0);
            backgroundColor = PreferenceManager.getDefaultSharedPreferences(this).getInt
                    (keys.BACKGROUND_COLOR_NORMAL, 0);
        }

        findViewById(R.id.layout).setBackgroundColor(backgroundColor);
        digitalClock.setColors(foregroundColor);
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
        boolean keepScreenOn = PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean(keys.KEEP_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        isSeconds = PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean(keys.SECONDS, false);

        digitalClock.showSeconds(isSeconds);

        isScreensaverMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(keys.MOVE_CLOCK, false);
        digitalClock.setScreensaverMode(isScreensaverMode);

        hideAlarm = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(keys.HIDE_ALARM, false);

        digitalClock.init();
        timeCountExecutor = new ScheduledThreadPoolExecutor(1);

        if (!isBleMode) {
            showActualTime();
        } else {
            digitalClock.setTime(0, false);
            digitalClock.postInvalidate();

            bleInitialPoll = new Runnable() {
                @Override
                public void run() {
                    if (TimeBeaconReceiverService.isRecentUpdateAvailable()) {
                        showActualTime();
                    } else {
                        TimeBeaconReceiverService.maybeResync(MainActivity.this);
                        handler.postDelayed(bleInitialPoll, 250);
                    }
                }
            };
            TimeBeaconReceiverService.forceInitialScan(this);
            handler.postDelayed(bleInitialPoll, 250);
        }

        showNextAlarm();
        getWindow().getDecorView().setSystemUiVisibility(FULLSCREEN_OPTIONS);
    }

    private Runnable showTimeNextMinute = new Runnable() {
        @Override
        public void run() {
            showActualTime();
        }
    };

    private volatile int sec;

    private void addSeconds(int numberOfSeconds) {
        sec += numberOfSeconds;
        if (sec > 59) {
            sec = 60 % numberOfSeconds;
        }
    }

    private Runnable showNextSecond = new Runnable() {
        @Override
        public void run() {
            addSeconds(1);
            digitalClock.setSeconds(sec);
            if (isScreensaverMode && sec % 4 == 0) {
                digitalClock.moveOneStep();
            }
            if (sec == 0) {
                showActualTime();
                return;
            }
            digitalClock.postInvalidate();
        }
    };

    private Runnable showTimeNext4Seconds = new Runnable() {
        @Override
        public void run() {
            digitalClock.moveOneStep();
            addSeconds(4);
            if (sec == 0) {
                showActualTime();
                return;
            }
            digitalClock.postInvalidate();
        }
    };

    private void showActualTime() {
        final long milliseconds;
        if (isBleMode) {
            milliseconds = TimeBeaconReceiverService.currentTimeMillis();
            TimeBeaconReceiverService.maybeResync(this);
            if (TimeBeaconReceiverService.isRecentUpdateAvailable()) {
                setColors();
            } else {
                // Turn the clock red if we can't receive beacons anymore
                digitalClock.setColors(0xffff0000);
            }
        } else {
            milliseconds = System.currentTimeMillis();
        }

        int msec = (int) (milliseconds % 60000);
        sec = msec / 1000;
        if (timer != null) {
            timer.cancel(false);
        }
        if (isSeconds) {
            int nextSec = 1000 - msec % 1000;
            timer = timeCountExecutor.scheduleAtFixedRate(showNextSecond, nextSec, 1000,
                    TimeUnit.MILLISECONDS);
        } else if (isScreensaverMode) {
            int next4Sec = 4000 - msec % 4000;
            timer = timeCountExecutor.scheduleAtFixedRate(showTimeNext4Seconds, next4Sec, 4000,
                    TimeUnit.MILLISECONDS);
        } else {
            int nextMin = 60000 - msec;
            timer = timeCountExecutor.scheduleAtFixedRate(showTimeNextMinute, nextMin, 60000,
                    TimeUnit.MILLISECONDS);
        }
        digitalClock.setTime(milliseconds, isBleMode);
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
                    Manifest.permission.ACCESS_FINE_LOCATION };
            requestPermissions(permissionList, 0);
        }
    }

    @Override
    protected void onDestroy() {
        layout.removeAllViews();
        unregisterClockReceiver();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        fab.hide();
        unregisterClockReceiver();
        handler.removeCallbacks(autoHide);
        handler.removeCallbacks(bleInitialPoll);
        timeCountExecutor.shutdown();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setClockOrientation();
        setColors();
        registerClockReceiver();
        init();
    }

}

