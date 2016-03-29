package de.andreas1724.bigdigitalclock;

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
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;


/**
 * Created by andreas on 16.03.16.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private DigitalClock digitalClock = null;
    private boolean isSeconds = false;
    private FrameLayout layout;
    private FloatingActionButton fab;
    private MyStringKeys keys;
    private Handler timeChangeHandler = new Handler();

    private Handler handler = new Handler();
    private Runnable autoHide = new Runnable() {
        @Override
        public void run() {
            if (fab.isShown()) {
                fab.hide();
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (fab.isShown()) {
            fab.hide();
            handler.removeCallbacks(autoHide);
        } else {
            fab.show();
            handler.postDelayed(autoHide, 3000);
        }
    }

    private class TimeChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                init();
            }
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
        layout.setOnClickListener(this);

        if (keys == null) {
            keys = MyStringKeys.getInstance(this);
        }
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        if (digitalClock == null) {
            digitalClock = new DigitalClock(this);
            layout.addView(digitalClock);
        }
    }

    private void openPreferenceSite() {
        fab.hide();
        Intent intent = new Intent(this, ClockPreferenceActivity.class);
        startActivity(intent);
    }

    private TimeChangedReceiver receiver = null;

    private void registerListener() {
        if (receiver == null) {
            receiver = new TimeChangedReceiver();
        }
        // IntentFilter timeTickFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        IntentFilter timeChangedFilter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
        // registerReceiver(receiver, timeTickFilter);
        registerReceiver(receiver, timeChangedFilter);
    }

    private void unregisterListener() {
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
        digitalClock.init();

        showActualTime();
        showNextAlarm();
    }

    private Runnable showTimeNextMinute = new Runnable() {
        @Override
        public void run() {
            showActualTime();
        }
    };

    private int sec;

    private void addSecond() {
        sec += 1;
        if (sec > 59) {
            sec = 0;
        }
    }

    private Runnable showNextSecond = new Runnable() {
        @Override
        public void run() {
            timeChangeHandler.postDelayed(showNextSecond, 1000);
            addSecond();
            digitalClock.setSeconds(sec);
        }
    };

    private void showActualTime() {
        long milliseconds = System.currentTimeMillis();
        int msec = (int) (milliseconds % 60000);
        sec = msec / 1000;
        if (isSeconds) {
            int nextSec = 1000 - msec % 1000;
            timeChangeHandler.postDelayed(showNextSecond, nextSec);
        } else {
            int nextMin = 60000 - msec;
            timeChangeHandler.postDelayed(showTimeNextMinute, nextMin);
        }
        digitalClock.setTime(milliseconds);
    }

    private void showNextAlarm() {
        long alarmMilliseconds = TimeTool.getNextAlarmMilliseconds(this);
        digitalClock.setAlarm(alarmMilliseconds);
    }

    @Override
    protected void onDestroy() {
        layout.removeAllViews();
        unregisterListener();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        fab.hide();
        unregisterListener();
        handler.removeCallbacks(autoHide);
        timeChangeHandler.removeCallbacks(showNextSecond);
        timeChangeHandler.removeCallbacks(showTimeNextMinute);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setClockOrientation();
        setColors();
        registerListener();
        init();
    }

}

