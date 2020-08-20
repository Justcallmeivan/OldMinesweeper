package com.example.ivan.minesweeper;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.GridLayout;

public class ModifiedGridLayout extends GridLayout {


    public ModifiedGridLayout(Context context) {
        super(context);
    }
    public ModifiedGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ModifiedGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        int count = getChildCount();
        Log.d("Child Count:", "" + count);

        int col = getColumnCount();
        int row = getRowCount();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        measureChildWithMargins(getChildAt(0), widthMeasureSpec, 0, heightMeasureSpec, 0);

        maxWidth = col * getChildAt(0).getMeasuredWidth();
        maxHeight = row* getChildAt(0).getMeasuredHeight();
        Log.d("Deb ", "Square Length1:" + getChildAt(0).getMeasuredWidth());

        for (int i = 0; i < count; i++) {
            childState = combineMeasuredStates(childState, getChildAt(0).getMeasuredState());
        }

        Log.d("Max Width: ", "" + maxWidth);

        setMeasuredDimension(maxWidth, maxHeight);
        Log.d("Measured Width: ", "" + getMeasuredWidth());

    }
}
