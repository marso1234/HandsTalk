package com.handstalk.signdetect.translate.view;

import android.graphics.Canvas;

import com.google.mediapipe.tasks.components.containers.Connection;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;

import java.util.List;

public class HandLandmarkPaints extends LandmarkPaints{

    @Override
    public void draw(Canvas canvas, List<NormalizedLandmark> landmarkList) {
        super.draw(canvas, landmarkList);
        for (Connection handLandmark : HandLandmarker.HAND_CONNECTIONS) {
            canvas.drawLine(
                    landmarkList.get(handLandmark.start())
                            .x() * imageWidth * scaleFactor,
                    landmarkList.get(handLandmark.start())
                            .y() * imageHeight * scaleFactor,
                    landmarkList.get(handLandmark.end())
                            .x() * imageWidth * scaleFactor,
                    landmarkList.get(handLandmark.end())
                            .y() * imageHeight * scaleFactor,
                    linePaint
            );
        }
    }
}
