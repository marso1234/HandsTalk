package com.handstalk.signdetect.utilities;

import androidx.annotation.NonNull;

import com.handstalk.signdetect.BuildConfig;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;

public class Constants {

    public static final String KEY_COLLECTIONS_USERS = "users";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_COLLECTIONS_USER_ID = "userIds";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_AUTH_ID = "auth_id";
    public static final String KEY_MSG_ID = "message";
    public static final String KEY_COLLECTION_FRIENDS = "friends";
    public static final String KEY_FRIEND_IDS = "friendIds";
    public static final String KEY_FCM_TOKEN = "fcm_token";

    public static final String SHARED_PREF_FIREBASE_INFO="firebase_info";

    public static final String BASE_URL="https://fcm.googleapis.com/v1/";
    public static final String FCM_SEND_ENDPOINT="projects/video-call-example-5eeb7/messages:send";

    public static final int passwordMinLength = 8;
    public static final int passwordMaxLength = 22;
    public static final int usernameMinLength = 1;
    public static final int usernameMaxLength = 22;

    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_BEARER = "Bearer ";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";

    public static final String REMOTE_MSG_TYPE = "type";
    public static final String REMOTE_MSG_INVITATION = "invitation";
    public static final String REMOTE_MSG_MEETING_TYPE = "meetingType";
    public static final String REMOTE_MSG_INVITER_TOKEN = "inviterToken";
    public static final String REMOTE_MSG_DATA = "data";

    public static final String REMOTE_MSG_END_CALL = "endCall";

    public static final String REMOTE_MSG_INVITATION_RESPONSE = "invitationResponse";
    public static final String REMOTE_MSG_INVITATION_ACCEPTED = "accepted";
    public static final String REMOTE_MSG_INVITATION_REJECTED = "rejected";
    public static final String REMOTE_MSG_INVITATION_CANCELLED = "invitationCancel";

    public static final String REMOTE_MSG_VIDEO_MESSAGE = "videoMessage";

    public static final String REMOTE_MSG_ICE_CANDIDATE = "iceCandidate";
    public static final String REMOTE_MSG_SDP_MID = "sdpMid";
    public static final String REMOTE_MSG_SDP_MLINE_INDEX = "sdpMLineIndex";
    public static final String REMOTE_MSG_SDP = "sdp";

    public static final String REMOTE_MSG_SESSION_DESCRIPTION = "sessionDescription";

    // 500MB Quota only, Please use local if possible
    public static final String ICE_SERVER_API_URL = "https://sign-translate.metered.live/api/v1/turn/";
    public static final String ICE_SERVER_API_ENDPOINT = "credentials";
    public static final String ICE_SERVER_API_KEY = BuildConfig.ICE_SERVER_API_KEY;

    // GPT - Sentence Completion
    public static final String OPENAI_API_URL = "https://hkust.azure-api.net/";
    public static final String OPENAI_API_ENDPOINT = "openai/deployments/gpt-35-turbo/chat/completions?api-version=2023-12-01-preview";
    public static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;
    public static final String OPENAI_CONTENT_TYPE = "Content-Type";
    public static final String OPENAI_API_HEADER = "api-key";

    public static final String TOGGLE_HINT = "Perform <Toggle> to start";

    public static final String HAND_TASK_PATH = "hand_landmarker.task";
    public static final String POSE_TASK_PATH = "pose_landmarker.task";

    public static HashMap<String,String> getRemoteMessageHeaders(String CredentialKey) {
        HashMap<String,String> headers = new HashMap<>();
        headers.put(REMOTE_MSG_AUTHORIZATION, REMOTE_MSG_BEARER + CredentialKey);
        headers.put(REMOTE_MSG_CONTENT_TYPE, "application/json; charset=utf-8");
        return headers;
    }

    private static HashMap<String, String> openAIHeader = new HashMap<>();
    public static HashMap<String,String> getOpenAICompletionHeaders() {
        if (openAIHeader.isEmpty()){
            openAIHeader = new HashMap<>();
            openAIHeader.put(OPENAI_API_HEADER, OPENAI_API_KEY);
            openAIHeader.put(OPENAI_CONTENT_TYPE, "application/json; charset=utf-8");
        }
        return openAIHeader;
    }



    @NonNull
    public static String generateRandomId(){
        Date currentTime = Timestamp.now().toDate();
        String timeId = String.valueOf(currentTime.getTime());
        return timeId + (int) Math.floor(Math.random() * 999);
    }



}
