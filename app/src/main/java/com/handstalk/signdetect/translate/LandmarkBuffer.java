package com.handstalk.signdetect.translate;

import android.content.Context;
import android.util.Log;

import com.handstalk.signdetect.sentenceCompletion.DetectionBuffer;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import org.jetbrains.annotations.TestOnly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

// Ring Buffer For Landmark of 20 frames
public class LandmarkBuffer {
    private final int INPUT_SIZE;
    private final int BUFFER_SIZE;
    private final int ROW_SIZE;
    private final int LANDMARK_SIZE;
    private final int POSE_LANDMARK_SIZE;
    private final int HAND_LANDMARK_SIZE;
    // Default Values for buffer
    private static final int DEFAULT_POSE_LANDMARK_SIZE = 99;
    private static final int DEFAULT_HAND_LANDMARK_SIZE = 126*2;
    private static final int DEFAULT_ROW_SIZE = 20;

    private static final int TEST_POSE_LANDMARK_SIZE = 99;
    private static final int TEST_HAND_LANDMARK_SIZE = 126;
    private static final int TEST_ROW_SIZE = 20;

    private final FloatBuffer inputBuffer;
    private final FloatBuffer poseBuffer;
    private final FloatBuffer handBuffer;
    private int handLabelBuffer; // Buffer to store current hand label
    private final int HAND_LABEL_LEFT = 0;
    private final int HAND_LABEL_RIGHT = 1;
    private final int HAND_LABEL_TWO = 2;
    private int pointer = 0;
    private int capacity = 0;
    private NormalizedLandmark pivot = null;
    private Float poseScale = null;
    private boolean poseFilled = false;
    private boolean handFilled = false;
    private boolean hasNewInfo = false;
    private final float[] defaultHand;
    private final int[] handLabel;
    private int majorityLabel = -1;

    public LandmarkBuffer(int bufferSize, int rowSize, int handBufferSize, int poseBufferSize){
        ROW_SIZE = rowSize;
        LANDMARK_SIZE = poseBufferSize + handBufferSize;
        POSE_LANDMARK_SIZE = poseBufferSize;
        HAND_LANDMARK_SIZE = handBufferSize;

        BUFFER_SIZE = bufferSize;
        INPUT_SIZE = LANDMARK_SIZE * bufferSize;
        // System.out.println("Input Size: " + INPUT_SIZE);


        inputBuffer = FloatBuffer.allocate(INPUT_SIZE);
        poseBuffer = FloatBuffer.allocate(POSE_LANDMARK_SIZE);
        handBuffer = FloatBuffer.allocate(HAND_LANDMARK_SIZE);
        handLabel = new int[BUFFER_SIZE];
        defaultHand = initDefaultHand();
    }

    public LandmarkBuffer(int bufferSize){
        this(bufferSize, DEFAULT_ROW_SIZE, DEFAULT_HAND_LANDMARK_SIZE, DEFAULT_POSE_LANDMARK_SIZE);
    }

    public void Append(){
        //Only append when ready
        if(!(poseFilled && handFilled)) return;

        inputBuffer.position(pointer* LANDMARK_SIZE);
        for(int i = 0; i< POSE_LANDMARK_SIZE; ++i){
            inputBuffer.put(poseBuffer.get(i));
        }
        for(int i = 0; i< HAND_LANDMARK_SIZE; ++i){
            inputBuffer.put(handBuffer.get(i));
        }

        // Set Hand Label Buffer
        handLabel[pointer] = handLabelBuffer;

        //Reset two buffers
        poseBuffer.rewind();
        handBuffer.rewind();
        poseFilled = false;
        handFilled = false;

        //Handle Capacity
        pointer++;
        if(capacity<BUFFER_SIZE){
            capacity++;
        }
        if(pointer==BUFFER_SIZE){
            pointer = 0;
        }
        //Log.d("Landmark Buffer", String.valueOf(pointer));

        hasNewInfo = true;
    }

