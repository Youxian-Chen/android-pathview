package com.eftimoff.androipathview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;

import com.eftimoff.mylibrary.R;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.AnimatorSet;

import java.util.ArrayList;
import java.util.List;

/**
 * PathView is a View that animates paths.
 */
@SuppressWarnings("unused")
public class PathView extends View {
    public static final String LOG_TAG = "PathView";
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SvgUtils svgUtils = new SvgUtils(paint);
    private List<SvgUtils.SvgPath> paths = new ArrayList<>();
    private final Object mSvgLock = new Object();
    private Thread mLoader;
    private int svgResourceId;
    private float progress = 1f;
    private boolean naturalColors;
    private boolean fillAfter;
    private boolean fill;
    private int divider;

    private int fillColor;
    private int width;
    private int height;

    private Bitmap mTempBitmap;
    private Canvas mTempCanvas;
    private boolean backgroundInit = false;
    private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap mBackgroundBitmap;
    private Canvas mBackgroundCanvas;
    private Bitmap mAnimateBitmap;
    private Canvas mAnimateCanvas;
    private int mStateCounter = 0;
    private List<Path> originPaths = new ArrayList<>();
    private Paint tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isBackward = false;
    private boolean isForward = false;

    public PathView(Context context) {
        this(context, null);
    }

