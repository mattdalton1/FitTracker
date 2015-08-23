package com.matthew.fittracker.fit_tracker.logic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by dalton on 20/08/2015.
 * Description: To create a pop up box.
 * Source: Keerthy, Android Codes,
 * available at http://android-codes-examples.blogspot.ie/2011/04/animated-customized-popup-transparent.html
 */
public class MiniMenu extends LinearLayout {
    private Paint innerPaint, borderPaint;

    public MiniMenu(Context context, AttributeSet as){
        super(context, as);
        init();
    }
    public MiniMenu(Context context){
        super(context);
        init();
    }
    private void init(){
        innerPaint = new Paint();
        innerPaint.setARGB(91, 164, 255, 0);
        innerPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setARGB(255, 255, 255, 255);
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10);
    }
    public void setInnerPaint(Paint innerPaint){
        this.innerPaint = innerPaint;
    }
    public void setBorderPaint(Paint borderPaint){
        this.borderPaint = borderPaint;
    }
    public void dispatchDraw(Canvas canvas){
        RectF rect = new RectF();
        rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        canvas.drawRoundRect(rect, 5, 5, innerPaint);
        canvas.drawRoundRect(rect, 5, 5, borderPaint);
        super.dispatchDraw(canvas);
    }
}
