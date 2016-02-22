package com.eftimoff.androipathview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;

import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.ArrayList;
import java.util.List;

public class SvgUtils {
    private static final String LOG_TAG = "SVGUtils";
    private final List<SvgPath> mPaths = new ArrayList<>();
    private final Paint mSourcePaint;
    private SVG mSvg;
    public SvgUtils(final Paint sourcePaint) {
        mSourcePaint = sourcePaint;
    }
    public void load(Context context, int svgResource) {
        if (mSvg != null) return;
        try {
            mSvg = SVG.getFromResource(context, svgResource);
            mSvg.setDocumentPreserveAspectRatio(PreserveAspectRatio.UNSCALED);
        } catch (SVGParseException e) {
            Log.e(LOG_TAG, "Could not load specified SVG resource", e);
        }
    }
    public void drawSvgAfter(final Canvas canvas, final int width, final int height) {
        final float strokeWidth = mSourcePaint.getStrokeWidth();
        rescaleCanvas(width, height, strokeWidth, canvas);
    }

    public List<SvgPath> getPathsForViewport(final int width, final int height) {
        final float strokeWidth = mSourcePaint.getStrokeWidth();
        Canvas canvas = new Canvas() {
            private final Matrix mMatrix = new Matrix();

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public void drawPath(Path path, Paint paint) {
                Path dst = new Path();

                //noinspection deprecation
                getMatrix(mMatrix);
                path.transform(mMatrix, dst);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(strokeWidth);
                mPaths.add(new SvgPath(dst, paint));
            }
        };

        rescaleCanvas(width, height, strokeWidth, canvas);

        return mPaths;
    }

    private void rescaleCanvas(int width, int height, float strokeWidth, Canvas canvas) {
        if (mSvg == null) return;
        final RectF viewBox = mSvg.getDocumentViewBox();

        final float scale = Math.min(width
                        / (viewBox.width() + strokeWidth),
                height / (viewBox.height() + strokeWidth));

        canvas.translate((width - viewBox.width() * scale) / 2.0f,
                (height - viewBox.height() * scale) / 2.0f);
        canvas.scale(scale, scale);

        mSvg.renderToCanvas(canvas);
    }

    public static class SvgPath {

        private static final Region REGION = new Region();
        private static final Region MAX_CLIP =
                new Region(Integer.MIN_VALUE, Integer.MIN_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE);
        final Path path;
        final Paint paint;
        float length;
        final Rect bounds;
        final PathMeasure measure;

        SvgPath(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;

            measure = new PathMeasure(path, false);
            this.length = measure.getLength();

            REGION.setPath(path, MAX_CLIP);
            bounds = REGION.getBounds();
        }

        public void setLength(float length) {
            path.reset();
            measure.getSegment(0.0f, length, path, true);
            path.rLineTo(0.0f, 0.0f);
        }

        public float getLength() {
            return length;
        }
    }
}