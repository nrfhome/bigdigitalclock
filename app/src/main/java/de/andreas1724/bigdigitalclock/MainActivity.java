package de.andreas1724.bigdigitalclock;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


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
                initTime();
            }
        }
    }

    Typeface ttf;
    private static final float TEXT_SIZE_START = 1000f; // arbitrary value to start with
    private static final float AMPM_TIME_RATIO = 1f / 6;
    private static final float ALARM_TIME_RATIO = 1f / 8;
    private static final float GAP_TIME_RATIO = 1f / 16;
    private static final float SECONDS_TIME_RATIO =
            new Float((3 - Math.sqrt(5)) / 2); // golden ratio ;-)
    /*
    In this font:
        "{" -> icon for 'Alarm on' as character
        "}" -> icon for 'Alarm off' as character
        "[" -> 'AM' as character
        "]" -> 'PM' as character
    */
    private static final String LARGEST_ALARM = "{ 20:45[ +3d";
    private static final String LARGEST_SECONDS = "00";
    private static final String LARGEST_AMPM = "AMPM";
    private static final String LARGEST_24TIME = "20:00";
    private static final String LARGEST_12TIME = "12:00";
    private float textSize;
    private SimpleTextView time;
    private SimpleTextView alarm;
    private SimpleTextView amPm;
    private SimpleTextView seconds;
    private Paint timePaint;
    private Paint alarmPaint;
    private Paint amPmPaint;
    private Paint secondsPaint;
    private int screenWidth, screenHeight;
    private FrameLayout layout;
    private boolean is24hFormat;
    private boolean showSeconds;
    private FloatingActionButton fab;
    private MyStringKeys keys;
    private Handler timeChangeHandler = new Handler();

    private Handler handler = new Handler();
    Rect r = new Rect();
    private Runnable autoHide = new Runnable() {
        @Override
        public void run() {
            if (fab.isShown()) {
                fab.hide();
            }
        }
    };

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

        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        if (ttf == null) {
            ttf = Typeface.createFromAsset(getAssets(), "digital.ttf");
        }
        layout = (FrameLayout) findViewById(R.id.layout);
        layout.setOnClickListener(this);
        if (timePaint == null) {
            timePaint = new Paint();
            timePaint.setTypeface(ttf);
            timePaint.setAntiAlias(true);
            timePaint.setTextAlign(Paint.Align.LEFT);
            alarmPaint = new Paint(timePaint);
            amPmPaint = new Paint(timePaint);
            secondsPaint = new Paint(timePaint);
        }
        if (keys == null) {
            keys = MyStringKeys.getInstance(this);
        }
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
    }

    private void openPreferenceSite() {
        fab.hide();
        Intent intent = new Intent(this, ClockPreferenceActivity.class);
        startActivity(intent);
    }

    private void initTime() {
        showActualTime();
        showNextAlarm();
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
        timePaint.setColor(foregroundColor);
        alarmPaint.setColor(foregroundColor);
        amPmPaint.setColor(foregroundColor);
        secondsPaint.setColor(foregroundColor);
    }

    private float getBiggestPossibleTextSize() {
        timePaint.setTextSize(TEXT_SIZE_START);
        secondsPaint.setTextSize(SECONDS_TIME_RATIO * TEXT_SIZE_START);
        String widestTime = is24hFormat? LARGEST_24TIME: LARGEST_12TIME;
        timePaint.getTextBounds(widestTime, 0, widestTime.length(), r);
        int timeWidth = r.width();
        if (showSeconds) {
            secondsPaint.getTextBounds(LARGEST_SECONDS, 0, LARGEST_SECONDS.length(), r);
            timeWidth += r.width();
        }
        return TEXT_SIZE_START * (float) screenWidth / timeWidth;
    }

    private void setAllTextSizes(float textSize) {
        timePaint.setTextSize(textSize);
        alarmPaint.setTextSize(textSize * ALARM_TIME_RATIO);
        amPmPaint.setTextSize(textSize * AMPM_TIME_RATIO);
        secondsPaint.setTextSize(textSize * SECONDS_TIME_RATIO);
    }

    class Dimensions {
        public float width;
        public float height;
        public Dimensions(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }

    private Dimensions getMaxClockDimensions() {
        String widestTime = is24hFormat? LARGEST_24TIME: LARGEST_12TIME;
        timePaint.getTextBounds(widestTime, 0, widestTime.length(), r);
        Dimensions clockDimensions = new Dimensions(r.width(), r.height());

        if (showSeconds) {
            secondsPaint.getTextBounds(LARGEST_SECONDS, 0, LARGEST_SECONDS.length(), r);
            clockDimensions.width += r.width();
        }
        float gap = clockDimensions.height * GAP_TIME_RATIO;
        clockDimensions.height += gap;
        amPmPaint.getTextBounds(LARGEST_AMPM, 0, LARGEST_AMPM.length(), r);
        clockDimensions.height += r.height();
        if (clockDimensions.height > screenHeight) {
            textSize *= screenHeight / clockDimensions.height;
            setAllTextSizes(textSize);
            timePaint.getTextBounds(widestTime, 0, widestTime.length(), r);
            clockDimensions.width = r.width();
            clockDimensions.height = screenHeight;
            if (showSeconds) {
                secondsPaint.getTextBounds(LARGEST_SECONDS, 0, LARGEST_SECONDS.length(), r);
                clockDimensions.width += r.width();
            }
        }
        return clockDimensions;
    }

    private void addSimpleTextViews(float clockBoundLeft,
                                    float clockBoundRight,
                                    float clockBoundTop,
                                    float clockBoundBottom) {

        String widestTime = is24hFormat? LARGEST_24TIME: LARGEST_12TIME;

        if (time == null) {
            time = new SimpleTextView(this, timePaint, widestTime);
        } else {
            time.setText(widestTime);
            time.wrapContent();
        }
        time.setPosition(Math.round(clockBoundLeft), Math.round(clockBoundTop));
        layout.addView(time);

        if (showSeconds) {
            if (seconds == null) {
                seconds = new SimpleTextView(this, secondsPaint, LARGEST_SECONDS);
            } else {
                seconds.setText(LARGEST_SECONDS);
                seconds.wrapContent();
            }
            seconds.setPosition(
                    Math.round(clockBoundRight - seconds.getBoundsWidth()),
                    Math.round(clockBoundTop) + time.getBoundsHeight() - seconds.getBoundsHeight()
            );
            layout.addView(seconds);
        }

        if (amPm == null) {
            amPm = new SimpleTextView(this, amPmPaint, "AM");
        } else {
            amPm.setText("AM");
            amPm.wrapContent();
        }
        amPm.setPosition(
                Math.round(clockBoundRight - amPm.getBoundsWidth() - alarmPaint.measureText("0")),
                Math.round(clockBoundBottom - amPm.getBoundsHeight()));
        layout.addView(amPm);

        if (alarm == null) {
            alarm = new SimpleTextView(this, alarmPaint, LARGEST_ALARM);
        } else {
            alarm.setText(LARGEST_ALARM);
            alarm.wrapContent();
        }
        alarm.setPosition(
                Math.round(clockBoundLeft + alarmPaint.measureText("0")),
                Math.round(clockBoundBottom - alarm.getBoundsHeight()));
        layout.addView(alarm);
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
        layout.removeAllViews();

        is24hFormat = DateFormat.is24HourFormat(this);
        showSeconds = PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean(keys.SECONDS, false);
        boolean keepScreenOn = PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean(keys.KEEP_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        textSize = getBiggestPossibleTextSize();
        setAllTextSizes(textSize);

        Dimensions clockDim = getMaxClockDimensions();
        float clockBoundLeft = screenWidth / 2f - clockDim.width / 2;
        float clockBoundRight = screenWidth / 2f + clockDim.width / 2;
        float clockBoundTop;
        float clockBoundBottom;

        // center vertically if landscape; in portrait center vertically upper half
        if (screenWidth > screenHeight) {
            clockBoundTop = screenHeight / 2f - clockDim.height / 2;
            clockBoundBottom = screenHeight / 2f + clockDim.height / 2;
        } else {
            clockBoundTop = screenHeight / 4f - clockDim.height / 2;
            if (clockBoundTop <= 0) {
                clockBoundTop = 0;
            }
            clockBoundBottom = clockBoundTop + clockDim.height;
        }
        addSimpleTextViews(clockBoundLeft, clockBoundRight, clockBoundTop, clockBoundBottom);
    }

    private Runnable showTimeNextMinute = new Runnable() {
        @Override
        public void run() {
            showActualTime();
        }
    };

    private int sec;

    private boolean addSecond() {
        sec += 1;
        if (sec > 59) {
            sec = 0;
            return false;
        }
        return true;
    }

    private Runnable showNextSecond = new Runnable() {
        @Override
        public void run() {
            boolean isNormal = addSecond();
            if (!isNormal){
                showActualTime();
            } else {
                timeChangeHandler.postDelayed(showNextSecond, 1000);
                seconds.setText(String.format("%02d", sec));
            }
        }
    };

    private void showActualTime() {
        long milliseconds = System.currentTimeMillis();
        int msec = (int) (milliseconds % 60000);
        sec = msec / 1000;
        if (showSeconds) {
            int nextSec = 1000 - msec % 1000;
            timeChangeHandler.postDelayed(showNextSecond, nextSec);
            seconds.setText(String.format("%02d", sec));
        } else {
            int nextMin = 60000 - msec;
            timeChangeHandler.postDelayed(showTimeNextMinute, nextMin);
        }
        String shortTime = AlarmTimeTool.getShortTime(this, milliseconds);
        time.setText(shortTime.substring(0, 5));
        if (is24hFormat) {
            amPm.setText("");
        } else {
            amPm.setText(shortTime.substring(5));
        }
    }

    private void showNextAlarm() {
        long alarmMilliseconds = AlarmTimeTool.getNextAlarmMilliseconds(this);
        if (alarmMilliseconds < 0) {
            alarm.setText("}");
            return;
        }
        long diff = alarmMilliseconds - System.currentTimeMillis();
        diff /= 1000 * 60 * 60 * 24;

        String shortTime = AlarmTimeTool.getShortTime(this, alarmMilliseconds);
        String amPm = "";
        if (!is24hFormat) {
            amPm = shortTime.substring(5).equals("AM")? "[": "]";
        }
        shortTime = shortTime.substring(0, 5).trim();
        String format = "{ %s%s";
        if (diff > 0) {
            format = format.concat(" +%dd");
        }
        alarm.setText(String.format(Locale.US, format, shortTime, amPm, diff));
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
        setClockOrientation();
        setColors();
        registerListener();
        init();
        initTime();
        super.onResume();
    }

}

