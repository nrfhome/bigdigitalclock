package de.andreas1724.bigdigitalclock;

import android.content.Context;

/**
 * Created by andreas on 27.02.16.
 */
public class MyStringKeys {
    public final String ROTATION;
    public final String FOREGROUND_COLOR_NORMAL;
    public final String BACKGROUND_COLOR_NORMAL;
    public final String FOREGROUND_COLOR_NIGHT;
    public final String BACKGROUND_COLOR_NIGHT;
    public final String NIGHT_MODE;
    public final String ALARM_CLOCK;
    public final String SECONDS;
    public final String KEEP_SCREEN_ON;

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
        SECONDS = context.getResources().getString(R.string.key_seconds);
        KEEP_SCREEN_ON = context.getResources().getString(R.string.key_keep_screen_on);
    }

    public static MyStringKeys getInstance(Context context) {
        if (keys == null) {
            keys = new MyStringKeys(context);
        }
        return keys;
    }
}
