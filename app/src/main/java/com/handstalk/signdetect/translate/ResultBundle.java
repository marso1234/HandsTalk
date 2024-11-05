package com.handstalk.signdetect.translate;

import com.google.mediapipe.tasks.core.TaskResult;

public class ResultBundle {
    public static enum TYPE_LANDMARK {hand, pose;}

    private TaskResult results;
    private Long inferenceTime;
    private int inputImageHeight;
    private int inputImageWidth;
    private TYPE_LANDMARK typeLandmark;

    public ResultBundle(TaskResult results,
                        Long inferenceTime,
                        int inputImageHeight,
                        int inputImageWidth,
                        TYPE_LANDMARK typeLandmark) {
        this.results = results;
        this.inferenceTime = inferenceTime;
        this.inputImageHeight = inputImageHeight;
        this.inputImageWidth = inputImageWidth;
        this.typeLandmark = typeLandmark;
    }

    public int getInputImageHeight() {
        return inputImageHeight;
    }

    public Long getInferenceTime() {
        return inferenceTime;
    }

    public TaskResult getResults() {
        return results;
    }

    public int getInputImageWidth() {
        return inputImageWidth;
    }

    public TYPE_LANDMARK getTypeLandmark() {
        return typeLandmark;
    }

    public void setTypeLandmark(TYPE_LANDMARK typeLandmark) {
        this.typeLandmark = typeLandmark;
    }
}