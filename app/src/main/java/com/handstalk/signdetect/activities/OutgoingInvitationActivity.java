package com.handstalk.signdetect.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.databinding.ActivityOutgoingInvitationBinding;
import com.handstalk.signdetect.firebase.MessagingService;
import com.handstalk.signdetect.model.RemoteMessageBuilder;
import com.handstalk.signdetect.model.User;
import com.handstalk.signdetect.utilities.AndroidUtil;
import com.handstalk.signdetect.utilities.Constants;
import com.handstalk.signdetect.utilities.FirebaseUtil;
import com.handstalk.signdetect.utilities.MyLogging;
import com.handstalk.signdetect.videocall.MyPeerConnectionObserver;
import com.handstalk.signdetect.videocall.WebRTCClient;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;

public class OutgoingInvitationActivity extends AppCompatActivity implements MyLogging, MessagingService.remoteMessageCallback {

    private ActivityOutgoingInvitationBinding binding;
    private final String TAG = "Outgoing Invitation";
    private WebRTCClient webRTCClient;
    private boolean hasClickedButton = false;
    private CountDownTimer timeoutTimer;
    private TextView waitingText;
    private String opponentUserId;
    private String opponentToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOutgoingInvitationBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        String meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        User receiverUser = (User) getIntent().getSerializableExtra("User");

        opponentUserId = receiverUser.getUserId();
        opponentToken = receiverUser.getFcm_token();

        FirebaseUtil.getUserId(opponentUserId).get().addOnCompleteListener(task -> {
            String receiverAuthId = task.getResult().getString("auth_id");
            //set user image into the recycle view
            FirebaseUtil.getOtherProfilePicStorageRef(receiverAuthId).getDownloadUrl()
                    .addOnCompleteListener(t -> {
                        if(t.isSuccessful()){
                            Uri uri  = t.getResult();
                            AndroidUtil.setProfilePic(this,uri,binding.iconImage);
                        }
                    });
        });

        // Check Internet Connection
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(!(cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected())){
            popConnectionErrorDialog();
        }

        binding.textUsername.setText(receiverUser.getUsername());
        waitingText = binding.waitingText;
        waitingText.setText(getResources().getString(R.string.send_invitation));
        waitingText.setAnimation(AnimationUtils.loadAnimation(this, R.anim.pluse));

        RemoteMessageBuilder remoteMessageBuilder = RemoteMessageBuilder.getInstance();
        remoteMessageBuilder.setContext(getApplicationContext());
        remoteMessageBuilder.setOpponentToken(opponentToken);

        ImageView stopInvitation = binding.imageStopInvitation;
        stopInvitation.setOnClickListener(v -> {
            if(!hasClickedButton){
                cancelInvitation();
                hasClickedButton = true;
            }
        });

