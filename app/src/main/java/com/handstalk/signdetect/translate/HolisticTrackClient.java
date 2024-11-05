package com.handstalk.signdetect.translate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemClock;

import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageProxy;

import com.handstalk.signdetect.utilities.Constants;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.core.TaskResult;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;

public class HolisticTrackClient {
    private static HolisticTrackClient holisticTrackClient = null;
    private HandLandmarker handLandmarker;
    private PoseLandmarker poseLandmarker;
    private LandmarkerListener landmarkerListener;

    private static final float Confidence = 0.5f;

    public static HolisticTrackClient getInstance(){
        if(holisticTrackClient ==null){
            holisticTrackClient = new HolisticTrackClient();
        }
        return holisticTrackClient;
    }

    public void initializeClient(Context context, LandmarkerListener landmarkerListener){
        holisticTrackClient = this;
        this.landmarkerListener = landmarkerListener;

        // Build Hand Base Option
        BaseOptions.Builder handBaseOptionBuilder = BaseOptions.builder();
        handBaseOptionBuilder.setDelegate(Delegate.GPU);
        handBaseOptionBuilder.setModelAssetPath(Constants.HAND_TASK_PATH);
        BaseOptions handBaseOptions = handBaseOptionBuilder.build();

        // Hand Option
        HandLandmarker.HandLandmarkerOptions.Builder handOptionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(handBaseOptions)
                .setNumHands(2)
                .setMinHandDetectionConfidence(Confidence)
                .setRunningMode(RunningMode.LIVE_STREAM);

        handOptionsBuilder.setResultListener((result, input)->{
            returnLivestreamResult(result, input, ResultBundle.TYPE_LANDMARK.hand);
        });
        handOptionsBuilder.setErrorListener(this::returnLivestreamError);
        handLandmarker = HandLandmarker.createFromOptions(context, handOptionsBuilder.build());

        // Build Pose Base Option
        BaseOptions.Builder poseBaseOptionBuilder = BaseOptions.builder();
        poseBaseOptionBuilder.setDelegate(Delegate.GPU);
        poseBaseOptionBuilder.setModelAssetPath(Constants.POSE_TASK_PATH);
        BaseOptions poseBaseOptions = poseBaseOptionBuilder.build();

        // Pose Option
        PoseLandmarker.PoseLandmarkerOptions.Builder poseOptionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBaseOptions)
                .setMinPoseDetectionConfidence(Confidence)
                .setRunningMode(RunningMode.LIVE_STREAM);

        poseOptionsBuilder.setResultListener((result, input)->{
            returnLivestreamResult(result, input, ResultBundle.TYPE_LANDMARK.pose);
        });
        poseOptionsBuilder.setErrorListener(this::returnLivestreamError);
        poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptionsBuilder.build());
    }

    // Convert the ImageProxy to MP Image and feed it to HandlandmakerHelper.
    public void detectLiveStream(
            ImageProxy imageProxy,
            Boolean isFrontCamera
    ) {
        long frameTime = SystemClock.uptimeMillis();

        // Copy out RGB bits from the frame to a bitmap buffer
        Bitmap bitmapBuffer =
                Bitmap.createBitmap(
                        imageProxy.getWidth(),
                        imageProxy.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
        imageProxy.close();

        Matrix matrix = new Matrix();
        matrix.postRotate((float)imageProxy.getImageInfo().getRotationDegrees());
        if (isFrontCamera) {
            matrix.postScale(
                    -1f,
                    1f,
                    (float) imageProxy.getWidth(),
                    (float) imageProxy.getHeight()
            );
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(),
                matrix, true
        );

//        Bitmap outputbmp = Bitmap.createBitmap(rotatedBitmap.getWidth() + 2 * 240, rotatedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(outputbmp);
//        canvas.drawColor(Color.WHITE); // Change this if you need another background color
//        canvas.drawBitmap(rotatedBitmap, 120, 0, null);


        // Convert the input Bitmap object to an MPImage object to run inference
         MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();

        detectHandAsync(mpImage, frameTime);
        detectPoseAsync(mpImage, frameTime);
    }

    public void detectMP(MPImage mpImage){
        long frameTime = SystemClock.uptimeMillis();
        detectHandAsync(mpImage, frameTime);
        detectPoseAsync(mpImage, frameTime);
    }

    // Run hand hand landmark using MediaPipe Hand Landmarker API
    @VisibleForTesting
    private void detectHandAsync(MPImage mpImage, Long frameTime) {
        if(handLandmarker != null) {
            handLandmarker.detectAsync(mpImage, frameTime);
        }
    }

    @VisibleForTesting
    private void detectPoseAsync(MPImage mpImage, Long frameTime) {
        if(poseLandmarker != null) {
            poseLandmarker.detectAsync(mpImage, frameTime);
        }
    }

    private void returnLivestreamResult(TaskResult result, MPImage input, ResultBundle.TYPE_LANDMARK typeLandmark) {
        long finishTimeMs = SystemClock.uptimeMillis();
        long inferenceTime = finishTimeMs - result.timestampMs();
        landmarkerListener.onResults(new ResultBundle(
                result,
                inferenceTime,
                input.getHeight(),
                input.getWidth(),
                typeLandmark
        ));
    }

    private void returnLivestreamError(RuntimeException e) {
        landmarkerListener.onError(e.getMessage());
    }

    public interface LandmarkerListener {
        void onError(String error);
        void onResults(ResultBundle resultBundle);
    }

}
