package com.handstalk.signdetect.translate;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.handstalk.signdetect.sentenceCompletion.DetectionBuffer;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.jetbrains.annotations.TestOnly;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;


public class SignDetectClient {
    private static SignDetectClient signDetectClient;
    private LandmarkBuffer inputBuffer;
    private FloatBuffer outputBuffer;
    private static ArrayList<String> labelList;
    File modelFile;

    // Constant
    public static final String DEFAULT_MODEL = "Sign-Detector";
    private static final int DEFAULT_OUTPUT_SIZE = 84;
    private static final int DEFAULT_LANDMARK_SIZE = 20;

    public static final String TEST_MODEL = "test-model";
    private static final int TEST_OUTPUT_SIZE = 142;
    private static final int TEST_LANDMARK_BUFFER_SIZE = 20;

    private static final float PROB_THRESHOLD = 0.8f;
    private Interpreter interpreter;
    //Configurable
    private static int OUTPUT_SIZE = DEFAULT_OUTPUT_SIZE;
    private static int LANDMARK_BUFFER_SIZE = DEFAULT_LANDMARK_SIZE;
    private static String model_name = DEFAULT_MODEL;

    //Output
    private String predictedLabel = "";
    private float predictedProb = 0f;

    public static SignDetectClient getInstance(Context context){
        if(signDetectClient == null){
            signDetectClient = new SignDetectClient();
            signDetectClient.inputBuffer = new LandmarkBuffer(LANDMARK_BUFFER_SIZE);
            signDetectClient.initializeClient(context);
        }
        return signDetectClient;
    }

    private SignDetectClient initializeClient(Context context){

        outputBuffer = FloatBuffer.allocate(OUTPUT_SIZE);
        labelList = getLabelList(context);
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        Thread downloadModel = new Thread(new Runnable() {
            @Override
            public void run() {
                FirebaseModelDownloader.getInstance()
                        .getModel(model_name, DownloadType.LATEST_MODEL, conditions)
                        .addOnSuccessListener(new OnSuccessListener<CustomModel>() {
                            @Override
                            public void onSuccess(CustomModel model) {
                                // Download complete. Depending on your app, you could enable
                                // the ML feature, or switch from the local model to the remote
                                // model, etc.
                                modelFile = model.getFile();
                                interpreter = new Interpreter(modelFile);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
//                                System.out.println("Failed to download model");
                                Log.d("Sign Detect Client", "Failed to download model");
                            }
                        });
            }
        });
        downloadModel.run();

        return this;
    }

    private static ArrayList<String> getLabelList(Context context) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("labels.txt");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public void Detect(){
        if(inputBuffer.getMajorityLabel() == -1){
//            System.out.println("No Majority");
            return;
        }
        if (modelFile == null) {
//            System.out.println("Model is null");
            return;
        }
        outputBuffer.rewind();
        inputBuffer.resetNewInfoState();
//        inputBuffer.printBuffer();

        ByteBuffer genBuffer = inputBuffer.generateBuffer();
//        System.out.println("Random Line Check: "+genBuffer.get(15));

        interpreter.run(genBuffer, outputBuffer);


        float highest_prob = 0f;
        int predict_raw = -1;
        predictedLabel = "";
        predictedProb = 0f;
        for(int i=0; i<OUTPUT_SIZE;++i){
            float tempProb = outputBuffer.get(i);
            if (highest_prob < tempProb){
                highest_prob = tempProb;
                predict_raw = i;
            }
        }
        if (predict_raw != -1){
            predictedLabel = labelList.get(predict_raw);
            predictedProb = highest_prob;
        }

        if (predictedProb > PROB_THRESHOLD){
            DetectionBuffer.getInstance().addPrediction(predict_raw);
        }
//        System.out.println("Highest Prob "+predictedProb);
    }


    public static String getLabel(int index){
        return labelList.get(index);
    }

    public static int getLabelSize() {return labelList.size();}

    public void addHandLandmarks(HandLandmarkerResult handLandmarkerResult){
        inputBuffer.addHandBuffer(handLandmarkerResult);
        if(inputBuffer.hasNewInfo()) Detect();
    }

    public void addPoseLandmarks(PoseLandmarkerResult poseLandmarkerResult){
        inputBuffer.addPoseBuffer(poseLandmarkerResult);
        if(inputBuffer.hasNewInfo()) Detect();
    }

    // For Debugging
    public void printInputLandmarks(){
        inputBuffer.printBuffer();
    }

    @TestOnly
    public static SignDetectClient setTestMode(Context context, int testFile){
        OUTPUT_SIZE = SignDetectClient.TEST_OUTPUT_SIZE;
        LANDMARK_BUFFER_SIZE = SignDetectClient.TEST_LANDMARK_BUFFER_SIZE;
        model_name = SignDetectClient.TEST_MODEL;
        getInstance(context);
        signDetectClient.inputBuffer = LandmarkBuffer.getTestBuffer(TEST_LANDMARK_BUFFER_SIZE);
        signDetectClient.inputBuffer.fillTestData(context, testFile);
        return getInstance(context);
    }

    public String getPredictedLabel() {return predictedLabel;}
    public float getPredictedProb() {return predictedProb;}
}
