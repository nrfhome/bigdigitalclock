package de.andreas1724.bigdigitalclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by andreas on 06.02.16.
 */
public class SimpleTextView extends View {
    private float x;
    private float y;
    private float relativeX;
    private float relativeY;
    private Paint paint;
    private FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(0, 0);

    private String text;

    public SimpleTextView(Context context, Paint paint, String text) {
        super(context);
        setPosition(0, 0);
        this.paint = paint;
        setText(text);
        wrapContent();
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    public void wrapContent() {
        Rect r = new Rect();
        paint.getTextBounds(this.text, 0, this.text.length(), r);
        // add 1 pixel to each side of the canvas to prevent remaining pixels when changing text
        params.width = r.width() + 2;
        params.height = r.height() + 2;
        relativeX = -r.left + 1;
        relativeY = -r.top + 1;
        params.leftMargin = (int) x - 1;
        params.topMargin =  (int) y - 1;
        setLayoutParams(params);
        invalidate();
    }

    public int getBoundsHeight() {
        Rect r = new Rect();
        paint.getTextBounds(text, 0, text.length(), r);
        return r.height();
    }

    public int getBoundsWidth() {
        Rect r = new Rect();
        paint.getTextBounds(text, 0, text.length(), r);
        return r.width();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        params.leftMargin = x - 1;
        params.topMargin = y - 1;
        setLayoutParams(params);
        invalidate();
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(text, relativeX, relativeY, paint);
    }
}

