package com.handstalk.signdetect.sentenceCompletion;


import com.handstalk.signdetect.translate.SignDetectClient;

import java.util.ArrayList;
import java.util.Collections;

public class DetectionBuffer implements SentenceCompletionClient.SentenceCompletionCallback {
    private static DetectionBuffer detectionBuffer;
    private ArrayList<Integer> predictionList;
    private DetectionCallback callback;
    private boolean noHandsLock; // Prevent Keep Receiving no hands
    private static final int MAX_CAPACITY = 6;
    private static final int MAX_NO_HANDS = 6;
    private int pointer;
    private int noHandsCounter;

    public static DetectionBuffer getInstance(){
        if(detectionBuffer ==null){
            detectionBuffer = new DetectionBuffer();
        }
        return detectionBuffer;
    }

    public void subscribe(DetectionCallback detectionCallback){
        callback = detectionCallback;
    }

    public void unsubscribe(DetectionCallback detectionCallback){
        callback = null;
    }

    private DetectionBuffer(){
        predictionList = new ArrayList<>();
        pointer = 0;
        noHandsCounter = 0;
        SentenceCompletionClient.getInstance().subscribe(this);
    }

    public void addPrediction(int prediction){
        noHandsLock = false;
        if (predictionList.size()<MAX_CAPACITY){
            predictionList.add(prediction);

        }else{
            predictionList.set(pointer, prediction);
        }
        pointer+=1;

        if (pointer==MAX_CAPACITY){
            pointer = 0;
        }
        // Log.d("Pointer", String.valueOf(pointer));
        if (callback != null){
            if (Collections.frequency(predictionList, prediction) >= MAX_CAPACITY/2){
                callback.onWordConfirmed();
            }else{
                callback.onWordUnconfirmed();
            }
        }
    }

    private void addWord(){
        int predictionIdx = getMajorPrediction();
        if (predictionIdx == -1) return;
        String word = SignDetectClient.getLabel(predictionIdx);
        SentenceCompletionClient sentenceCompletionClient = SentenceCompletionClient.getInstance();
        sentenceCompletionClient.addWord(word);

        callback.onNewWordsAdded(word);
    }

    public void notifyNoHands(){
        if (!noHandsLock){
            noHandsCounter+=1;
            if(noHandsCounter==MAX_NO_HANDS){
                addWord();
                clearList();
                noHandsLock = true;
                noHandsCounter = 0;
            }
        }
    }

    private int getMajorPrediction(){
        int max_count = -1;
        int max_count_idx = -1;
        for (int i=0; i< SignDetectClient.getLabelSize(); ++i){
            int count = Collections.frequency(predictionList, i);
            if(count>max_count && count >= MAX_CAPACITY/2){
                max_count = count;
                max_count_idx = i;
            }
        }
        return max_count_idx;
    }

    public void clearList(){
        predictionList.clear();
        pointer=0;
    }

    private boolean isFilled(){
        return predictionList.size()==MAX_CAPACITY;
    }

    @Override
    public void onResultReceived(String result) {
        clearList();
    }

    public interface DetectionCallback {
        void onNewWordsAdded(String sentence);
        void onWordConfirmed();
        void onWordUnconfirmed();
    }
}