    public PathView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PathView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        getFromAttributes(context, attrs);
        tempPaint.setStyle(Paint.Style.STROKE);
        tempPaint.setColor(Color.RED);
        tempPaint.setStrokeWidth(8);
        tempPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    private void getFromAttributes(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PathView);
        try {
            if (a != null) {
                paint.setColor(a.getColor(R.styleable.PathView_pathColor, 0xffff0000));
                paint.setStrokeWidth(a.getDimensionPixelSize(R.styleable.PathView_pathWidth, 8));
                svgResourceId = a.getResourceId(R.styleable.PathView_svg, 0);
                naturalColors = a.getBoolean(R.styleable.PathView_naturalColors, false);
                fill = a.getBoolean(R.styleable.PathView_fill,false);
                fillColor = a.getColor(R.styleable.PathView_fillColor,Color.argb(0,0,0,0));
                divider = a.getInteger(R.styleable.PathView_pathDivider, 1);
            }
        } finally {
            if (a != null) {
                a.recycle();
            }
            //to draw the svg in first show , if we set fill to true
            invalidate();
        }
    }

    public void setPaths(final List<Path> paths) {
        for (Path path : paths) {
            this.paths.add(new SvgUtils.SvgPath(path, paint));
        }
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
    }

    public void setPath(final Path path) {
        paths.add(new SvgUtils.SvgPath(path, paint));
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
    }

    public void setPercentage(float percentage) {
        if (percentage < 0.0f || percentage > 1.0f) {
            throw new IllegalArgumentException("setPercentage not between 0.0f and 1.0f");
        }
        progress = percentage;
        synchronized (mSvgLock) {
            updatePathsPhaseLocked();
        }
        invalidate();
    }

    private void updatePathsPhaseLocked() {
        final int count = paths.size();
        for (int i = 0; i < count; i++) {
            SvgUtils.SvgPath svgPath = paths.get(i);
            svgPath.path.reset();
            svgPath.measure.getSegment(0.0f, svgPath.length * progress, svgPath.path, true);
            // Required only for Android 4.4 and earlier
            svgPath.path.rLineTo(0.0f, 0.0f);
        }
        if (mBackgroundBitmap == null && mBackgroundCanvas == null) {
            Log.d(LOG_TAG, "background not init");
            Handler handler = new Handler(getContext().getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    invalidate();
                    backgroundInit = true;
                }
            });
        }
    }

    public void setStateCounter(int counter) {
        mStateCounter = counter;
        Log.d(LOG_TAG, "counter: " + mStateCounter);
    }

    public int getStateCounter() {
        return mStateCounter;
    }

    public void setStateDivider(int divider) {
        if (divider > 0) {
            this.divider = divider;
        }
    }

    public void setBackward(boolean backward) {
        isBackward = backward;
    }

    public void setForward(boolean forward) {
        isForward = forward;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundInit && mBackgroundBitmap == null && mBackgroundCanvas == null) {
            Log.d(LOG_TAG, "draw background");
            mBackgroundBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mBackgroundCanvas = new Canvas(mBackgroundBitmap);
            backgroundPaint.setStyle(Paint.Style.STROKE);
            backgroundPaint.setStrokeWidth(8);
            backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
            backgroundPaint.setColor(Color.GRAY);

            synchronized (mSvgLock) {
                mBackgroundCanvas.save();
                mBackgroundCanvas.translate(getPaddingLeft(), getPaddingTop());
                fill(mBackgroundCanvas);
                int count = paths.size();
                for (int i = 0; i < count; i++) {
                    SvgUtils.SvgPath svgPath = paths.get(i);
                    Path path = new Path(svgPath.path);
                    originPaths.add(path);
                    mBackgroundCanvas.drawPath(path, backgroundPaint);
                }
                fillAfter(mBackgroundCanvas);
                mBackgroundCanvas.restore();
                applySolidColor(mBackgroundBitmap);
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
                progress = 0f;
                return;
            }
        }

        if(mAnimateBitmap == null || (mAnimateBitmap.getWidth() != canvas.getWidth() || mAnimateBitmap.getHeight() != canvas.getHeight()))
        {
            mAnimateBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mAnimateCanvas = new Canvas(mAnimateBitmap);

            mTempBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mTempCanvas = new Canvas(mTempBitmap);
        }

        if (isBackward) {
            if (mStateCounter > 0) {
                mTempCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                Log.d(LOG_TAG, "draw temp backward");
                int subState = (mStateCounter - 1) * divider;
                for (int j = 0; j < subState; j++) {
                    Path path = originPaths.get(j);
                    mTempCanvas.drawPath(path, tempPaint);
                }
                isBackward = false;
            }
        }

        if (isForward) {
            if (mStateCounter > 0) {
                Log.d(LOG_TAG, "draw forward");
                int subState = (mStateCounter - 1) * divider;
                for (int j = 0; j < subState; j++) {
                    Path path = originPaths.get(j);
                    mTempCanvas.drawPath(path, tempPaint);
                }
                isForward = false;
            }
        }

        mAnimateBitmap.eraseColor(0);
        synchronized (mSvgLock) {
            mAnimateCanvas.save();
            mAnimateCanvas.translate(getPaddingLeft(), getPaddingTop());
            fill(mAnimateCanvas);
            int count = paths.size();
            if (paths.size() > 0 && mStateCounter > 0) {
                int start = (mStateCounter - 1) * divider;
                int end = mStateCounter * divider;
                if (mStateCounter == 5)
                    end = end - 1;
                if (end > count) {
                    end = count;
                }
                for (int i = start; i < end; i++) {
                    SvgUtils.SvgPath svgPath = paths.get(i);
                    Path path = svgPath.path;
                    Paint paint1 = naturalColors ? svgPath.paint : paint;
                    mAnimateCanvas.drawPath(path, paint1);
                }

            }

            fillAfter(mAnimateCanvas);
            mAnimateCanvas.restore();
            applySolidColor(mAnimateBitmap);

            if (mBackgroundBitmap != null)
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            canvas.drawBitmap(mTempBitmap, 0, 0, null);
            canvas.drawBitmap(mAnimateBitmap, 0, 0, null);
        }
    }

    private void fillAfter(final Canvas canvas) {
        if (svgResourceId != 0 && fillAfter && progress == 1f) {
            svgUtils.drawSvgAfter(canvas, width, height);
        }
    }

    private void fill(final Canvas canvas) {
        if (svgResourceId != 0 && fill) {
            svgUtils.drawSvgAfter(canvas, width, height);
        }
    }

    private void applySolidColor(final Bitmap bitmap) {
        if(fill && fillColor!=Color.argb(0,0,0,0) )
            if (bitmap != null) {
                for(int x=0;x<bitmap.getWidth();x++)
                {
                    for(int y=0;y<bitmap.getHeight();y++)
                    {
                        int argb = bitmap.getPixel(x,y);
                        int alpha = Color.alpha(argb);
                        if(alpha!=0)
                        {
                            int red = Color.red(fillColor);
                            int green = Color.green(fillColor);
                            int blue =  Color.blue(fillColor);
                            argb = Color.argb(alpha,red,green,blue);
                            bitmap.setPixel(x,y,argb);
                        }
                    }
                }
            }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mLoader != null) {
            try {
                mLoader.join();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Unexpected error", e);
            }
        }
        if (svgResourceId != 0) {
            mLoader = new Thread(new Runnable() {
                @Override
                public void run() {

                    svgUtils.load(getContext(), svgResourceId);

                    synchronized (mSvgLock) {
                        width = w - getPaddingLeft() - getPaddingRight();
                        height = h - getPaddingTop() - getPaddingBottom();
                        paths = svgUtils.getPathsForViewport(width, height);
                        updatePathsPhaseLocked();
                    }
                }
            }, "SVG Loader");
            mLoader.start();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (svgResourceId != 0) {
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(widthSize, heightSize);
            return;
        }

        int desiredWidth = 0;
        int desiredHeight = 0;
        final float strokeWidth = paint.getStrokeWidth() / 2;
        for (SvgUtils.SvgPath path : paths) {
            desiredWidth += path.bounds.left + path.bounds.width() + strokeWidth;
            desiredHeight += path.bounds.top + path.bounds.height() + strokeWidth;
        }
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(widthMeasureSpec);

        int measuredWidth, measuredHeight;

        if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = desiredWidth;
        } else {
            measuredWidth = widthSize;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = desiredHeight;
        } else {
            measuredHeight = heightSize;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    public void setFillAfter(final boolean fillAfter) {
        this.fillAfter = fillAfter;
    }

    public void setFill(final boolean fill) {
        this.fill = fill;
    }

    public void setFillColor(final int color){
        this.fillColor=color;
    }
    public void useNaturalColors() {
        naturalColors = true;
    }

    public int getPathColor() {
        return paint.getColor();
    }

    public void setPathColor(final int color) {
        paint.setColor(color);
    }

    public float getPathWidth() {
        return paint.getStrokeWidth();
    }

    public void setPathWidth(final float width) {
        paint.setStrokeWidth(width);
    }

    public int getSvgResource() {
        return svgResourceId;
    }

    public void setSvgResource(int svgResource) {
        svgResourceId = svgResource;
    }
    
}