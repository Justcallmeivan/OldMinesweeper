package com.example.ivan.minesweeper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.*;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity implements OnTouchListener {

    ModifiedGridLayout mGridLayout;
    ImageView[] images;

    GridLayout mGridLayout1;

    private GestureDetector mDetector;
    private MyGestureListener myGestureListener;

    private float squareLength;

    //Scale of zoom
    //Change to static vars, consider different zooms for different devices
    private float minScale = 1.0f;
    private float recommendedScale = 1.0f;
    private float maxScale = 2.5f;
    private float mScaleFactor = 1.0f;

    private double dx = 0f;
    private double dy = 0f;


    //Available Modes
    //Snapback may be an outdated idea - 4/28
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int SNAPBACK = 3;
    static final int DRAG1 = 4;
    static final int DRAG2 = 5;

    //move to on create probably
    int mode = NONE;
    int fingers = 0;

    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;


    //Screen size in pixels
    float screenWidth;
    float screenHeight;
    float screenDiagonal;

    //Used in resolving touch controls in onTouch
    float translationX;
    float translationY;
    int columnCount;
    int rowCount;

    //Used in setting scale in setGridScale
    //Probably do not need to be global
    float zeroedX = 0;
    float zeroedY = 0;

    //Used in onTouch and other methods for screen zoom location calculations
    float zeroedX1 = 0;
    float zeroedY1 = 0;

    PointF pivot = new PointF();

    GameLogic mGameLogic;
    int[] visibleBoard;

    long lastDown = 0;
    long lastUp = 0;
    int lastIndex = -1;
    int index = -1;
    boolean isHeld = false;

    LinearLayout mLinearLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);

        mLinearLayout = new LinearLayout(this);


        mGridLayout = new ModifiedGridLayout(this);
        mGridLayout.setAlignmentMode(GridLayout.ALIGN_MARGINS);
        mGridLayout1 = new GridLayout(this);
        myGestureListener = new MyGestureListener();
        mDetector = new GestureDetector(this, myGestureListener);

        calculateWindowSize();
        setGridDimensions(16, 26);
        mGameLogic = new GameLogic(26, 16, 99);
        visibleBoard = mGameLogic.getConvertedBoard();
        setImages();
        squareLength = images[0].getDrawable().getIntrinsicWidth();
        Log.d("Deb ", "Square Length:" + squareLength);
        setRecommendedScale();
        setGridScale();

        mGridLayout1.addView(mGridLayout);
        mGridLayout1.setOnTouchListener(this);
        setContentView(mGridLayout1);

        pivot.set(screenWidth / 2, screenHeight / 2);
    }

    public void calculateWindowSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        Log.d("Deb ", "Screen width:" + screenWidth);
        Log.d("Deb ", "Screen height:" + screenHeight);
        screenDiagonal = (float) sqrt(screenWidth * screenWidth + screenHeight * screenHeight);
    }

    public void setGridDimensions(int col, int row) {
        columnCount = col;
        rowCount = row;
        mGridLayout.setColumnCount(col);
        mGridLayout.setRowCount(row);
    }

    public void setGridScale() {
        mGridLayout.setPivotX(width() / 2);
        mGridLayout.setPivotY(height() / 2);

        mGridLayout.setScaleX(recommendedScale);
        mGridLayout.setScaleY(recommendedScale);

        zeroedX = -width() / 2 + width() * recommendedScale / 2;
        zeroedY = -height() / 2 + height() * recommendedScale / 2;

        mGridLayout.setTranslationX(zeroedX);
        mGridLayout.setTranslationY(zeroedY);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        //GridLayout layout = (GridLayout) view;
        if (mode != SNAPBACK) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    start.set(event.getX(), event.getY());
                    mode = DRAG1;
                    fingers++;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    fingers++;
                    if (oldDist > 10f && mode != ZOOM) {
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d("Tap", "up");
                    lastUp = System.nanoTime();
//                    if (TimeUnit.NANOSECONDS.toSeconds(lastUp - lastDown) >= 1) {
//                        mGameLogic.hold(index);
//                    } else {
//                        mGameLogic.singleTap(index);
//                    }
                    Log.d("isHeld", "" + isHeld);
                    if (!isHeld && (mode == NONE || mode == DRAG1)) {
                        mGameLogic.singleTap(index);
                        visibleBoard = mGameLogic.getConvertedBoard();
                        updateImages();
                        lastIndex = index;
                    }
                    isHeld = false;
                    mode = NONE;
                case MotionEvent.ACTION_POINTER_UP:
                    fingers--;
                    if (fingers > 1) {
                        mode = ZOOM;
                    } else if (fingers == 1) {
                        mode = DRAG2;
                    } else {
                        mode = NONE;
                    }

                    if (mode == NONE) {
                        translationX = mGridLayout1.getTranslationX() + (mGridLayout1.getPivotX()) * (1 - mScaleFactor);
                        translationY = mGridLayout1.getTranslationY() + (mGridLayout1.getPivotY()) * (1 - mScaleFactor);

                        if (mScaleFactor < minScale) {
                            mode = SNAPBACK;
                            snapBack();
                        } else if (translationX > zeroedX1 ||
                                translationX < zeroedX1 + screenWidth * (minScale - mScaleFactor) ||
                                translationY > zeroedY1 ||
                                translationY < zeroedY1 + screenHeight * (minScale - mScaleFactor)) {
                            mode = SNAPBACK;
                            snapBackXY();
                        }
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    if (sqrt((event.getX() - start.x) * (event.getX() - start.x) + (event.getY() - start.y) * (event.getY() - start.y)) > 10f && mode == DRAG1) {
                        mode = DRAG;
                    } else if (mode == DRAG2) {
                        start.set(event.getX(0), event.getY(0));
                        mode = DRAG;
                    }
                    float maxDx = -(mGridLayout1.getPivotX()) * (1 - mScaleFactor);
                    float maxDy = -(mGridLayout1.getPivotY()) * (1 - mScaleFactor);
                    float minDx = screenWidth * (minScale - mScaleFactor) + maxDx;
                    float minDy = screenHeight * (minScale - mScaleFactor) + maxDy;
                    if (mode == DRAG) {
                        dx += event.getX() - start.x;
                        dy += event.getY() - start.y;

                        dx = Math.min(Math.max(dx, minDx), maxDx);
                        dy = Math.min(Math.max(dy, minDy), maxDy);

                        mGridLayout1.setTranslationX((float) dx);
                        mGridLayout1.setTranslationY((float) dy);

                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {

                            float scale = newDist / oldDist;
                            mScaleFactor *= scale;
                            mScaleFactor = Math.max(minScale, Math.min(mScaleFactor, maxScale));
                            Log.d("Scale: ", "" + mScaleFactor);


                            dx = mGridLayout1.getTranslationX() + (mGridLayout1.getPivotX() - mid.x) * (1 - mScaleFactor);
                            dy = mGridLayout1.getTranslationY() + (mGridLayout1.getPivotY() - mid.y) * (1 - mScaleFactor);

                            mGridLayout1.setTranslationX((float) dx);
                            mGridLayout1.setTranslationY((float) dy);

                            mGridLayout1.setPivotX(mid.x);
                            mGridLayout1.setPivotY(mid.y);
                            mGridLayout1.setScaleX(mScaleFactor);
                            mGridLayout1.setScaleY(mScaleFactor);


                            translationX = mGridLayout1.getTranslationX() + (mGridLayout1.getPivotX()) * (1 - mScaleFactor);
                            translationY = mGridLayout1.getTranslationY() + (mGridLayout1.getPivotY()) * (1 - mScaleFactor);

                            if (mScaleFactor < minScale) {
                                mode = SNAPBACK;
                                snapBack();
                            } else if (translationX > zeroedX1 ||
                                    translationX < zeroedX1 + screenWidth * (minScale - mScaleFactor) ||
                                    translationY > zeroedY1 ||
                                    translationY < zeroedY1 + screenHeight * (minScale - mScaleFactor)) {
                                snapBackXY();
                                mode = ZOOM;
                            }

                        }
                    }
                    break;


            }
        }
        return true;
    }

    private float spacing(MotionEvent event) {
        Log.d("fingers:", "" + fingers);
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) sqrt(x * x + y * y);

    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private float distanceToCenter(PointF point) {
        float x = point.x - screenWidth / 2;
        float y = point.y - screenHeight / 2;
        return (float) sqrt(x * x + y * y);
    }

    private void snapBack() {
        boolean isPivotSet = true;
        boolean isScaleSet = false;
        boolean isDXSet = false;
        boolean isDYSet = false;


        PointF pivotPoint = new PointF();
        pivotPoint.set(screenWidth / 2, screenHeight / 2);

        mGridLayout1.setPivotX(pivotPoint.x);
        mGridLayout1.setPivotY(pivotPoint.y);

        if (mScaleFactor >= minScale - .2) {
            mScaleFactor = minScale;
            isScaleSet = true;
        } else {
            mScaleFactor += .2;
        }

        mGridLayout1.setScaleX(mScaleFactor);
        mGridLayout1.setScaleY(mScaleFactor);

        if (isDXSet) {
            dx = zeroedX1;
        }
        if (isDYSet) {
            dy = zeroedY1;
        }

        dx = mGridLayout1.getTranslationX();
        dy = mGridLayout1.getTranslationY();


        if (dx == zeroedX1) {
            isDXSet = true;
        } else {
            snapX();
        }
        if (dy == zeroedY1) {
            isDYSet = true;
        } else {
            snapY();
        }

        mGridLayout1.setTranslationX((float) dx);
        mGridLayout1.setTranslationY((float) dy);

        if (isPivotSet && isScaleSet && isDXSet && isDYSet) {
            mode = NONE;
        } else {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            snapBack();
        }


    }

    public void snapX() {
        if (dx < zeroedX1 && dx >= zeroedX1 - screenWidth / 10) {
            dx = zeroedX1;
        } else if (dx > zeroedX1 && dx <= zeroedX1 + screenWidth / 10) {
            dx = zeroedX1;
        } else {
            if (dx > zeroedX1) {
                dx -= screenWidth / 10;
            } else if (dx < zeroedX1) {
                dx += screenWidth / 10;
            }
        }
    }

    public void snapY() {
        if (dy < zeroedY1 && dy >= zeroedY1 - screenHeight / 10) {
            dy = zeroedY1;
        } else if (dy > zeroedY1 && dy <= zeroedY1 + screenHeight / 10) {
            dy = zeroedY1;
        } else {
            if (dy > zeroedY1) {
                dy -= screenHeight / 10;
            } else if (dy < zeroedY1) {
                dy += screenHeight / 10;
            }
        }
    }

    public void snapBackX() {
        if (translationX < zeroedX1 + screenWidth * (1 - mScaleFactor)) {
            Log.d("well", "lesser");
            if (translationX <= zeroedX1 + screenWidth * (1 - mScaleFactor) + screenWidth / 10) {
                translationX = zeroedX1 + screenWidth * (1 - mScaleFactor);
            } else {
                Log.d("well", "yes");
                translationX += screenWidth / 10;
            }
        } else if (translationX > zeroedX1) {
            Log.d("well", "greater");
            if (translationX <= zeroedX1 + screenWidth / 10) {
                translationX = zeroedX1;
            } else {
                translationX -= screenWidth / 10;
            }
        }
    }

    public void snapBackY() {
        if (translationY < zeroedY1 + screenHeight * (1 - mScaleFactor)) {
            if (translationY <= zeroedY1 + screenHeight * (1 - mScaleFactor) + screenHeight / 10) {
                translationY = zeroedY1 + screenHeight * (1 - mScaleFactor);
            } else {
                translationY += screenHeight / 10;
            }
        } else if (translationY > zeroedY1) {
            if (translationY <= zeroedY1 + screenHeight / 10) {
                translationY = zeroedY1;
            } else {
                translationY -= screenHeight / 10;
            }
        }
    }

    public void snapBackXY() {
        if (translationX > zeroedX1 || translationX < zeroedX1 + screenWidth * (1 - mScaleFactor)) {
            snapBackX();
        }
        if (translationY > zeroedY1 || translationY < zeroedY1 + screenHeight * (1 - mScaleFactor)) {
            snapBackY();
        }
        dx = translationX - (mGridLayout1.getPivotX()) * (1 - mScaleFactor);
        dy = translationY - (mGridLayout1.getPivotY()) * (1 - mScaleFactor);
        mGridLayout1.setTranslationX((float) dx);
        mGridLayout1.setTranslationY((float) dy);
        if (translationX <= zeroedX1 && translationX >= zeroedX1 + screenWidth * (1 - mScaleFactor) && translationY <= zeroedY1 && translationY >= zeroedY1 + screenHeight * (1 - mScaleFactor)) {
            mode = NONE;
        } else {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            snapBackXY();
        }

    }

    public void setRecommendedScale() {
        float widthScale;
        float heightScale;

        widthScale = screenWidth / width();
        heightScale = screenHeight / height();

        if (widthScale < heightScale) {
            recommendedScale = widthScale;
        } else {
            recommendedScale = heightScale;
        }

    }

    private float width() {
        int col = columnCount;
        float width = col * squareLength;
        Log.d("Deb ", "Width:" + width);
        return width;
    }

    private float height() {
        int row = rowCount;
        float height = row * squareLength;
        Log.d("Deb ", "Height:" + height);
        return height;
    }

    private void setImages() {
        int total = columnCount * rowCount;
        images = new ImageView[total];
//        GridLayout.LayoutParams layoutParams=new GridLayout.LayoutParams();
//        layoutParams.setMargins(1,1,1,1);
//        layoutParams.width=98;
//        layoutParams.height=98;
        for (int i = 0; i < images.length; i++) {
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
            layoutParams.setMargins(0, 0, 0, 0);
            //layoutParams.width = 100;
            //layoutParams.height = 100;
            images[i] = new ImageView(this);
            if (visibleBoard[i] == mGameLogic.UNTOUCHED) {
                images[i].setImageResource(R.drawable.blue_square);
            } else if (visibleBoard[i] == mGameLogic.FLAG) {
                images[i].setImageResource(R.drawable.flag);
            } else if (visibleBoard[i] == mGameLogic.QUESTION) {
                images[i].setImageResource(R.drawable.question);
            } else if (visibleBoard[i] == mGameLogic.MINE) {
                images[i].setImageResource(R.drawable.mine);
            } else if (visibleBoard[i] == mGameLogic.EMPTY) {
                images[i].setImageResource(R.drawable.empty_square);
            } else if (visibleBoard[i] == mGameLogic.ONE) {
                images[i].setImageResource(R.drawable.one);
            } else if (visibleBoard[i] == mGameLogic.TWO) {
                images[i].setImageResource(R.drawable.two);
            } else if (visibleBoard[i] == mGameLogic.THREE) {
                images[i].setImageResource(R.drawable.three);
            } else if (visibleBoard[i] == mGameLogic.FOUR) {
                images[i].setImageResource(R.drawable.four);
            } else if (visibleBoard[i] == mGameLogic.FIVE) {
                images[i].setImageResource(R.drawable.five);
            } else if (visibleBoard[i] == mGameLogic.SIX) {
                images[i].setImageResource(R.drawable.six);
            } else if (visibleBoard[i] == mGameLogic.SEVEN) {
                images[i].setImageResource(R.drawable.seven);
            } else if (visibleBoard[i] == mGameLogic.EIGHT) {
                images[i].setImageResource(R.drawable.eight);
            }
            images[i].setOnTouchListener(touchListener);
            images[i].setLayoutParams(layoutParams);
        }
        //images[0].setOnTouchListener(touchListener);
        //see if i can extend views by their edge pixels, maybe using matrices
        // a
        //extension should be done only to the bottom and right edges

        for (int i = 0; i < images.length; i++) {
            mGridLayout.addView(images[i], i);
        }
        Log.d("Deb ", "Width1: " + images[0].getDrawable().getIntrinsicWidth());
    }

    //set file locations for images to variables for easy change
    public void updateImages() {
        Log.d("Tap", "updating");
        for (int i = 0; i < images.length; i++) {
            // Log.d("Tap", "updating" + i);
//            try {
            // images[i] = new ImageView(this);
            if (visibleBoard[i] == mGameLogic.UNTOUCHED) {
                images[i].setImageResource(R.drawable.blue_square);
            } else if (visibleBoard[i] == mGameLogic.FLAG) {
                images[i].setImageResource(R.drawable.flag);
            } else if (visibleBoard[i] == mGameLogic.QUESTION) {
                images[i].setImageResource(R.drawable.question);
            } else if (visibleBoard[i] == mGameLogic.MINE) {
                images[i].setImageResource(R.drawable.mine);
            } else if (visibleBoard[i] == mGameLogic.EMPTY) {
                images[i].setImageResource(R.drawable.empty_square);
            } else if (visibleBoard[i] == mGameLogic.ONE) {
                images[i].setImageResource(R.drawable.one);
            } else if (visibleBoard[i] == mGameLogic.TWO) {
                images[i].setImageResource(R.drawable.two);
            } else if (visibleBoard[i] == mGameLogic.THREE) {
                images[i].setImageResource(R.drawable.three);
            } else if (visibleBoard[i] == mGameLogic.FOUR) {
                images[i].setImageResource(R.drawable.four);
            } else if (visibleBoard[i] == mGameLogic.FIVE) {
                images[i].setImageResource(R.drawable.five);
            } else if (visibleBoard[i] == mGameLogic.SIX) {
                images[i].setImageResource(R.drawable.six);
            } else if (visibleBoard[i] == mGameLogic.SEVEN) {
                images[i].setImageResource(R.drawable.seven);
            } else if (visibleBoard[i] == mGameLogic.EIGHT) {
                images[i].setImageResource(R.drawable.eight);
            }
            // images[i].setOnTouchListener(touchListener);
//            }catch(Exception e){
//                Log.d("Error:", "");
//                e.printStackTrace();
//
//            }
        }
        Log.d("Tap", "updated");
    }

    //likely unnecessary
    public void setFlag(int i) {
        try {
            if (visibleBoard[i] == GameLogic.FLAG) {
                Bitmap bImage = BitmapFactory.decodeResource(this.getResources(), R.drawable.flag);
                images[i].setImageBitmap(bImage);
            }
        } catch (Exception e) {
            Log.d("Error:", "");
            e.printStackTrace();

        }
    }

    View.OnTouchListener touchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d("Tap", "down");
            Log.d("Mode", "" + mode);
            if (mode == NONE || mode == DRAG) {
                index = -1;
                for (int i = 0; i < images.length; i++) {
                    if (v.equals(images[i])) {
                        index = i;
                    }
                }
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d("Tap", "initial down");
                        lastDown = System.nanoTime();
                        if (TimeUnit.NANOSECONDS.toSeconds(lastDown - lastUp) <= .3 && index == lastIndex) {
                            mGameLogic.doubleTap(index);
                            visibleBoard = mGameLogic.getConvertedBoard();
                            updateImages();
                            lastIndex = index;
                        }
                        Runnable myRunnable = new Runnable() {

                            public void run() {
                                final int currentIndex = index;
                                final float currentLastDown = lastDown;
                                final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                                executorService.schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("Tap", "delay");
                                        Log.d("Tap", "" + lastDown);
                                        Log.d("Tap", "" + lastUp);
                                        //updateImages();
                                        if (currentLastDown > lastUp && (mode == NONE || mode == DRAG1) && index == currentIndex) {
                                            Log.d("Tap", "hold");
                                            mGameLogic.hold(index);
                                            visibleBoard = mGameLogic.getConvertedBoard();
                                            runOnUiThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    updateImages();
                                                }
                                            });
                                            isHeld = true;
                                            //updateImages();
                                            //setFlag(index);
                                            lastIndex = index;
                                            Log.d("Tap", "updated1");
                                        }
                                    }
                                }, 500, TimeUnit.MILLISECONDS);
                                Log.d("Tap", "delay");
                            }
                        };

                        Thread thread = new Thread(myRunnable);
                        thread.start();


                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        Log.d("Tap", "up");
                        lastUp = System.nanoTime();
                        if (TimeUnit.NANOSECONDS.toSeconds(lastUp - lastDown) >= .5) {
                            //mGameLogic.hold(index);
                        } else {
                            mGameLogic.singleTap(index);
                        }
                        lastIndex = index;
                        break;


                }
//            if (mode == NONE) {
//                myGestureListener.setIndex((ImageView) v);
//                return mDetector.onTouchEvent(event);
//            }
            }
            return false;

        }
    };


    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        int index;

        public void setIndex(ImageView view) {
            for (int i = 0; i < images.length; i++)
                if (view.equals(images[i])) {
                    index = i;
                }
        }

        @Override
        public boolean onDown(MotionEvent event) {
            // don't return false here or else none of the other
            // gestures will work
            Log.d("Tap", "down");
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mode == NONE) {
                mGameLogic.singleTap(index);
                visibleBoard = mGameLogic.getConvertedBoard();
                updateImages();
            }
            Log.d("Tap", "single");
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
//            if (mode == NONE) {
//                mGameLogic.hold(index);
//                visibleBoard = mGameLogic.getConvertedBoard();
//                updateImages();
//            }
            //Log.d("Tap", "hold");

        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mode == NONE) {
                mGameLogic.doubleTap(index);
                visibleBoard = mGameLogic.getConvertedBoard();
                updateImages();
            }
            Log.d("Tap", "double");
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            Log.i("TAG", "onScroll: ");
            return false;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d("TAG", "onFling: ");
            return false;
        }

    }

}
