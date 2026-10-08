package com.example.sensorscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class FFTPlotView extends View {

    private double[] xValues;
    private double[] yValues;
    private double[] zValues;

    private double samplingFrequency;

    private Paint paint = new Paint();

    public FFTPlotView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setStrokeWidth(3f);
        paint.setTextSize(24f);
    }

    // Gets FFT magnitudes and sampling frequency from SessionActivity
    public void setFFTData(double[] x, double[] y, double[] z, double frequency) {

        xValues = x;
        yValues = y;
        zValues = z;

        samplingFrequency = frequency;

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

        float centerX = getWidth() / 2f;
        float bottomY = getHeight() - 10;

        // X-axis label
        canvas.drawText("Frequency (Hz)", centerX - 65, bottomY, paint);

        canvas.save();
        canvas.rotate(-90);

        float centerY = -getHeight() / 2f;

        // Y-axis label
        canvas.drawText("FFT Magnitude", centerY - 65, 25, paint);

        canvas.restore();

        // X/Y/Z legend
        paint.setColor(Color.RED);
        canvas.drawText("X", right - 90, top + 20, paint);

        paint.setColor(Color.GREEN);
        canvas.drawText("Y", right - 55, top + 20, paint);

        paint.setColor(Color.BLUE);
        canvas.drawText("Z", right - 20, top + 20, paint);

        if (xValues == null) {
            return;
        }

        // Find largest magnitude so the graph scales
        double max = 0;

        for (int i = 1; i < xValues.length; i++) {
            max = Math.max(max, xValues[i]);
            max = Math.max(max, yValues[i]);
            max = Math.max(max, zValues[i]);
        }

        if (max == 0) {
            max = 1;
        }

        drawLine(canvas, xValues, Color.RED, left, right, top, bottom, max);
        drawLine(canvas, yValues, Color.GREEN, left, right, top, bottom, max);
        drawLine(canvas, zValues, Color.BLUE, left, right, top, bottom, max);

        // Show the highest plotted frequency
        double maxFrequency = 63 * samplingFrequency / 128.0;

        paint.setColor(Color.BLACK);
        paint.setTextSize(20f);

        canvas.drawText(String.format(Locale.US, "%.1f", maxFrequency), right - 25, bottom + 22, paint);
    }

    private void drawLine(Canvas canvas, double[] values, int color, float left, float right, float top, float bottom, double max) {

        paint.setColor(color);
        paint.setStrokeWidth(3f);

        double maxFrequency = 63 * samplingFrequency / 128.0;

        // Draw FFT bins 1 through 63
        for (int i = 2; i < values.length; i++) {

            double frequency1 = (i - 1) * samplingFrequency / 128.0;
            double frequency2 = i * samplingFrequency / 128.0;

            float x1 = left + (float) (frequency1 / maxFrequency) * (right - left);
            float x2 = left + (float) (frequency2 / maxFrequency) * (right - left);
            float y1 = bottom - (float) (values[i - 1] / max) * (bottom - top);
            float y2 = bottom - (float) (values[i] / max) * (bottom - top);

            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}