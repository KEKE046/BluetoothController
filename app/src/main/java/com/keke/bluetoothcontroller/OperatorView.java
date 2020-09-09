package com.keke.bluetoothcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class OperatorView extends View {

    float mCenterX = 0f;
    float mCenterY = 0f;
    float mRadius = 0f;

    public boolean mCursorOn = false;
    float mCursorX = 0f, mCursorY = 0f;
    float mDX = 0f, mDY = 0f;
    public float mRX = 0f, mRY = 0f;

    Paint mPaint = new Paint();
    Paint mTouchPaint = new Paint();

    protected void calcCenter() {
        mCenterX = getWidth() / 2.0f;
        mCenterY = getWidth() / 2.0f;
        mRadius = Math.min(mCenterX, mCenterY) * 0.9f;
    }

    public OperatorView(Context context, AttributeSet set) {
        super(context, set);
    }

    static float distanceSquare(float x1, float y1, float x2, float y2) {
        return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    }

    static float distance(float x1, float y1, float x2, float y2) {
        return (float)Math.sqrt(distanceSquare(x1, y1, x2, y2));
    }

    static float length(float x, float y) {
        return (float)Math.sqrt(x * x + y * y);
    }

    static float clip(float x, float mn, float mx) {
        if(x < mn) return mn;
        if(x > mx) return mx;
        return x;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calcCenter();
        float lineWidth = mRadius / 25f;
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(lineWidth);
        mPaint.setColor(0xffffff);
        mPaint.setAlpha(200);
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
        if(mCursorOn) {
            mTouchPaint.setColor(0xffffff);
            mTouchPaint.setAlpha(200);
            float px = mDX, py = mDY;
            float len = length(mDX, mDY);
            if(len >= mRadius * 0.9f) {
                px = px / len * mRadius * 0.9f;
                py = py / len * mRadius * 0.9f;
            }
            canvas.drawCircle(mCenterX + px, mCenterY + py, lineWidth * 5, mTouchPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        calcCenter();
        mCursorX = event.getX();
        mCursorY = event.getY();
        switch(event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float x = event.getX(), y = event.getY();
                if(distance(x, y, mCenterX, mCenterY) <= mRadius) {
                    mCursorOn = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mCursorOn = false;
                invalidate();
                break;
        }
        if(mCursorOn) {
            mDX = mCursorX - mCenterX;
            mDY = mCursorY - mCenterY;
            invalidate();
        }
        else{
            mDX = 0;
            mDY = 0;
        }
        mRX = clip(mDX / mRadius * 1.1f, -1f, 1f);
        mRY = clip(- mDY / mRadius * 1.1f, -1f, 1f);
//        Log.d("Operator", "MODE:" + mCursorOn + "DX:" + mDX + "DY:" + mDY);
        return true;
    }
}
