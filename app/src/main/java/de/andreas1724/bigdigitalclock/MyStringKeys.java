package de.andreas1724.bigdigitalclock;

import android.content.Context;

/**
 * Created on 27.02.16.
 */
class MyStringKeys {
    final String ROTATION;
    final String FOREGROUND_COLOR_NORMAL;
    final String BACKGROUND_COLOR_NORMAL;
    final String FOREGROUND_COLOR_NIGHT;
    final String BACKGROUND_COLOR_NIGHT;
    final String NIGHT_MODE;
    final String ALARM_CLOCK;
    final String HIDE_ALARM;
    final String SECONDS;
    final String KEEP_SCREEN_ON;
    final String MOVE_CLOCK;
    final String BLE;
    final String INFO;

    private static MyStringKeys keys = null;

    private MyStringKeys(Context context) {
        ROTATION = context.getResources().getString(R.string.key_rotation);
        FOREGROUND_COLOR_NORMAL = context.getResources().getString(
                R.string.key_foreground_color_normal);
        BACKGROUND_COLOR_NORMAL = context.getResources().getString(
                R.string.key_background_color_normal);
        FOREGROUND_COLOR_NIGHT = context.getResources().getString(
                R.string.key_foreground_color_night);
        BACKGROUND_COLOR_NIGHT = context.getResources().getString(
                R.string.key_background_color_night);
        NIGHT_MODE = context.getResources().getString(R.string.key_night_mode);
        ALARM_CLOCK = context.getResources().getString(R.string.key_alarmclock);
        HIDE_ALARM = context.getResources().getString(R.string.key_hide_alarm);
        SECONDS = context.getResources().getString(R.string.key_seconds);
        KEEP_SCREEN_ON = context.getResources().getString(R.string.key_keep_screen_on);
        MOVE_CLOCK = context.getResources().getString(R.string.key_move_clock);
        BLE = "de.felixonyu.bigclock.preference.ble";
        INFO = context.getResources().getString(R.string.key_info);
    }

    static MyStringKeys getInstance(Context context) {
        if (keys == null) {
            keys = new MyStringKeys(context);
        }
        return keys;
    }
}
