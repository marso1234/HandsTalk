package com.handstalk.signdetect.firebase;

import static com.handstalk.signdetect.network.AccessToken.getAccessToken;
import static com.handstalk.signdetect.network.AccessToken.loadAccessToken;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.handstalk.signdetect.activities.IncomingInvitationActivity;
import com.handstalk.signdetect.activities.VideoCallActivity;
import com.handstalk.signdetect.network.FCMRetrofitClient;
import com.handstalk.signdetect.network.RetrofitService;
import com.handstalk.signdetect.utilities.Constants;
import com.handstalk.signdetect.videocall.WebRTCClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.webrtc.IceCandidate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SharedPreferences sharePrefFirebase = getSharedPreferences(Constants.SHARED_PREF_FIREBASE_INFO,MODE_PRIVATE);

        Log.d("FCM", "Token: "+token);
        getSharedPreferences(Constants.SHARED_PREF_FIREBASE_INFO,MODE_PRIVATE).edit().putString(Constants.KEY_FCM_TOKEN, token).commit();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore mDB = FirebaseFirestore.getInstance();
        //Set FCM
        if(mAuth.getUid() != null){
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if(task.isSuccessful() && task.getResult()!=null){
                    Map<String,Object> update_values = new HashMap<>();
                    update_values.put(Constants.KEY_FCM_TOKEN, task.getResult());
                    mDB.collection(Constants.KEY_COLLECTIONS_USERS).document(mAuth.getUid()).update(update_values).addOnCompleteListener(updateFCMTask -> {
                        if(updateFCMTask.isSuccessful()){
                            Log.d("FCM", "Update FCM successful");
                            sharePrefFirebase.edit().putString(Constants.KEY_FCM_TOKEN, task.getResult()).commit();
                        }else{
                            Log.d("FCM", "Update FCM failed");
                        }
                    });

                }
            });
        }
    }



    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String type = remoteMessage.getData().get(Constants.REMOTE_MSG_TYPE);
        Log.d("Message received",type);
        if(type != null){
            // Case "invitation" : Receiving Video Meeting Invitation
            if(type.equals(Constants.REMOTE_MSG_INVITATION)){
                Intent intent = new Intent(getApplicationContext(), IncomingInvitationActivity.class);
                intent.putExtra(
                        Constants.REMOTE_MSG_MEETING_TYPE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE));
                intent.putExtra(
                        Constants.KEY_USERNAME,
                        remoteMessage.getData().get(Constants.KEY_USERNAME)
                );
                intent.putExtra(
                        Constants.KEY_AUTH_ID,
                        remoteMessage.getData().get(Constants.KEY_AUTH_ID)
                );
                intent.putExtra(
                        Constants.REMOTE_MSG_INVITER_TOKEN,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
                intent.putExtra(
                        Constants.KEY_USER_ID,
                        remoteMessage.getData().get(Constants.KEY_USER_ID)
                );
                intent.putExtra(
                        Constants.REMOTE_MSG_SESSION_DESCRIPTION,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_SESSION_DESCRIPTION)
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            // Case "invitationResponse" : Getting Invitation Response
            else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)){
                //Intent Filter: REMOTE_MSG_INVITATION_RESPONSE
                //https://www.geeksforgeeks.org/how-to-use-localbroadcastmanager-in-android/
                Intent intent = new Intent(Constants.REMOTE_MSG_INVITATION_RESPONSE);
                String response = remoteMessage.getData().get(Constants.REMOTE_MSG_INVITATION_RESPONSE);
                intent.putExtra(
                        Constants.REMOTE_MSG_INVITATION_RESPONSE,
                        response
                        );

                //Accept Call -> Sent Session Description Answer to caller
                if(response.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){

                    intent.putExtra(
                            Constants.REMOTE_MSG_SESSION_DESCRIPTION,
                            remoteMessage.getData().get(Constants.REMOTE_MSG_SESSION_DESCRIPTION)
                    );
                }

                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
            else if (type.equals(Constants.REMOTE_MSG_ICE_CANDIDATE)){
                WebRTCClient webRTCClient = WebRTCClient.getInstance();
                IceCandidate iceCandidate = new IceCandidate(
                        remoteMessage.getData().get(Constants.REMOTE_MSG_SDP_MID),
                        Integer.parseInt(remoteMessage.getData().get(Constants.REMOTE_MSG_SDP_MLINE_INDEX)),
                        remoteMessage.getData().get(Constants.REMOTE_MSG_SDP)
                );
                if(!webRTCClient.isInitialized()){
                    webRTCClient.addIceCandidateQueue(iceCandidate);
                }else{
                    webRTCClient.addIceCandidate(iceCandidate);
                }
                Log.d("Ice Candidate Queue Size:", String.valueOf(webRTCClient.getIceCandidateQueueSize()));
            }
            else if (type.equals(Constants.REMOTE_MSG_END_CALL)){
                // End Call When other's Click the endcall button
                Log.d("Message Received","end call detected");
                if(Objects.equals(VideoCallActivity.getOpponentId(), remoteMessage.getData().get(Constants.KEY_USER_ID))){
                    VideoCallActivity.videoCallActivity.finish();
                    if (WebRTCClient.getInstance() != null){
                        WebRTCClient.getInstance().closeConnection();
                    }
                }
            }
            else if (type.equals(Constants.REMOTE_MSG_VIDEO_MESSAGE)){
                // Prevent Unexpected call from others
                String opponentId = VideoCallActivity.getOpponentId();
                if (remoteMessage.getData().get(Constants.KEY_USER_ID).equals(opponentId)){
                    String message = remoteMessage.getData().get(Constants.REMOTE_MSG_VIDEO_MESSAGE);
                    VideoCallActivity.videoCallActivity.setOpponentMessage(message);
                    Log.d("Video Message: opponentToken", opponentId);

                }else{
                    Log.d("Video Message: opponentToken", opponentId);
                    Log.d("Video Message: Message opponentToken", remoteMessage.getData().get(Constants.KEY_USER_ID));
                    Log.d("Video Message", "FCM Not equal");
                }
            }
        }
    }

    //https://github.com/bimalkaf/Android_Chat_Application/blob/main/app/src/main/java/com/example/easychat/ChatActivity.java
    //Send Video/ Audio Call Invitation
    public static void sendRemoteMessage(Context context, String remoteMessageBody, String type, remoteMessageCallback remoteMessageCallback) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Async Handling
                    Thread loadAT = loadAccessToken(context);
                    loadAT.start();
                    loadAT.join();
                    String accessToken = getAccessToken();
                    if(accessToken.isEmpty()) {
                        Log.d("Messaging Service", "");
                        Toast.makeText(context, "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FCMRetrofitClient.getClient().create(RetrofitService.class).sendRemoteMessage(
                            Constants.getRemoteMessageHeaders(getAccessToken()), remoteMessageBody
                    ).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                            if(response.isSuccessful()){
                                remoteMessageCallback.OnSuccessCallback(type);
                            }else{
                                try {
                                    remoteMessageCallback.OnFailureCallback(response.errorBody().string());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                            remoteMessageCallback.OnFailureCallback(t.getMessage());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public interface remoteMessageCallback {
        void OnSuccessCallback(String type);
        void OnFailureCallback(String msg);
    }
}
