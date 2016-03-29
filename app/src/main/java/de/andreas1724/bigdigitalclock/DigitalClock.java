package de.andreas1724.bigdigitalclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.View;

import java.util.Locale;

/**
 * Created by andreas on 16.03.16.
 */
public class DigitalClock extends View {

    private static final float TEXT_SIZE_START = 1000f; // arbitrary value to start with
    private static final float AMPM_TIME_RATIO = 0.5f; // max. 0.4
    private static final float ALARM_TIME_RATIO = 1f / 5; // max. 1/5
    private static final float GAP_TIME_RATIO = 0.03f;
    private static final float SECONDS_TIME_RATIO =
            new Float((3 - Math.sqrt(5)) / 2); // golden ratio ;-)
    private static final String FONT = "segments.ttf";
    /*
    In this font:
        "{" -> icon for 'Alarm on' as character
        "}" -> icon for 'Alarm off' as character
        "[" -> 'AM' as character
        "]" -> 'PM' as character
    */
    private static final String LARGEST_ALARM = "{ 20:45[ +3d";
    private static final String LARGEST_SECONDS = "00";
    private static final String LARGEST_AMPM = "AM";
    private static final String LARGEST_24TIME = "20:00";
    private static final String LARGEST_12TIME = "12:00";

    class SomeText {
        float x, y;
        String txt;
        public SomeText() {
            x = 0;
            y = 0;
            txt = "";
        }
    }

    private Context context;
    private float hoursMinutesTextSize; // all other sizes depends on it
    private SomeText hoursMinutes = new SomeText();
    private SomeText amPm = new SomeText();
    private SomeText seconds = new SomeText();
    private SomeText alarm = new SomeText();
    private boolean is24HourFormat;
    private boolean isSeconds = false;
    private Paint hoursMinutesPaint;
    private Paint amPmPaint;
    private Paint secondsPaint;
    private Paint alarmPaint;
    private int canvasWidth, canvasHeight;
    private Rect boundingRect = new Rect();

    private Rect r = new Rect(); // we will need this for measuring text boundss

