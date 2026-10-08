package com.example.sensorscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class TimePlotView extends View {

    private static final int MAX_POINTS = 500;

    // Stores time and X/Y/Z values for the graph
    private float[] times = new float[MAX_POINTS];
    private float[] xValues = new float[MAX_POINTS];
    private float[] yValues = new float[MAX_POINTS];
    private float[] zValues = new float[MAX_POINTS];

    private int sampleCount = 0;

    private long firstTimestamp = -1;
    private long lastDrawTime = 0;

    private Paint paint = new Paint();

    public TimePlotView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setStrokeWidth(3f);
        paint.setTextSize(24f);
    }

    public void addSample(long timestamp, float x, float y, float z) {

        // Save the time of the first sensor event
        if (firstTimestamp == -1) {
            firstTimestamp = timestamp;
        }

        // Convert nanoseconds to seconds
        float time = (timestamp - firstTimestamp) / 1000000000f;

        if (sampleCount == MAX_POINTS) {
            removeFirst();
        }

        // Add the newest sensor values
        times[sampleCount] = time;
        xValues[sampleCount] = x;
        yValues[sampleCount] = y;
        zValues[sampleCount] = z;

        sampleCount++;

        // Keep only the most recent 5 seconds
        while (sampleCount > 0 && time - times[0] > 5.0f) {
            removeFirst();
        }

        // Redraw about every 100 ms
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastDrawTime >= 100) {
            invalidate();
            lastDrawTime = currentTime;
        }
    }

    private void removeFirst() {

        // Shift values left to remove the oldest point
        for (int i = 1; i < sampleCount; i++) {
            times[i - 1] = times[i];
            xValues[i - 1] = xValues[i];
            yValues[i - 1] = yValues[i];
            zValues[i - 1] = zValues[i];
        }
        sampleCount--;
    }

    public void clearPlot() {
        sampleCount = 0;
        firstTimestamp = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = 75;
        float right = getWidth() - 25;
        float top = 30;
        float bottom = getHeight() - 50;

        // Draw axes
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2f);

        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);
        canvas.drawText("Time (s)", getWidth() / 2f - 40, getHeight() - 10, paint);

        canvas.save();
        canvas.rotate(-90);
        canvas.drawText("Sensor Value", -getHeight() / 2f - 60, 25, paint);
        canvas.restore();

        // Draw X/Y/Z legend
        paint.setColor(Color.RED);
        canvas.drawText("X", right - 90, top + 20, paint);

        paint.setColor(Color.GREEN);
        canvas.drawText("Y", right - 55, top + 20, paint);

        paint.setColor(Color.BLUE);
        canvas.drawText("Z", right - 20, top + 20, paint);

        if (sampleCount < 2) {
            return;
        }

        // Find smallest and largest values for scaling
        float min = xValues[0];
        float max = xValues[0];

        for (int i = 0; i < sampleCount; i++) {

            min = Math.min(min, xValues[i]);
            min = Math.min(min, yValues[i]);
            min = Math.min(min, zValues[i]);

            max = Math.max(max, xValues[i]);
            max = Math.max(max, yValues[i]);
            max = Math.max(max, zValues[i]);
        }

        if (max == min) {
            max = min + 1;
        }

        float newestTime = times[sampleCount - 1];
        float oldestTime = newestTime - 5.0f;

        // Draw the three sensor lines
        drawValues(canvas, xValues, Color.RED, left, right, top, bottom, oldestTime, min, max);
        drawValues(canvas, yValues, Color.GREEN, left, right, top, bottom, oldestTime, min, max);
        drawValues(canvas, zValues, Color.BLUE, left, right, top, bottom, oldestTime, min, max);
    }

    private void drawValues(Canvas canvas, float[] values, int color, float left, float right, float top, float bottom, float oldestTime, float min, float max) {

        paint.setColor(color);
        paint.setStrokeWidth(3f);

        // Connect each point to the next point
        for (int i = 1; i < sampleCount; i++) {

            float x1 = left + ((times[i - 1] - oldestTime) / 5.0f) * (right - left);
            float x2 = left + ((times[i] - oldestTime) / 5.0f) * (right - left);
            float y1 = bottom - ((values[i - 1] - min) / (max - min)) * (bottom - top);
            float y2 = bottom - ((values[i] - min) / (max - min)) * (bottom - top);

            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}