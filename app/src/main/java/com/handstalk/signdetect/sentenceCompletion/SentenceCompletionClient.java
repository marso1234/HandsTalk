package com.handstalk.signdetect.sentenceCompletion;

import android.util.Log;

import com.handstalk.signdetect.network.OpenaiRetrofitClient;
import com.handstalk.signdetect.network.RetrofitService;
import com.handstalk.signdetect.utilities.Constants;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SentenceCompletionClient {
    private static final String promptSystem = "This system is designed to assist users by completing fragmented sentences formed from a list of words.\n" +

            "**Input:** The system expects a list of words as input.\n" +

            "**Output:**\n" +
            "  * **Result (String):** The completed sentence based on the provided word list.\n" +
            "  * **Confidence (float):** A score between 0.0 (uncertain) and 1.0 (highly confident) indicating the system's trust in the generated sentence.\n" +

            "**Functionality:**\n" +
            "  * The system prioritizes generating grammatically correct and coherent sentences based on the input words.\n" +
            "  * When the input words are limited, the system attempts to create a reasonable sentence.\n" +

            "**Important Notes:**\n" +
            "  * The system avoids responding in the first person (e.g., 'I', 'me') or engaging in conversations.\n" +
            "  * The system does not answer user questions directly. Its sole purpose is sentence completion based on the provided word list.\n" +

            "**Confidence Score:**\n" +
            "  * The confidence score is determined by factors like the number of input words, their semantic relationships, and the system's internal assessment of the generated sentence.\n" +

            "**Examples:**\n" +
            "  * Input: [the, dog, chased, cat]\n" +
            "    * Output: {\"Result\": \"The dog chased the cat.\", \"Confidence\": 0.9}\n" +
            "  * Input: [went, store, milk]\n" +
            "    * Output: {\"Result\": \"I went to the store for milk.\", \"Confidence\": 0.7}";

    private static String promptUser(String joinedString){
        return "Input:"+joinedString+"\nOutput:";
    }
    private static SentenceCompletionClient sentenceCompletionClient;
    private final ArrayList<String> words = new ArrayList<>();
    private ArrayList<SentenceCompletionCallback> callbackList;
    public static final String toggleSymbol = "<Toggle>";
    public static final String spaceSymbol = "<Space>";
    public static final String toggleQuestion = "<?>";
    private static final String deleteSymbol = "<-";
    private static final String statusTranslate = "Translating...";
    private String result = "";
    private int minDetectionLength = 3;
    private boolean toggle=false;
    private final StringBuilder lettersBuilder = new StringBuilder();
    // private int maxDetectionLength = 7;
    private boolean lastInputIsLetter = false;
    private boolean lastInputIsDelete = false;
    private boolean isTranslating = false;
    private boolean isQuestion = false;

    public static SentenceCompletionClient getInstance(){
        if(sentenceCompletionClient==null){
            sentenceCompletionClient = new SentenceCompletionClient();
        }
        return sentenceCompletionClient;
    }

    public void resetValues(){
        toggle=false;
        isTranslating=false;
        lastInputIsDelete=false;
        lastInputIsLetter=false;
        isQuestion = false;
        words.clear();
        lettersBuilder.setLength(0);
    }

    public boolean getToggle(){
        return toggle;
    }

    private SentenceCompletionClient(){
        callbackList = new ArrayList<>();
    }

    public boolean getLastInputIsDelete() {return lastInputIsDelete;}

    public void subscribe(SentenceCompletionCallback callback){
        callbackList.add(callback);
    }

    public void unsubscribe(SentenceCompletionCallback callback){
        callbackList.remove(callback);
    }

    public void setResult(String r) {
        sentenceCompletionClient.result = r;
    }


    public void addWord(String word) {
//        if (words.size() + 1 >= maxDetectionLength){
//            words.remove(0);
//        }
//        // Prevent Duplication of words
        if(word.equals(toggleSymbol)){
            if(toggle){
                run();
                Log.d("Toggle", String.valueOf(toggle));
            }else{
                toggle=true;
            }
            return;
        }

        if(!toggle) return;
        if(word.equals(toggleQuestion)){
            isQuestion = true;
            run();
            return;
        }
        if(word.equals(deleteSymbol)) {
            if (lastInputIsLetter) {
                int letterLength = lettersBuilder.length();
                if (letterLength > 0) {
                    lettersBuilder.setLength(letterLength - 1);
                }else{
                    lastInputIsLetter = false;
                }
            }
            if (!lastInputIsLetter) {
                int lastWordIndex = words.size();
                if (lastWordIndex > 0) {
                    words.remove(lastWordIndex - 1);
                }
            }
            lastInputIsDelete = true;
            return;
        }
        if(word.equals(spaceSymbol)) {
            if (lastInputIsLetter) {
                combineLetters();
            }
            return;
        }

        if (!words.isEmpty()){
            if(words.get(words.size()-1).equals(word)) return;
        }
        if(word.length()==1) {
            lettersBuilder.append(word);
            lastInputIsLetter = true;
            return;
        }
        if(lastInputIsLetter){
            combineLetters();
        }

        words.add(word);
    }

    private void combineLetters() {
        String formFromLetter = lettersBuilder.toString();
        lettersBuilder.setLength(0);
        words.add(formFromLetter);
        lastInputIsLetter = false;
    }

    public String getRaw(){
        StringBuilder sbString = new StringBuilder("");
        if(isTranslating){
            return statusTranslate;
        }
        for(String w : words){
            //append ArrayList element followed by space bar
            sbString.append(w).append(" ");
        }
        if (lastInputIsLetter) {
            String formFromLetter = lettersBuilder.toString();
            sbString.append(formFromLetter);
        }

        return sbString.toString();
    }

    public void resetBuffer(){
        lettersBuilder.setLength(0);
        words.clear();
        result = "";
    }

    @TestOnly
    public void reset(){
        result = "";
        resetLock();
        resetBuffer();
    }

    @TestOnly
    public void resetLock(){
        sentenceCompletionClient.isTranslating = false;
    }

    public String getResult(){
        if (result.isEmpty()) return result;
        try{
            JSONObject resultJson = new JSONObject(result);
            String predict_result = resultJson.get("Result").toString();
            String confidence = resultJson.get("Confidence").toString();
            return predict_result + " (" + confidence+")";
        }catch (JSONException e){
            e.printStackTrace();
            System.out.println(result);
            return result;
        }

    }

    public void run(){
        if (isTranslating) {
            return;
        }
        // Also add the last word if it is letters
        if (lastInputIsLetter) {
            combineLetters();
        }

//        // Toggle Off
//        if (words.size() == 0 && lettersBuilder.length()==0){
//            resetValues();
//        }

        // Toggle off if too less input
        if (words.size() < minDetectionLength) {
            resetValues();
            return;
        }

        isTranslating = true;
        Thread thread = new Thread(() -> {
            StringBuilder sbString = new StringBuilder("");

            sbString.append("[");

            //iterate through ArrayList
            for(String w : words){
                //append ArrayList element followed by space bar
                sbString.append(w).append(", ");
            }
            if(isQuestion){
                sbString.append("?").append(", ");
            }
            sbString.append("]");
            String joinedString = sbString.toString();
            String body = createBody(joinedString);
            OpenaiRetrofitClient.getClient().create(RetrofitService.class).sentenceCompletion(
                    Constants.getOpenAICompletionHeaders(), body
            ).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if(response.isSuccessful()){
                        String result = response.body();
                        JSONObject resultJson = null;
                        try {
                            resultJson = new JSONObject(result);
                            JSONObject choiceArr = resultJson.getJSONArray("choices").getJSONObject(0);
                            JSONObject messageObj = choiceArr.getJSONObject("message");
                            String message = messageObj.get("content").toString();
                            SentenceCompletionClient.getInstance().setResult(message);
                            isTranslating = false;
                            for (SentenceCompletionCallback callback:
                                    callbackList) {
                                callback.onResultReceived(getResult());
                            }
                            words.clear();
                            isQuestion=false;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else{
                        System.out.println(response.message());
                    }
                    isTranslating = false;
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    System.out.println("Completion Failure");
                    isTranslating = false;
                }
            });
        });
        thread.start();
    }

    private String createBody(String joinedString){
        /*
            Structure:
            messages: [{
                    role,content
                    },{role,content
                }]
         */
        try {
            JSONObject message1 = new JSONObject();
            message1.put("role", "system");
            message1.put("content", promptSystem);

            JSONObject message2 = new JSONObject();
            message2.put("role", "user");
            message2.put("content", promptUser(joinedString));
//            System.out.println(promptUser(joinedString));
            JSONArray messages = new JSONArray();
            messages.put(message1);
            messages.put(message2);

            JSONObject result = new JSONObject();
            result.put("messages", messages);

            return result.toString();
        }catch (Exception exception){
            exception.printStackTrace();
            return "";
        }
    }

    public boolean isTranslating() {
        return isTranslating;
    }

    public interface SentenceCompletionCallback{
        public void onResultReceived(String result);
    }
}