    public DigitalClock(Context context) {
        super(context);
        this.context = context;
        Typeface ttf = Typeface.createFromAsset(context.getAssets(), FONT);
        hoursMinutesPaint = new Paint();
        hoursMinutesPaint.setTypeface(ttf);
        hoursMinutesPaint.setAntiAlias(true);
        hoursMinutesPaint.setTextAlign(Paint.Align.LEFT);
        amPmPaint = new Paint(hoursMinutesPaint);
        secondsPaint = new Paint(hoursMinutesPaint);
        alarmPaint = new Paint(hoursMinutesPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasWidth = w;
        canvasHeight = h;
        boundingRect.set(0, 0, w, h);
        init();
    }

    public void showSeconds(boolean isSeconds) {
        this.isSeconds = isSeconds;
    }

    public void init() {
        is24HourFormat = DateFormat.is24HourFormat(context);
        hoursMinutesTextSize = getWidestPossibleTextSize();
        setAllTextSizes(hoursMinutesTextSize);
        recalculateIfTextTooHigh();
        calculateBoundingRect();
        setAllPositions();
    }

    private void setAllPositions() {
        String widestTime = is24HourFormat? LARGEST_24TIME: LARGEST_12TIME;
        hoursMinutesPaint.getTextBounds(widestTime, 0, widestTime.length(), r);
        hoursMinutes.x = boundingRect.left - r.left;
        hoursMinutes.y = boundingRect.top - r.top;
        secondsPaint.getTextBounds(LARGEST_SECONDS, 0, LARGEST_SECONDS.length(), r);
        seconds.x = boundingRect.right - r.width() - r.left;
        seconds.y = hoursMinutes.y;
        alarmPaint.getTextBounds(LARGEST_ALARM, 0, LARGEST_ALARM.length(), r);
        alarm.x = boundingRect.left - r.left;
        alarm.y = boundingRect.bottom - r.bottom;
        amPmPaint.getTextBounds(LARGEST_AMPM, 0, LARGEST_AMPM.length(), r);
        amPm.x = boundingRect.right - r.width() - r.left;
        amPm.y = boundingRect.bottom - r.bottom;
    }

    private void calculateBoundingRect() {
        String widestTime = is24HourFormat? LARGEST_24TIME: LARGEST_12TIME;
        hoursMinutesPaint.getTextBounds(widestTime, 0, widestTime.length(), r);
        int width = r.width();
        if (isSeconds) {
            secondsPaint.getTextBounds(LARGEST_SECONDS, 0, LARGEST_SECONDS.length(), r);
            width += r.width();
        }
        int height = getTotalHeight();
        if (canvasWidth >= canvasHeight) {
            boundingRect.set(
                    (canvasWidth - width) / 2,
                    (canvasHeight - height) / 2,
                    (canvasWidth + width) / 2,
                    (canvasHeight + height) / 2
            );
        } else {
            boundingRect.set(
                    (canvasWidth - width) / 2,
                    (canvasHeight - 2 * height) / 4,
                    (canvasWidth + width) / 2,
                    (canvasHeight + 2 * height) / 4
            );
        }
    }

    private void recalculateIfTextTooHigh() {
        int height = getTotalHeight();
        if (height > canvasHeight) {
            hoursMinutesTextSize *= ((float) canvasHeight / (float) height);
        }
        setAllTextSizes(hoursMinutesTextSize);
    }

    private int getTotalHeight() {
        int height;
        hoursMinutesPaint.getTextBounds(LARGEST_24TIME, 0, LARGEST_24TIME.length(), r);
        height = r.height();
        height += Math.round(GAP_TIME_RATIO * hoursMinutesTextSize);
        amPmPaint.getTextBounds(LARGEST_AMPM, 0, LARGEST_AMPM.length(), r);
        int amPmHeight = r.height();
        alarmPaint.getTextBounds(LARGEST_ALARM, 0, LARGEST_ALARM.length(), r);
        int alarmHeight = r.height();
        if (!is24HourFormat && (amPmHeight > alarmHeight)) {
            height += amPmHeight;
        } else {
            height += alarmHeight;
        }
        return height;
    }

    private void setAllTextSizes(float textSize) {
        hoursMinutesPaint.setTextSize(textSize);
        alarmPaint.setTextSize(textSize * ALARM_TIME_RATIO);
        amPmPaint.setTextSize(textSize * AMPM_TIME_RATIO);
        secondsPaint.setTextSize(textSize * SECONDS_TIME_RATIO);
    }

    private float getWidestPossibleTextSize() {
        hoursMinutesPaint.setTextSize(TEXT_SIZE_START);
        secondsPaint.setTextSize(SECONDS_TIME_RATIO * TEXT_SIZE_START);
        String widestTime = is24HourFormat? LARGEST_24TIME: LARGEST_12TIME;
        hoursMinutesPaint.getTextBounds(widestTime, 0, widestTime.length(), r);
        int timeWidth = r.width();
        if (isSeconds) {
            secondsPaint.getTextBounds(LARGEST_SECONDS, 0, LARGEST_SECONDS.length(), r);
            timeWidth += r.width();
        }
        return (TEXT_SIZE_START * canvasWidth) / timeWidth;
    }

    public void setColors(int color) {
        hoursMinutesPaint.setColor(color);
        amPmPaint.setColor(color);
        secondsPaint.setColor(color);
        alarmPaint.setColor(color);
        invalidate();
    }

    public void setTime(long milliseconds) {
        String actualTime = TimeTool.getShortTime(milliseconds, is24HourFormat);
        hoursMinutes.txt = actualTime.substring(0, 5);
        if (!is24HourFormat) {
            amPm.txt = actualTime.substring(5);
        } else {
            amPm.txt = "";
        }
        if (isSeconds) {
            int sec = (int) ((milliseconds / 1000) % 60);
            seconds.txt = String.format(Locale.US, "%02d", sec);
        }
        invalidate();
    }

    public void setSeconds(int sec) {
        seconds.txt = String.format(Locale.US, "%02d", sec);
        if (sec == 0) {
            setTime(System.currentTimeMillis());
        } else {
            invalidate();
        }
    }

    public void setAlarm(long milliseconds) {
        if (milliseconds < 0) {
            alarm.txt = "}";
            return;
        }
        String alarmTime = TimeTool.getShortTime(milliseconds, is24HourFormat);
        alarm.txt = "{ " + alarmTime.substring(0, 5).trim();
        if (!is24HourFormat) {
            if (alarmTime.substring(5).equals("AM")) {
                alarm.txt += "[";
            } else {
                alarm.txt += "]";
            }
        }
        long diff = milliseconds - System.currentTimeMillis();
        diff /= 1000 * 60 * 60 * 24;
        if (diff > 0) {
            alarm.txt += " +" + diff + "d";
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isSeconds) {
            canvas.drawText(seconds.txt, seconds.x, seconds.y, secondsPaint);
        }
        canvas.drawText(hoursMinutes.txt, hoursMinutes.x, hoursMinutes.y, hoursMinutesPaint);
        canvas.drawText(alarm.txt, alarm.x, alarm.y, alarmPaint);
        if (!is24HourFormat) {
            canvas.drawText(amPm.txt, amPm.x, amPm.y, amPmPaint);
        }
    }

}
