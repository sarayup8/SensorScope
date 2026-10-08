package com.example.sensorscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MotionAnimationView extends View {

    private Paint paint = new Paint();

    private float circleX = 0;
    private float speed = 0;

    private boolean movingRight = true;

    private long lastTime = 0;

    // Converts dp values to pixels
    private float density;

    public MotionAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setColor(Color.BLACK);

        // Tells you how many pixels correspond to one dp
        density = getResources().getDisplayMetrics().density;
    }

    // Set circle speed based on LOW, MEDIUM, or HIGH
    public void setMotionLevel(String level) {

        if (level.equals("LOW")) {
            speed = 0;

        } else if (level.equals("MEDIUM")) {
            speed = 100 * density;

        } else if (level.equals("HIGH")) {
            speed = 300 * density;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerY = getHeight() / 2f;
        float radius = 20 * density;

        // Put the circle in the center the first time
        if (circleX == 0) {
            circleX = getWidth() / 2f;
        }

        // LOW keeps the circle in the center
        if (speed == 0) {
            circleX = getWidth() / 2f;
        }

        // MEDIUM and HIGH move the circle horizontally
        if (speed > 0) {

            long currentTime = System.currentTimeMillis();

            if (lastTime == 0) {
                lastTime = currentTime;
            }

            float seconds = (currentTime - lastTime) / 1000f;

            if (movingRight) {
                circleX += speed * seconds;
            } else {
                circleX -= speed * seconds;
            }

            // Bounce when the circle reaches an edge
            if (circleX + radius >= getWidth()) {
                circleX = getWidth() - radius;
                movingRight = false;
            } else if (circleX - radius <= 0) {
                circleX = radius;
                movingRight = true;
            }

            lastTime = currentTime;

            invalidate();
        }

        canvas.drawCircle(circleX, centerY, radius, paint);
    }
}