package com.madinabektayeva.unist_indoortracker;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Created by madina on 02.05.18.
 */


public class CustomView extends android.support.v7.widget.AppCompatImageView {

    Bitmap bitmap;

    private Rect imageBounds;
    private Drawable mCustomImage;
    private int pathMarkSize;

    int x;
    int y;


    public CustomView(Context context) {
        super(context);
    }

    public CustomView(Context context, AttributeSet attrst) {
        super(context, attrst);
        mCustomImage = context.getResources().getDrawable(R.drawable.pathmark);
        imageBounds = new Rect(0,0,0,0);
        pathMarkSize = 16;

    }


    public CustomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void drawPathMark(int x, int y, Bitmap bitmap){
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
        invalidate();

    }

    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        this.setImageBitmap(bitmap);
        imageBounds = new Rect(x-pathMarkSize/2, y+pathMarkSize/2, x+pathMarkSize/2, y-pathMarkSize/2);
        mCustomImage.setBounds(imageBounds);
        mCustomImage.draw(canvas);

    }

}