        //Fetch Info from intent extra
        if(meetingType != null){
            initiateMeeting();
        }
    }

    // Only when activity is visible to user
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver, new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE));
    }

    // Only when activity is visible to user
    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver);
    }

    private void init(){

        // Init Client
        webRTCClient = new WebRTCClient();
        webRTCClient.initClient(getApplicationContext(), new MyPeerConnectionObserver(){
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Log.d("Log:","On Ice Candidate");
                webRTCClient.addIceCandidate(iceCandidate);
                String body = RemoteMessageBuilder.iceCandidate(
                        iceCandidate.sdpMid,
                        iceCandidate.sdpMLineIndex,
                        iceCandidate.sdp);
                MessagingService.sendRemoteMessage(
                        getApplicationContext(),
                        body,
                        Constants.REMOTE_MSG_ICE_CANDIDATE,
                        (MessagingService.remoteMessageCallback) OutgoingInvitationActivity.this);
            }

            @Override
            public void onAddStream(MediaStream mediaStream){
                super.onAddStream(mediaStream);
                Log.d("Log:","On Add Stream");
                VideoCallActivity.setMediaStream(mediaStream);
            }

        });
    }

    private void initiateMeeting(){
        // Permission Checking
        VideoCallActivity.initVideoStream(this, ()->{
            Logging("Please Accept All Permission","Please Accept All Permission");
            finish();
        });
        Thread initiateMeeting = new Thread(new Runnable() {
            @Override
            public void run() {
                //Start Init Only when accept call
                init();

                //Lock Init before proceed
                int counter = 0;
                while(!webRTCClient.isInitialized()||counter<30){
                    if(webRTCClient.isInitialized()) break;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.d("Log:","Error when sleeping");
                        e.printStackTrace();
                        return;
                    }
                    counter++;
                }
                if(!webRTCClient.isInitialized()){
                    Logging("WebRTC Client Initialization Timeout","Error");
                }

                // Start Sending Offer to Peer
                webRTCClient.createOffer((sd)->{
                    SessionDescription sessionDescription = (SessionDescription) sd;
                    String body = RemoteMessageBuilder.invitationOffer(
                            Constants.REMOTE_MSG_MEETING_TYPE,
                            sessionDescription.description);
                    MessagingService.sendRemoteMessage(getApplicationContext(), body, Constants.REMOTE_MSG_INVITATION, OutgoingInvitationActivity.this);
                });
            }
        });
        initiateMeeting.start();
    }

    private void cancelInvitation(){
        if (timeoutTimer != null){
            timeoutTimer.cancel(); // Prevent Logging Multiple Times
        }
        Logging("Invitation Cancelled","Invitation Cancelled");
        Thread cancelInvitation = new Thread(new Runnable() {
            @Override
            public void run() {
                String body = RemoteMessageBuilder.invitationResponse(Constants.REMOTE_MSG_INVITATION_CANCELLED, null);
                MessagingService.sendRemoteMessage(getApplicationContext(), body, Constants.REMOTE_MSG_INVITATION_CANCELLED, OutgoingInvitationActivity.this);
            }
        });
        cancelInvitation.start();
        finish();
    }

    @Override
    public void Logging(String debugMsg, String userMsg) {
        Log.d(TAG, debugMsg);

        //Skip User Message If Empty
        if(userMsg.isEmpty()) return;
        Toast.makeText(getApplicationContext(), userMsg, Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if(type != null){
                if(type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
                    waitingText.setText(getResources().getString(R.string.connecting));
                    String remoteSessionDescription = intent.getStringExtra(Constants.REMOTE_MSG_SESSION_DESCRIPTION);
                    webRTCClient.setRemoteSessionDescription(SessionDescription.Type.ANSWER, remoteSessionDescription,
                            (sd)->{
                                Log.d(TAG,"Remote Description Set");
                                startVideoCall();
                            });

                }else if(type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                    timeoutTimer.cancel(); // Prevent Logging Multiple Times
                    Logging("Invitation Rejected", "Invitation Rejected");
                    finish();
                }
            }
        }
    };

    // Set Remote sessionDescription when accept Call and Start VideoCallActivity
    private void startVideoCall(){
        timeoutTimer.cancel();
        webRTCClient.handleIceCandidate();
        webRTCClient.state();
        Intent intent = new Intent(OutgoingInvitationActivity.this, VideoCallActivity.class);
        intent.putExtra("opponentUserId",opponentUserId);
        intent.putExtra("opponentToken",opponentToken);
        startActivity(intent);
        finish();
    }


    @Override
    public void OnSuccessCallback(String type) {
        if(type.equals(Constants.REMOTE_MSG_INVITATION)){
            waitingText.setText(getResources().getString(R.string.waiting_for_response));
            timeoutTimer = startTimeoutTimer();
        }else if(type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)){
            // finish();
        }else if(type.equals(Constants.REMOTE_MSG_ICE_CANDIDATE)){
            //Ice Candidate
            Logging("Ice Candidate Sent","");
        }
    }

    @Override
    public void OnFailureCallback(String msg) {
        Logging(msg,"");
    }

    private void popConnectionErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connection Error")
                .setMessage("Please Check Your Connection")
                .setPositiveButton("OK", (dialog, which) -> {
                    finish();
                }).show();
    }

    private CountDownTimer startTimeoutTimer(){
        return new CountDownTimer(30000, 1000){
            public void onTick(long millisUntilDone){
                Log.d(TAG, "Counting Down...");
            }

            public void onFinish() {
                Logging("Invitation Timeout", "Timeout");
                cancelInvitation();
                finish();
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        return;
    }
}
