package com.handstalk.signdetect.translate.view;

import static java.lang.Math.max;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import com.handstalk.signdetect.R;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;

public class LandmarkPaints {
    protected final float LANDMARK_STROKE_WIDTH = 8f;
    protected Paint linePaint = new Paint();
    protected Paint pointPaint = new Paint();

    protected float scaleFactor = 1f;
    protected int imageWidth = 1;
    protected int imageHeight = 1;
    private Context context;

    public void initPaints(){
        linePaint.setColor(ContextCompat.getColor(context, R.color.purple_500));

        linePaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint.setColor(ContextCompat.getColor(context, R.color.purple_700));
        pointPaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    public void initPaints(Context context){
        this.context = context;
        initPaints();
    }

    public void clear(){
        linePaint.reset();
        pointPaint.reset();

        initPaints();
    }

    public void setValue(int imageHeight, int imageWidth, float scaleFactor){
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
        this.scaleFactor = scaleFactor;
    }

    public void draw(Canvas canvas, List<NormalizedLandmark> landmarkList) {
        for(NormalizedLandmark landmark: landmarkList){
            canvas.drawPoint(
                    landmark.x() * imageWidth * scaleFactor,
                    landmark.y() * imageHeight * scaleFactor,
                    pointPaint
                    );
        }
    }
}
