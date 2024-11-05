package com.handstalk.signdetect.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.databinding.ActivityVideoCallBinding;
import com.handstalk.signdetect.firebase.MessagingService;
import com.handstalk.signdetect.model.RemoteMessageBuilder;
import com.handstalk.signdetect.sentenceCompletion.DetectionBuffer;
import com.handstalk.signdetect.sentenceCompletion.SentenceCompletionClient;
import com.handstalk.signdetect.translate.HolisticTrackClient;
import com.handstalk.signdetect.translate.ResultBundle;
import com.handstalk.signdetect.translate.SignDetectClient;
import com.handstalk.signdetect.utilities.Constants;
import com.handstalk.signdetect.videocall.WebRTCClient;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.permissionx.guolindev.PermissionX;

import org.webrtc.MediaStream;

import java.util.ArrayList;

public class VideoCallActivity extends AppCompatActivity implements HolisticTrackClient.LandmarkerListener,
        SentenceCompletionClient.SentenceCompletionCallback, DetectionBuffer.DetectionCallback {
    private ActivityVideoCallBinding binding;
    private WebRTCClient webRTCClient;
    private HolisticTrackClient holisticTrackClient;
    private SignDetectClient signDetectClient;
    private static MediaStream mediaStream;
    private boolean audioEnable = true;
    private boolean videoEnable = true;
    private boolean speechRecognitionEnable = false;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);



    public static VideoCallActivity videoCallActivity;
    private String opponentUserId = "";
    private String opponentToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        opponentUserId = getIntent().getStringExtra("opponentUserId");
        opponentToken = getIntent().getStringExtra("opponentToken");

        initViewBinding();

        initSignDetection();

        webRTCClient = WebRTCClient.getInstance();
        webRTCClient.initLocalSurfaceView(binding.localView);
        webRTCClient.initRemoteSurfaceView(binding.remoteView);
        videoCallActivity = this;

        initSpeechRecognizer();
        initHolistic();

        if(mediaStream!=null) {
            addStream(mediaStream);
            webRTCClient.startVideoCapture(binding.localView);
        }else{
            Log.d("Error","Stream is Null");
        }

        binding.micButton.setOnLongClickListener((v)->{
            toggleMic();
            return true;
        });

        binding.micButton.setOnClickListener((v)->{
            toggleSpeechRecognition();
        });

        binding.videoButton.setOnClickListener((v)->{
            toggleVideo();
        });

        binding.endCallButton.setOnClickListener((v)->{
            endCall();
        });

        binding.switchCameraButton.setOnClickListener((v)->{
            switchCamera();
        });

        binding.translateButton.setOnClickListener((v)->{
            toggleTranslation();
        });
    }

    private void toggleSpeechRecognition() {
        if(!audioEnable) return;
        if(speechRecognitionEnable){
            speechRecognitionEnable = false;
            binding.setStartTranslating(false);
            speechRecognizer.stopListening();
            binding.micButton.setImageResource(R.drawable.ic_mic);
        }else{
            // Stop Translation Recognition To prevent conflict
            Toast.makeText(getApplicationContext(), "Speech Recognition On", Toast.LENGTH_SHORT).show();
            toggleTranslationOff();
            binding.setStartTranslating(true);
            speechRecognitionEnable = true;
            speechRecognizer.startListening(speechRecognizerIntent);
            binding.micButton.setImageResource(R.drawable.ic_speech_recognize);
        }
    }

    private void initSignDetection() {
        signDetectClient = SignDetectClient.getInstance(this);
        DetectionBuffer.getInstance().subscribe(this);
        SentenceCompletionClient.getInstance().subscribe(this);
        SentenceCompletionClient.getInstance().resetValues();
    }

    private void initViewBinding() {
        binding.setStartTranslation(false);
        binding.setOpponentTranslation(false);
        binding.setStartTranslating(false);
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                "en-US");
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                binding.micButton.setImageResource(R.drawable.ic_mic);
                speechRecognitionEnable = false;
            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onResults(Bundle results) {
                //getting all the matches
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                //displaying the first match
                if (matches != null){
                    onResultReceived(matches.get(0));

                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
//                //getting all the matches
//                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//                Log.d("Speech","Speech Running");
//                if (matches != null){
//                    onResultReceived(matches.get(0));
//                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    private void toggleVideo() {
        videoEnable = !videoEnable;
        if(!videoEnable) {
            binding.videoButton.setImageResource(R.drawable.ic_videocam_off);
            toggleTranslationOff();
            //Toast.makeText(getApplicationContext(), "Stop Video", Toast.LENGTH_SHORT).show();
        }
        else {
            binding.videoButton.setImageResource(R.drawable.ic_facetime);
            //Toast.makeText(getApplicationContext(), "Start Video", Toast.LENGTH_SHORT).show();
        }
        webRTCClient.toggleVideo(videoEnable);
    }

    private void initHolistic() {
        holisticTrackClient = HolisticTrackClient.getInstance();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                holisticTrackClient.initializeClient(getApplicationContext(), VideoCallActivity.this);
                binding.setIsLoading(false);
            }
        });
        thread.start();
    }

    private void toggleTranslation() {
        if (binding != null){
            if (!binding.getStartTranslation()){
                toggleTranslationOn();
            }else{
                // Put Toast here to prevent calling when doing speech recognition
                //Toast.makeText(getApplicationContext(), "Stop Translation", Toast.LENGTH_SHORT).show();
                toggleTranslationOff();
            }
        }
    }

    private void toggleTranslationOff() {
        binding.translateButton.setImageResource(R.drawable.ic_translate_off);
        binding.setStartTranslation(false);
        binding.setStartTranslating(false);
        SentenceCompletionClient.getInstance().resetValues();
        DetectionBuffer.getInstance().clearList();
        binding.mySentence.setText("");
    }

    private void toggleTranslationOn() {
        binding.translateButton.setImageResource(R.drawable.ic_translate);
        //Toast.makeText(getApplicationContext(), "Start Translation", Toast.LENGTH_SHORT).show();
        binding.setStartTranslation(true);
    }

    private void switchCamera() {
        webRTCClient.switchCamera();
    }

    private void endCall() {
        webRTCClient.closeConnection();
        Thread rejectCall = new Thread(new Runnable() {
            @Override
            public void run() {
                RemoteMessageBuilder.getInstance().setOpponentToken(opponentToken);
                String body = RemoteMessageBuilder.endCallResponse();
                MessagingService.sendRemoteMessage(getApplicationContext(),
                        body,
                        Constants.REMOTE_MSG_INVITATION_REJECTED,
                        new MessagingService.remoteMessageCallback() {
                            @Override
                            public void OnSuccessCallback(String type) {
                                Log.d("Message","End Call Sent");
                                finish();
                            }

                            @Override
                            public void OnFailureCallback(String msg) {
                                Log.d("Message", msg);
                                finish();
                            }
                        });
            }
        });
        rejectCall.start();
    }

    private void toggleMic() {
        audioEnable = !audioEnable;
        if(!audioEnable) {
            speechRecognizer.stopListening(); // Make sure speech recognition is end
            binding.micButton.setImageResource(R.drawable.ic_mic_off);
            Toast.makeText(getApplicationContext(), "Mute Mic", Toast.LENGTH_SHORT).show();
        }
        else {
            binding.micButton.setImageResource(R.drawable.ic_mic);
            Toast.makeText(getApplicationContext(), "Start Mic", Toast.LENGTH_SHORT).show();
        }
        webRTCClient.toggleAudio(audioEnable);
    }

    private void addStream(MediaStream mediaStream){
        mediaStream.videoTracks.get(0).addSink(binding.remoteView);
    }

    public static void setMediaStream(MediaStream mediaStream){
        VideoCallActivity.mediaStream = mediaStream;
    }

    public static void initVideoStream(Context context, PermissionRetrieveCallback callback){
        PermissionX.init((FragmentActivity) context)
                .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .request((allGranted, grantedList, deniedList)->{
                    if (allGranted) {
                        Log.d("Log:","Permission Granted");
                    } else {
                        callback.OnPermissionRetrieveFailure();
                    }
                });
    }

    @Override
    public void onResults(ResultBundle resultBundle) {
        if(binding.getStartTranslation()){
            this.runOnUiThread(()->{
                if (binding != null && binding.getStartTranslation()) {
                    if (resultBundle.getTypeLandmark().equals(ResultBundle.TYPE_LANDMARK.hand)){
                        binding.localOverlay.setHandResults(
                                (HandLandmarkerResult) resultBundle.getResults(),
                                resultBundle.getInputImageHeight(),
                                resultBundle.getInputImageWidth());
                        signDetectClient.addHandLandmarks((HandLandmarkerResult) resultBundle.getResults());
                    } else if(resultBundle.getTypeLandmark().equals(ResultBundle.TYPE_LANDMARK.pose)){
                        binding.localOverlay.setPoseResult(
                                (PoseLandmarkerResult) resultBundle.getResults(),
                                resultBundle.getInputImageHeight(),
                                resultBundle.getInputImageWidth());
                        signDetectClient.addPoseLandmarks((PoseLandmarkerResult) resultBundle.getResults());
                    }

                    // Force a redraw
                    binding.localOverlay.invalidate();
                    setLabel(signDetectClient.getPredictedLabel(), signDetectClient.getPredictedProb());
                }
            });
        }

    }

    public static String getOpponentId(){
        if(videoCallActivity != null){
            return videoCallActivity.opponentUserId;
        }else{
            return "";
        }
    }

    public void setOpponentMessage(String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.setOpponentTranslation(true);
                binding.opponentTranslate.setText(message);
            }
        });

    }

    private void setLabel(String label, float prediction){
        if(prediction!=0f){
            if(SentenceCompletionClient.getInstance().getToggle() || label.equals(SentenceCompletionClient.toggleSymbol)){
                binding.labelPredict.setText(label);
                String displayTxt = String.valueOf(Math.round(prediction*10000)/100)+"%";
                binding.probPredict.setText(displayTxt);
            }else{
                binding.labelPredict.setText("-");
                binding.probPredict.setText("");
                binding.labelContainer.setBackground(getResources().getDrawable(R.color.colorTransparentBox));
            }
        }
    }

    @Override
    public void onDestroy() {
        opponentUserId = "";
        videoCallActivity = null;
        speechRecognizer.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        return;
    }

    @Override
    public void onError(String error) {
        this.runOnUiThread(()->{
            {
                binding.localOverlay.clear();
            }
        });
    }

    @Override
    public void onNewWordsAdded(String sentence) {
        if(sentence.equals(SentenceCompletionClient.toggleSymbol) && !SentenceCompletionClient.getInstance().isTranslating()) {
            if(SentenceCompletionClient.getInstance().getToggle()){
                if(!SentenceCompletionClient.getInstance().getLastInputIsDelete()){
                    Toast.makeText(getApplicationContext(), "Start Input", Toast.LENGTH_SHORT).show();
                }
                if(binding!=null) {
                    binding.setStartTranslating(true);
                    binding.mySentence.setText("");
                }
            }else{
                Toast.makeText(getApplicationContext(), "Stop Input", Toast.LENGTH_SHORT).show();
                if(binding!=null) {
                    binding.setStartTranslating(false);
                    binding.mySentence.setText("");
                }
            }
            return;
        }
        sentence = SentenceCompletionClient.getInstance().getRaw();
        if(binding != null){
            binding.setStartTranslating(true);
            binding.mySentence.setText(sentence);
            videoMessage(sentence).start();
        }

    }

    @Override
    public void onWordConfirmed() {
        if(binding != null){
            if(SentenceCompletionClient.getInstance().getToggle() || binding.labelPredict.getText().toString().equals(SentenceCompletionClient.toggleSymbol)){
                binding.labelContainer.setBackground(getResources().getDrawable(R.color.colorTransparentYellow));
            }
        }
    }

    @Override
    public void onWordUnconfirmed() {
        if(binding != null){
            if(SentenceCompletionClient.getInstance().getToggle() || binding.labelPredict.getText().toString().equals(SentenceCompletionClient.toggleSymbol)){
                binding.labelContainer.setBackground(getResources().getDrawable(R.color.colorTransparentBox));
            }
        }
    }

    @Override
    public void onResultReceived(String result) {
        if(binding != null){
            binding.mySentence.setText(result);
            videoMessage(result).start();
        }
    }

    public interface PermissionRetrieveCallback{
        void OnPermissionRetrieveFailure();
    }

    Thread videoMessage(String sentence){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                RemoteMessageBuilder.getInstance().setOpponentToken(opponentToken);
                String body = RemoteMessageBuilder.VideoMessage(sentence);
                MessagingService.sendRemoteMessage(getApplicationContext(),
                        body,
                        Constants.REMOTE_MSG_VIDEO_MESSAGE,
                        new MessagingService.remoteMessageCallback() {
                            @Override
                            public void OnSuccessCallback(String type) {
                                Log.d("Message","Message Sent");
                            }

                            @Override
                            public void OnFailureCallback(String msg) {
                                Log.d("Message", msg);
                            }
                        });
            }
        });
    }
}
