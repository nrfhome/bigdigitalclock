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
 * Created on 16.03.16.
 */
public class DigitalClock extends View {

    private static final float TEXT_SIZE_START = 1000f; // arbitrary value to start with
    private static final float AMPM_TIME_RATIO = 0.5f;
    private static final float ALARM_TIME_RATIO = 1f / 5;
    private static final float GAP_TIME_RATIO = 0.05f;
    private static final float SECONDS_TIME_RATIO =
            (float) ((3 - Math.sqrt(5)) / 2); // golden ratio ;-)
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
    private static final int NO_FLIP = 0;
    private static final int VERTICAL_FLIP = 1;
    private static final int HORIZONTAL_FLIP = 2;

    private class SomeText {
        float x, y;
        String txt;
        SomeText() {
            x = 0;
            y = 0;
            txt = "";
        }
    }

    private Context context;
    private float hoursMinutesTextSize; // all other sizes depends on it
    private boolean timeValid;
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
    private boolean isScreensaverMode = true;
    private final static double SIZE_SCREENSAVER_MODE = 0.90d;

    private int velocity; // bounding rect velocity in pixel distance per step
    int dx, dy;
    private int flipped = DigitalClock.NO_FLIP;

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
        init();
    }

    public void showSeconds(boolean isSeconds) {
        this.isSeconds = isSeconds;
    }

    public void setScreensaverMode(boolean screensaverMode) {
        this.isScreensaverMode = screensaverMode;
    }

    public void moveOneStep() {
        //flipped = DigitalClock.NO_FLIP;
        boundingRect.offsetTo(boundingRect.left + dx, boundingRect.top + dy);
        int count = 1;
        while (isOutOfScreen()) {
            if (count++ > 10) {
                randomizePosition();
                randomizeMotion();
                break;
            }
            if (dx > 0) {
                flipHorizontally();
            }
            if (dy > 0) {
                flipVertically();
            }
            bounceBack();
            if ((flipped & DigitalClock.HORIZONTAL_FLIP) != 0) {
                flipHorizontally();
            }
            if ((flipped & DigitalClock.VERTICAL_FLIP) != 0) {
                flipVertically();
            }
        }
        setAllPositions();
    }

    // requirement: dx <= 0 && dy <= 0 && (boundingRect.left < 0 || boundingRect.top < 0)
    private boolean intersectsXAxisFirst() {
        if (dx == 0) return true;
        if (dy == 0) return false;
        if (boundingRect.left - dx == 0) return false;
        double gradientToPoint = (float)dy / dx;
        double gradientToOrigin = (float)(boundingRect.top - dy) / (boundingRect.left - dx);
        return gradientToPoint > gradientToOrigin;
    }

    private static final double square(double x) {
        return Math.pow(x, 2);
    }

    /**
     * bounceBack() requires negative vertical and horizontal moving (to the top, left). Flip before
     * and after bounceBack() if other direction.
     */
    private void bounceBack() {
        if (intersectsXAxisFirst()) {
            double intersection = boundingRect.left - dx - (boundingRect.top - dy) * dx / dy;
            double radiusFromIntersection = velocity * boundingRect.top / dy;
            double min = Math.max(intersection - radiusFromIntersection, 0);
            double nextX = min + Math.random() * radiusFromIntersection;
            double nextY = Math.sqrt(square(radiusFromIntersection) - square(intersection - nextX));
            double dxTemp = nextX - intersection;
            double dyTemp = nextY;
            dx = (int) (dxTemp * velocity / radiusFromIntersection);
            dy = (int) (dyTemp * velocity / radiusFromIntersection);
            boundingRect.offsetTo((int) nextX, (int) nextY);
        } else {
            double intersection = boundingRect.top - dy - (boundingRect.left - dx) * dy / dx;
            double radiusFromIntersection = velocity * boundingRect.left / dx;
            double min = Math.max(intersection - radiusFromIntersection, 0);
            double nextY = min + Math.random() * radiusFromIntersection;
            double nextX = Math.sqrt(square(radiusFromIntersection) - square(intersection - nextY));
            double dyTemp = nextY - intersection;
            double dxTemp = nextX;
            dx = (int) (dxTemp * velocity / radiusFromIntersection);
            dy = (int) (dyTemp * velocity / radiusFromIntersection);
            boundingRect.offsetTo((int) nextX, (int) nextY);
        }
    }

    private void flipHorizontally() {
        boundingRect.set(canvasWidth - boundingRect.right -1,
                boundingRect.top,
                canvasWidth - boundingRect.left - 1,
                boundingRect.bottom);
        dx = -dx;
        flipped ^= DigitalClock.HORIZONTAL_FLIP;
    }

    private void flipVertically() {
        boundingRect.set(boundingRect.left,
                canvasHeight - boundingRect.bottom -1,
                boundingRect.right,
                canvasHeight - boundingRect.top -1);
        dy = -dy;
        flipped ^= DigitalClock.VERTICAL_FLIP;
    }

    private boolean isOutOfScreen() {
        return boundingRect.left < 0 || boundingRect.right >= canvasWidth || boundingRect.top < 0 ||
                boundingRect.bottom >= canvasHeight;
    }

    private void setVelocity() {
        velocity = Math.max(5, (int) (Math.sqrt(canvasWidth * canvasHeight) / 80));
    }

    private void randomizePosition() {
        int x = (int) (Math.random() * (canvasWidth - boundingRect.width()));
        int y = (int) (Math.random() * (canvasHeight - boundingRect.height()));
        boundingRect.offsetTo(x, y);
    }

    private void randomizeMotion() {
        dx = (int) (Math.random() * (velocity + 1));
        dy = (int) Math.sqrt(square(velocity) - square(dx));
        dx *= Math.random() * 2 > 1 ? -1: 1;
        dy *= Math.random() * 2 > 1 ? -1: 1;
    }

    public void init() {
        is24HourFormat = DateFormat.is24HourFormat(context);
        hoursMinutesTextSize = getWidestPossibleTextSize();
        setAllTextSizes(hoursMinutesTextSize);
        recalculateIfTextTooHigh();
        if (isScreensaverMode) {
            hoursMinutesTextSize *= SIZE_SCREENSAVER_MODE;
        }
        setAllTextSizes(hoursMinutesTextSize);
        calculateBoundingRect();
        if (isScreensaverMode) {
            setVelocity();
            randomizePosition();
            randomizeMotion();
        }
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
            hoursMinutesTextSize *= (float) canvasHeight / (float) height;
        }
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

    public void setTime(long milliseconds, boolean isUtc) {
        timeValid = milliseconds != 0;

        String actualTime = TimeTool.getShortTime(milliseconds, is24HourFormat, isUtc);
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
    }

    public void setSeconds(int sec) {
        seconds.txt = String.format(Locale.US, "%02d", sec);
    }

    public void setAlarm(long milliseconds) {
        if (milliseconds == 0) {
            alarm.txt = "}";
            return;
        } else if (milliseconds < 0) {
            alarm.txt = "{";
            return;
        }
        String alarmTime = TimeTool.getShortTime(milliseconds, is24HourFormat, false);
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
        if (!timeValid) {
            return;
        }
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
