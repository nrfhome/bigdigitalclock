package de.andreas1724.bigdigitalclock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class ClockPreferenceActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private MyStringKeys keys;

    private ColorPickerPreference foregroundNormal;
    private ColorPickerPreference foregroundNight;
    private ColorPickerPreference backgroundNormal;
    private ColorPickerPreference backgroundNight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        keys = MyStringKeys.getInstance(this);
        addPreferencesFromResource(R.xml.settings);
        foregroundNormal = (ColorPickerPreference) findPreference(keys.FOREGROUND_COLOR_NORMAL);
        foregroundNormal.setHexValueEnabled(true);
        foregroundNight = (ColorPickerPreference) findPreference(keys.FOREGROUND_COLOR_NIGHT);
        foregroundNight.setHexValueEnabled(true);
        backgroundNormal = (ColorPickerPreference) findPreference(keys.BACKGROUND_COLOR_NORMAL);
        backgroundNormal.setHexValueEnabled(true);
        backgroundNight = (ColorPickerPreference) findPreference(keys.BACKGROUND_COLOR_NIGHT);
        backgroundNight.setHexValueEnabled(true);

        boolean isNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean
                (keys.NIGHT_MODE, false);
        if (isNightMode) {
            getPreferenceScreen().removePreference(backgroundNormal);
            getPreferenceScreen().removePreference(foregroundNormal);
            setNightColorSummaries();
        } else {
            getPreferenceScreen().removePreference(backgroundNight);
            getPreferenceScreen().removePreference(foregroundNight);
            setNormalColorSummaries();
        }

        findPreference(keys.ALARM_CLOCK).setOnPreferenceClickListener(new Preference
                .OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    i = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                } else {
                    i = new Intent(AlarmClock.ACTION_SET_ALARM);
                }
                startActivity(i);
                return true;
            }
        });
    }



    private void setNormalColorSummaries() {
        int backgroundColorNormal = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                keys.BACKGROUND_COLOR_NORMAL, 0);
        int foregroundColorNormal = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                keys.FOREGROUND_COLOR_NORMAL, 0);
        backgroundNormal.setSummary(ColorPickerPreference.convertToRGB(backgroundColorNormal));
        foregroundNormal.setSummary(ColorPickerPreference.convertToRGB(foregroundColorNormal));
    }

    private void setNightColorSummaries() {
        int backgroundColorNight = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                keys.BACKGROUND_COLOR_NIGHT, 0);
        int foregroundColorNight = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                keys.FOREGROUND_COLOR_NIGHT, 0);
        backgroundNight.setSummary(ColorPickerPreference.convertToRGB(backgroundColorNight));
        foregroundNight.setSummary(ColorPickerPreference.convertToRGB(foregroundColorNight));
    }

    @Override
    protected void onResume() {
        findPreference(keys.ALARM_CLOCK).setSummary(AlarmTimeTool.getNextAlarmAndDays(this));
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().
                unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(keys.BACKGROUND_COLOR_NIGHT) || key.equals(keys.FOREGROUND_COLOR_NIGHT)) {
            setNightColorSummaries();
        } else if (key.equals(keys.BACKGROUND_COLOR_NORMAL) || key.equals(keys
                .FOREGROUND_COLOR_NORMAL)) {
            setNormalColorSummaries();
        } else if (key.equals(keys.NIGHT_MODE)) {
            boolean isNightMode = sharedPreferences.getBoolean(keys.NIGHT_MODE, false);
            if (isNightMode) {
                getPreferenceScreen().addPreference(foregroundNight);
                getPreferenceScreen().addPreference(backgroundNight);
                getPreferenceScreen().
                        removePreference(backgroundNormal);
                getPreferenceScreen().
                        removePreference(foregroundNormal);
                setNightColorSummaries();
            } else {
                getPreferenceScreen().addPreference(foregroundNormal);
                getPreferenceScreen().addPreference(backgroundNormal);
                getPreferenceScreen().
                        removePreference(backgroundNight);
                getPreferenceScreen().
                        removePreference(foregroundNight);
                setNormalColorSummaries();
            }
        }
    }
}