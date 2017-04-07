package de.andreas1724.bigdigitalclock;

import android.app.AlarmManager;
import android.content.Context;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TimeTool {

//    private static String concatStringArray(String[] str, String seperator) {
//        StringBuilder result = new StringBuilder();
//        for (int i = 0; i < str.length; ++i) {
//            result.append(str[i]);
//            if (i + 1 < str.length) {
//                result.append(seperator);
//            }
//        }
//        return result.toString();
//    }

    // no alarm => return 0
    // unreadable alarm => return -1
    // alarm fetched => return alarm time in milliseconds
    static long getNextAlarmMilliseconds(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo info =
                    ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                            .getNextAlarmClock();
            if (info == null) {
                return 0;
            }
            return info.getTriggerTime();
        }
        String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System
                .NEXT_ALARM_FORMATTED);
        // Log.d("TimeTool.java", nextAlarm);
        if (nextAlarm == null || nextAlarm.length() == 0) {
            return 0;
        }
        String[] weekdays = DateFormatSymbols.getInstance().getShortWeekdays();
        // Log.d("TimeTool.java", "Short names for weekdays = " +
        //        TimeTool.concatStringArray(weekdays, ", "));

        String[] amPm = DateFormatSymbols.getInstance().getAmPmStrings();
        // Log.d("TimeTool.java", "AM / PM = " + TimeTool.concatStringArray(amPm, " / "));

        long milliseconds;
        try {
            milliseconds = getNextAlarm(nextAlarm, weekdays, amPm, context);
        } catch (Exception e) {
            Log.e("TimeTool.java", e.getMessage());
            return -1;
        }
        return milliseconds;
    }

    /**
     *
     * @param context
     * @param milliseconds
     * @return examples: "23:00PM", "11:00PM", " 9:00AM"
     */
    private static String getShortTime(Context context, long milliseconds) {
        SimpleDateFormat simpleDateFormat = DateFormat.is24HourFormat(context)?
                new SimpleDateFormat("H:mma", Locale.US):
                new SimpleDateFormat("h:mma", Locale.US);
        return String.format("%7s", simpleDateFormat.format(new Date(milliseconds)));
    }

    static String getShortTime(long milliseconds, boolean is24HourFormat) {
        SimpleDateFormat simpleDateFormat = is24HourFormat?
                new SimpleDateFormat("H:mma", Locale.US):
                new SimpleDateFormat("h:mma", Locale.US);
        return String.format("%7s", simpleDateFormat.format(new Date(milliseconds)));
    }

    static String getNextAlarmAndDays(Context context) {
        long alarmMilliseconds = getNextAlarmMilliseconds(context);
        if (alarmMilliseconds <= 0) {
            return "--:--";
        }
        long diff = alarmMilliseconds - System.currentTimeMillis();
        diff /= 1000 * 60 * 60 * 24;
        String alarmTime = getShortTime(context, alarmMilliseconds);
        if (DateFormat.is24HourFormat(context)) {
            alarmTime = alarmTime.substring(0, 5);
        }
        if (diff > 0) {
            alarmTime += " +" + diff + "d";
        }
        return alarmTime;
    }

    private static long getNextAlarm(String alarm, String[] weekdays, String[] amPm, Context
            context) {
        int alarmHours = -1, alarmMinutes = -1;
        int alarmWeekday = -1; // Calendar.SUNDAY = 1; Calendar.SATURDAY = 7;
        int alarmAmPm = -1; // Calendar.AM = 0; Calendar.PM = 1;

        for (String piece: alarm.split("\\s")) {
            // Weekdays should be tested before Am/Pm. piece could be s.th like "Samst" and should
            // NOT match "am". Should match weekday "Samstag"
            if (alarmWeekday == -1) {
                alarmWeekday = getWeekday(piece, weekdays);
                if (alarmWeekday != -1) {
                    continue;
                }
            }
            if (alarmHours == -1) {
                int[] hoursMinutes = getTime(piece);
                alarmHours = hoursMinutes[0];
                alarmMinutes = hoursMinutes[1];
                if (alarmHours != -1) {
                    continue;
                }
            }
            if (alarmAmPm == -1) {
                alarmAmPm = getAmPm(piece, amPm);
            }
        }

        if (alarmWeekday == -1) {
            throw new RuntimeException("could not fetch alarm weekday");
        }
        if (alarmHours == -1) {
            throw new RuntimeException("could not fetch alarm hour");
        }
        if (alarmAmPm == -1 && !DateFormat.is24HourFormat(context)) {
            throw new RuntimeException("could not determine whether alarm is AM or PM");
        }

        Calendar now = Calendar.getInstance();
        Calendar nextAlarm = Calendar.getInstance();
        nextAlarm.set(Calendar.DAY_OF_WEEK, alarmWeekday);
        nextAlarm.set(Calendar.MINUTE, alarmMinutes);
        nextAlarm.set(Calendar.SECOND, 0);
        nextAlarm.set(Calendar.MILLISECOND, 0);
        if (DateFormat.is24HourFormat(context)) {
            nextAlarm.set(Calendar.HOUR_OF_DAY, alarmHours);
        } else {
            nextAlarm.set(Calendar.AM_PM, alarmAmPm);
            nextAlarm.set(Calendar.HOUR, alarmHours % 12);
        }
        if (nextAlarm.before(now)) {
            nextAlarm.add(Calendar.DAY_OF_MONTH, 7);
        }

        return nextAlarm.getTimeInMillis();
    }

    private static int[] getTime(String piece) {
        int hours = -1;
        int minutes = -1;

        // Android only (\d has different meanings):
        Pattern p = Pattern.compile("(\\d{1,2})\\D(\\d{2})");

        Matcher m = p.matcher(piece);
        if (m.find()) {
            hours = Integer.parseInt(m.group(1));
            minutes = Integer.parseInt(m.group(2));
        }
        return new int[] {hours, minutes};
    }

    private static int getWeekday(String piece, String[] weekdays) {
        for (int i = 1; i < weekdays.length; i++) {
            if ((piece + ".").contains(weekdays[i])) {
                return i;
            }
        }
        return -1;
    }

    // This test should come in the end. The weekday "sams" shouldn't mean "am"
    private static int getAmPm(String piece, String[] amPm) {
        for (int i = 0; i < amPm.length; i++) {
            if (piece.toLowerCase().contains(amPm[i].toLowerCase()) ||
                    amPm[i].toLowerCase().contains(piece.toLowerCase())) {
                return i;
            } else if (piece.toLowerCase().contains("am")) {
                return 0;
            } else if (piece.toLowerCase().contains("pm")) {
                return 1;
            }
        }
        return -1;
    }
}
