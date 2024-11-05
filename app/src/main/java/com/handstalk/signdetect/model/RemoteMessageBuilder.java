package com.handstalk.signdetect.model;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Nullable;

import com.handstalk.signdetect.utilities.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

public class RemoteMessageBuilder {
    private static RemoteMessageBuilder remoteMessageBuilder;
    private Context context;
    private String opponentToken;

    public void setContext(Context context){
        remoteMessageBuilder.context = context;
    }

    public void setOpponentToken(String opponentToken){
        remoteMessageBuilder.opponentToken = opponentToken;
    }

    public static RemoteMessageBuilder getInstance(){
        if(remoteMessageBuilder==null){
            remoteMessageBuilder = new RemoteMessageBuilder();
        }
        return remoteMessageBuilder;
    }

    public static String invitationOffer(String meetingType, String senderLocalSessionDescription){
        try{
            SharedPreferences sharePrefFirebase = remoteMessageBuilder.context.getSharedPreferences(Constants.SHARED_PREF_FIREBASE_INFO,MODE_PRIVATE);
            /*
                Structure:
                Body
                    Message
                        Data
                            MessageType: Invitation
                            MeetingType: Video/ Audio
                            Username: Sender's name
                            Authid: Sender's authid
                            InviterToken: Sender's Token
                            SessionDescription: SessionDescription of Sender
                        RegistrationIds: receiverTokens[]
             */

            JSONArray tokens = new JSONArray();
            tokens.put(remoteMessageBuilder.opponentToken);

            JSONObject body = new JSONObject();
            JSONObject message = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_USERNAME, sharePrefFirebase.getString(Constants.KEY_USERNAME,""));
            data.put(Constants.KEY_AUTH_ID, sharePrefFirebase.getString(Constants.KEY_AUTH_ID, ""));
            data.put(Constants.KEY_USER_ID, sharePrefFirebase.getString(Constants.KEY_USER_ID, ""));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, sharePrefFirebase.getString(Constants.KEY_FCM_TOKEN,""));
            data.put(Constants.REMOTE_MSG_SESSION_DESCRIPTION, senderLocalSessionDescription);

            message.put(Constants.REMOTE_MSG_DATA, data);
            message.put("token", remoteMessageBuilder.opponentToken);

            body.put("message", message);
            return body.toString();
        }catch (Exception exception){
            Log.d("initiateMeeting", "Error");
            exception.printStackTrace();
            return "";
        }
    }

    public static String invitationResponse(String responseType, @Nullable String receiverLocalSessionDescription){
        try{

            /*
                Structure:
                Body
                    Message
                        Data
                            MessageType: invitationResponse
                            invitationResponse: accepted/ rejected
                        RegistrationIds: receiverTokens[]
             */

            Log.d("Opponent's Token",remoteMessageBuilder.opponentToken);
            JSONArray tokens = new JSONArray();
            tokens.put(remoteMessageBuilder.opponentToken);

            JSONObject body = new JSONObject();
            JSONObject message = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, responseType);

            if(responseType.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED) && receiverLocalSessionDescription != null){
                data.put(Constants.REMOTE_MSG_SESSION_DESCRIPTION, receiverLocalSessionDescription);
            }

            message.put(Constants.REMOTE_MSG_DATA, data);
            message.put("token", remoteMessageBuilder.opponentToken);

            body.put("message", message);

            return body.toString();

        }catch (Exception exception){
            exception.printStackTrace();
            return "";
        }
    }

    public static String iceCandidate(String sdpMid, int sdpMLineIndex, String sdp){
            /*
                Structure:
                Body
                    Message
                        Data
                            MessageType: Ice Candidate
                            sdpMid: sdpMid
                            sdpMLineIndex: sdpMLineIndex
                            sdp: sdp
                        RegistrationIds: receiverTokens[]
             */
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(remoteMessageBuilder.opponentToken);

            JSONObject body = new JSONObject();
            JSONObject message = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_ICE_CANDIDATE);
            data.put(Constants.REMOTE_MSG_SDP_MID, sdpMid);
            data.put(Constants.REMOTE_MSG_SDP_MLINE_INDEX, String.valueOf(sdpMLineIndex));
            data.put(Constants.REMOTE_MSG_SDP, sdp);

            message.put(Constants.REMOTE_MSG_DATA, data);
            message.put("token", remoteMessageBuilder.opponentToken);

            body.put("message", message);
            return body.toString();
        }catch (Exception e){
            Log.d("sendIceCandidate", "Error");
            e.printStackTrace();
            return "";
        }
    }

    public static String endCallResponse(){
        try{

            /*
                Structure:
                Body
                    Message
                        Data
                            MessageType: invitationResponse
                        RegistrationIds: receiverTokens[]
             */
            SharedPreferences sharePrefFirebase = remoteMessageBuilder.context.getSharedPreferences(Constants.SHARED_PREF_FIREBASE_INFO,MODE_PRIVATE);

            Log.d("Opponent's Token",remoteMessageBuilder.opponentToken);
            JSONArray tokens = new JSONArray();
            tokens.put(remoteMessageBuilder.opponentToken);

            JSONObject body = new JSONObject();
            JSONObject message = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_END_CALL);
            data.put(Constants.KEY_USER_ID, sharePrefFirebase.getString(Constants.KEY_USER_ID, ""));

            message.put(Constants.REMOTE_MSG_DATA, data);
            message.put("token", remoteMessageBuilder.opponentToken);

            body.put("message", message);

            return body.toString();

        }catch (Exception exception){
            exception.printStackTrace();
            return "";
        }
    }

    public static String VideoMessage(String toSend){
        try{
            /*
                Structure:
                Body
                    Message
                        Data
                            MessageType: invitationResponse
                            videoMessage: Message of translation (Could be the message, translating... and sentence)
                        RegistrationIds: receiverTokens[]
             */

            SharedPreferences sharePrefFirebase = remoteMessageBuilder.context.getSharedPreferences(Constants.SHARED_PREF_FIREBASE_INFO,MODE_PRIVATE);

            // Log.d("Opponent's Token",remoteMessageBuilder.opponentToken);
            JSONArray tokens = new JSONArray();
            tokens.put(remoteMessageBuilder.opponentToken);

            JSONObject body = new JSONObject();
            JSONObject message = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_VIDEO_MESSAGE);
            data.put(Constants.REMOTE_MSG_VIDEO_MESSAGE, toSend);
            data.put(Constants.KEY_USER_ID, sharePrefFirebase.getString(Constants.KEY_USER_ID, ""));

            message.put(Constants.REMOTE_MSG_DATA, data);
            message.put("token", remoteMessageBuilder.opponentToken);

            body.put("message", message);

            return body.toString();

        }catch (Exception exception){
            exception.printStackTrace();
            return "";
        }
    }
}
