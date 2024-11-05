package com.handstalk.signdetect.translate;

import static java.lang.Math.max;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.handstalk.signdetect.translate.view.HandLandmarkPaints;
import com.handstalk.signdetect.translate.view.LandmarkPaints;
import com.handstalk.signdetect.translate.view.PoseLandmarkPaints;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

public class HolisticOverlay extends View {
    private HandLandmarkerResult handResults;
    private PoseLandmarkerResult poseResult;

    private final HandLandmarkPaints[] handLandmarkPaints = new HandLandmarkPaints[2];
    private final LandmarkPaints poseLandmarkPaints = new PoseLandmarkPaints();

    private Context context;

    public HolisticOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initHandLandmarkPaints();
        initPoseLandmarkPaints();
    }

    public void clear(){
        clearHand();
        clearPose();
    }

    public void clearHand() {
        handResults = null;
        for (HandLandmarkPaints hp: handLandmarkPaints){
            hp.clear();
        }
        invalidate();
    }

    public void clearPose() {
        poseResult = null;
        poseLandmarkPaints.clear();
        invalidate();
    }

    private void initHandLandmarkPaints(){
        for (int i=0; i< handLandmarkPaints.length; ++i){
            if(handLandmarkPaints[i]==null){
                handLandmarkPaints[i] = new HandLandmarkPaints();
            }
            handLandmarkPaints[i].initPaints(context);
        }
    }

    private void initPoseLandmarkPaints(){
        poseLandmarkPaints.initPaints(context);
    }

    public void setHandResults(HandLandmarkerResult result, int imageHeight, int imageWidth){
        handResults = result;
        float scaleFactor = max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
        for(int i=0; i< result.landmarks().size(); ++i){
            handLandmarkPaints[i].setValue(imageHeight, imageWidth, scaleFactor);
        }
        invalidate();
    }

    public void setPoseResult(PoseLandmarkerResult result, int imageHeight, int imageWidth){
        poseResult = result;
        float scaleFactor = max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);
        poseLandmarkPaints.setValue(imageHeight, imageWidth, scaleFactor);
        invalidate();
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if(handResults != null){
            for(int i=0; i< handResults.landmarks().size(); ++i){
                handLandmarkPaints[i].draw(canvas, handResults.landmarks().get(i));
            }
        }
        if(poseResult != null){
            if(!poseResult.landmarks().isEmpty())
                poseLandmarkPaints.draw(canvas, poseResult.landmarks().get(0));
        }
    }
}
