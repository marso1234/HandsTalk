package com.handstalk.signdetect.activities;

import androidx.annotation.NonNull;
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
import android.widget.TextView;
import android.widget.Toast;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.databinding.ActivityIncomingInvitationBinding;
import com.handstalk.signdetect.firebase.FriendsClient;
import com.handstalk.signdetect.firebase.MessagingService;
import com.handstalk.signdetect.model.RemoteMessageBuilder;
import com.handstalk.signdetect.ui.facetime.FacetimeFragment;
import com.handstalk.signdetect.ui.message.MessageFragment;
import com.handstalk.signdetect.utilities.AndroidUtil;
import com.handstalk.signdetect.utilities.Constants;
import com.handstalk.signdetect.utilities.FirebaseUtil;
import com.handstalk.signdetect.utilities.MyLogging;
import com.handstalk.signdetect.videocall.MyPeerConnectionObserver;
import com.handstalk.signdetect.videocall.WebRTCClient;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;

public class IncomingInvitationActivity extends AppCompatActivity implements MyLogging, MessagingService.remoteMessageCallback {

    private ActivityIncomingInvitationBinding binding;
    private final String TAG = "Incoming Invitation";
    private WebRTCClient webRTCClient;
    private boolean hasClickedButton = false;
    private CountDownTimer timeoutTimer;
    private TextView waitingText;
    private String opponentToken;
    private String opponentUserId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityIncomingInvitationBinding.inflate(getLayoutInflater());

        View view = binding.getRoot();
        setContentView(view);

        String meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if(meetingType != null){
            if(meetingType.equals("video")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_facetime);
            }
        }

        String username = getIntent().getStringExtra(Constants.KEY_USERNAME);
        if(username != null){
            binding.textUsername.setText(username);
        }

        timeoutTimer = startTimeoutTimer();

        // Check Internet Connection
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(!(cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected())){
            popConnectionErrorDialog();
        }

        waitingText = binding.waitingText;
        waitingText.setAnimation(AnimationUtils.loadAnimation(this, R.anim.pluse));

        opponentToken = getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN);
        opponentUserId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        Log.d("opponent: userId", opponentUserId);

        FirebaseUtil.getUserId(opponentUserId).get().addOnCompleteListener(task -> {
            String receiverAuthId = task.getResult().getString("auth_id");
            if (receiverAuthId != null){
                Log.d("opponent: auth", receiverAuthId);
                //set user image into the recycle view
                FirebaseUtil.getOtherProfilePicStorageRef(receiverAuthId).getDownloadUrl()
                        .addOnCompleteListener(t -> {
                            if(t.isSuccessful()){
                                Uri uri  = t.getResult();
                                AndroidUtil.setProfilePic(this,uri,binding.iconImage);
                            }
                        });
            }
        });

        RemoteMessageBuilder remoteMessageBuilder = RemoteMessageBuilder.getInstance();
        remoteMessageBuilder.setContext(getApplicationContext());
        remoteMessageBuilder.setOpponentToken(opponentToken);

        binding.imageAcceptInvitation.setOnClickListener(v -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                opponentToken
        ));

        binding.imageRejectInvitation.setOnClickListener(v -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_REJECTED,
                opponentToken
        ));

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
                        (MessagingService.remoteMessageCallback) IncomingInvitationActivity.this);
            }

            @Override
            public void onAddStream(MediaStream mediaStream){
                super.onAddStream(mediaStream);
                Log.d("Log:","On Add Stream");
                VideoCallActivity.setMediaStream(mediaStream);
            }

        });
    }

    private void sendInvitationResponse(@NonNull String responseType, String opponentToken) {
        if(hasClickedButton){
            Logging("Duplicated Action","");
            return;
        }

        // Not Setting Session Description if rejected
        if(responseType.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
            Logging("Starting Video Call","");
            acceptCall();
        }else if(responseType.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
            Logging("Invitation Rejected","Invitation Rejected");
            rejectCall();
       }
        hasClickedButton = true;
    }

    private void acceptCall(){
        timeoutTimer.cancel();
        waitingText.setText(getResources().getString(R.string.connecting));
        // Permission Checking
        VideoCallActivity.initVideoStream(this, ()->{
            Logging("Please Accept All Permission","Please Accept All Permission");
            finish();
        });

        // Also Add Friend When Accepted Call
        Thread addFriend = new Thread(new Runnable() {
            @Override
            public void run() {
                FriendsClient.getInstance().addFriend(opponentUserId, ()->{
                    MessageFragment.setRefresh();
                    FacetimeFragment.setRefresh();
                    Log.d("Friend","Add Friend");
                });
            }
        });
        addFriend.start();

        Thread acceptCall = new Thread(new Runnable() {
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

                // Flow: Set Remote SD -> Set Local SD -> Send Local SD
                String remoteSessionDescription = getIntent().getStringExtra(Constants.REMOTE_MSG_SESSION_DESCRIPTION);

                webRTCClient.setRemoteSessionDescription(SessionDescription.Type.OFFER, remoteSessionDescription,
                        (rsd) -> {
                            Log.d("Log:","Remote Description Set");
                            webRTCClient.acceptOffer((lsd)->{
                                //This must be run after the webRTC generates the sessionDescription
                                SessionDescription localSessionDescription = (SessionDescription) lsd;
                                String body = RemoteMessageBuilder.invitationResponse(Constants.REMOTE_MSG_INVITATION_ACCEPTED, localSessionDescription.description);
                                MessagingService.sendRemoteMessage(getApplicationContext(), body, Constants.REMOTE_MSG_INVITATION_ACCEPTED, IncomingInvitationActivity.this);
                            });
                        });
            }
        });
        acceptCall.start();
    }

    private void rejectCall(){
        Thread rejectCall = new Thread(new Runnable() {
            @Override
            public void run() {
                String body = RemoteMessageBuilder.invitationResponse(Constants.REMOTE_MSG_INVITATION_REJECTED, null);
                MessagingService.sendRemoteMessage(getApplicationContext(), body, Constants.REMOTE_MSG_INVITATION_REJECTED, IncomingInvitationActivity.this);
            }
        });
        rejectCall.start();
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
                //Caller Cancel Invitation
                if(type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)){
                    timeoutTimer.cancel(); // Prevent Logging Multiple Times
                    Logging("Invitation Cancelled", "Invitation Cancelled");
                    finish();
                }
            }
        }
    };


    private void startVideoCall() {
        webRTCClient.handleIceCandidate();;
        // webRTCClient.state();
        Intent intent = new Intent(IncomingInvitationActivity.this, VideoCallActivity.class);
        intent.putExtra("opponentUserId",opponentUserId);
        intent.putExtra("opponentToken",opponentToken);
        startActivity(intent);
        finish();
    }

    @Override
    public void OnSuccessCallback(String type) {
        if(type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
            startVideoCall();
        }
        if(type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
            timeoutTimer.cancel(); // Prevent Logging Multiple Time
            finish();
        }else if(type.equals(Constants.REMOTE_MSG_ICE_CANDIDATE)){
            //Ice Candidate
            Logging("Ice Candidate Sent","");
        }
    }

    @Override
    public void OnFailureCallback(String msg) {
        Logging(msg,"Error");
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
                rejectCall();
                finish();
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        return;
    }
}