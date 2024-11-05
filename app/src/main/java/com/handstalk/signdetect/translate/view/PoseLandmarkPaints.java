package com.handstalk.signdetect.translate.view;

import android.graphics.Canvas;

import com.google.mediapipe.tasks.components.containers.Connection;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;

import java.util.List;

public class PoseLandmarkPaints extends LandmarkPaints{
    
    @Override
    public void draw(Canvas canvas, List<NormalizedLandmark> landmarkList) {
        super.draw(canvas, landmarkList);
        for (Connection poseLandmark : PoseLandmarker.POSE_LANDMARKS) {
            canvas.drawLine(
                    landmarkList.get(poseLandmark.start())
                            .x() * imageWidth * scaleFactor,
                    landmarkList.get(poseLandmark.start())
                            .y() * imageHeight * scaleFactor,
                    landmarkList.get(poseLandmark.end())
                            .x() * imageWidth * scaleFactor,
                    landmarkList.get(poseLandmark.end())
                            .y() * imageHeight * scaleFactor,
                    linePaint
                    );
        }
    }
}