    public boolean hasNewInfo() { return hasNewInfo;}
    public void resetNewInfoState() {hasNewInfo = false;}

    private float distance(NormalizedLandmark a, NormalizedLandmark b){
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        float dz = a.z() - b.z();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void addPoseBuffer(PoseLandmarkerResult poseLandmarkerResult){
        poseBuffer.rewind();
        if(poseLandmarkerResult.landmarks().size() == 0) return;
        List<NormalizedLandmark> poseLandmark = poseLandmarkerResult.landmarks().get(0);
        pivot = poseLandmark.get(0);
        poseScale = distance(poseLandmark.get(11), poseLandmark.get(12));
        for(int i=0; i < poseLandmark.size(); ++i){
            poseBuffer.put((poseLandmark.get(i).x()-pivot.x())/ poseScale);
            poseBuffer.put((poseLandmark.get(i).y()-pivot.y())/ poseScale);
            poseBuffer.put((poseLandmark.get(i).z()-pivot.z())/ poseScale);
        }
        poseFilled = true;
        Append();
    }

    public void addHandBuffer(HandLandmarkerResult handLandmarkerResult){
        if (poseScale == null) return; // Wait for poseScale and pivot
        handBuffer.rewind();
        float[] tempLeft = defaultHand;
        boolean leftIsDefault = true;
        float[] tempRight = defaultHand;
        boolean rightIsDefault = true;
        for(int i=0; i< handLandmarkerResult.handedness().size(); ++i){
            if(handLandmarkerResult.handedness().get(i).get(0).categoryName().equals("Left")){
                tempLeft = addHand(handLandmarkerResult.landmarks().get(i));
                leftIsDefault = false;
                handLabelBuffer = HAND_LABEL_LEFT;
            }
            if(handLandmarkerResult.handedness().get(i).get(0).categoryName().equals("Right")){
                tempRight = addHand(handLandmarkerResult.landmarks().get(i));
                rightIsDefault = false;
                handLabelBuffer = HAND_LABEL_RIGHT;
            }
        }
        // Should not be empty for both hands
        if(leftIsDefault && rightIsDefault){
            DetectionBuffer.getInstance().notifyNoHands();
            return;
        }
        if(!leftIsDefault && !rightIsDefault) handLabelBuffer = 2;

        for(int i = 0; i< HAND_LANDMARK_SIZE /2; ++i){
            handBuffer.put(tempLeft[i]);
        }
        for(int i = 0; i< HAND_LANDMARK_SIZE /2; ++i){
            handBuffer.put(tempRight[i]);
        }
        handFilled = true;
        Append();
    }



    // Helper Function for addHandBuffer (For each hands)
    private float[] addHand(List<NormalizedLandmark> landmark){
        float[] result = new float[HAND_LANDMARK_SIZE /2];
        int index = 0;
        NormalizedLandmark handPivot = landmark.get(0);
        float handScale = distance(landmark.get(0), landmark.get(5));
        for(int i=0; i< landmark.size(); ++i){
            result[index] = (landmark.get(i).x()-pivot.x())/ poseScale;
            ++index;
            result[index] = (landmark.get(i).y()-pivot.y())/ poseScale;
            ++index;
            result[index] = (landmark.get(i).z()-pivot.z())/ poseScale;
            ++index;

            result[index] = (landmark.get(i).x()-handPivot.x())/ handScale;
            ++index;
            result[index] = (landmark.get(i).y()-handPivot.y())/ handScale;
            ++index;
            result[index] = (landmark.get(i).z()-handPivot.z())/ handScale;
            ++index;
        }
        return result;
    }

    // Generate Default Values for hand in case it is none
    final private float[] initDefaultHand(){
        float[] result = new float[HAND_LANDMARK_SIZE /2];
        int index = 0;
        for(int i = 0; i< HAND_LANDMARK_SIZE /6; ++i){
            result[index] = 0f;
            ++index;
            result[index] = 0f;
            ++index;
            result[index] = 0f;
            ++index;
        }
        return result;
    }

    // For Debugging
    public void printBuffer(){
        for(int i = 0; i< LANDMARK_SIZE *capacity; ++i){
            Log.d("Landmark Buffer", String.valueOf(inputBuffer.get(i)));
        }
    }

    private boolean isFilled(){
        return capacity==BUFFER_SIZE;
    }


    public int getMajorityLabel() {
        // Exit if not filled
        if (!isFilled()) return -1;
        // System.out.println("Buffer is filled");
        int leftCount = 0;
        int rightCount = 0;
        int twoCount = 0;
        //Assumption: isFilled is checked before running
        for (int i = 0; i < BUFFER_SIZE; ++i ){
            switch (handLabel[i]){
                case HAND_LABEL_LEFT:
                    ++leftCount;
                    break;
                case HAND_LABEL_RIGHT:
                    ++rightCount;
                    break;
                case HAND_LABEL_TWO:
                    ++twoCount;
                    break;
            }
        }

        int highestIndex = -1;
        int highestCount = -1;
        int[] countArray = {leftCount, rightCount, twoCount};
        for(int i=0; i<3; ++i){
            if(countArray[i]>highestCount){
                highestCount = countArray[i];
                highestIndex = i;
            }
        }
        if (highestCount < ROW_SIZE && highestIndex != HAND_LABEL_TWO){
            // Not Enough Left/ Right samples
            majorityLabel = -1;
            return -1;
        }else{
            majorityLabel = highestIndex;
            return highestIndex;
        }
    }

    // Helper function for generateBuffer
    private float[] getInputBuffer(int row){
        int buffer_pos = row * LANDMARK_SIZE;
//        Log.d("row:", String.valueOf(row));
        float[] temp = new float[LANDMARK_SIZE];
        for (int i = 0; i< LANDMARK_SIZE; ++i){
            temp[i] = inputBuffer.get(buffer_pos+i);
            // System.out.println("Float value "+temp[i]);
        }
        return temp;
    }

    public ByteBuffer generateBuffer(){
        ByteBuffer result = ByteBuffer.allocateDirect(INPUT_SIZE*4).order(ByteOrder.nativeOrder());
        //Assumption: getMajorityLabel is checked before running
        //If the sign does not occur any hand label flickering, there could be some lagging issues, but shouldn't affect the result
        int row_index=0;
        for (int i = pointer+1; i < BUFFER_SIZE; ++i) {

            // Skip Rows with minor labels (left/right hands only)
            if (handLabel[i] != majorityLabel && majorityLabel != HAND_LABEL_TWO) continue;

            ++row_index;
            if (row_index >= ROW_SIZE) break;

            float[] toPut = getInputBuffer(i);
            for (int k=0; k< toPut.length; ++k){
                result.putFloat(toPut[k]);
            }

        }
        for (int i = 0; i < pointer; ++i) {

            // Skip Rows with minor labels (left/right hands only)
            if (handLabel[i] != majorityLabel && majorityLabel != HAND_LABEL_TWO) continue;

            ++row_index;
            if (row_index >= ROW_SIZE) break;

            float[] toPut = getInputBuffer(i);
            for (int k=0; k< toPut.length; ++k){
                result.putFloat(toPut[k]);
            }

        }
        return result;
    }

    @TestOnly
    public static LandmarkBuffer getTestBuffer(int bufferSize){
        return new LandmarkBuffer(bufferSize, TEST_ROW_SIZE, TEST_HAND_LANDMARK_SIZE, TEST_POSE_LANDMARK_SIZE);
    }

    @TestOnly
    public void fillTestData(Context context, int testFile){
        try {
            InputStream is = context.getAssets().open("test_point_"+testFile+".txt");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                inputBuffer.put(Float.parseFloat(line));
            }
            capacity = BUFFER_SIZE;
            pointer = 0;
            br.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
