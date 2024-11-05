package com.handstalk.signdetect.network;

import android.content.Context;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.InputStream;
import java.util.Arrays;

//Singleton Class for Getting Access Token
public final class AccessToken {
    private static AccessToken myAccessToken;
    private static final String[] SCOPES = { "https://www.googleapis.com/auth/firebase.messaging" };
    private static GoogleCredentials googleCredentials;
    private static String beaererToken;

    public static Thread loadAccessToken(Context context) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream jsonFile = context.getApplicationContext().getResources().openRawResource(
                            context.getApplicationContext().getResources().getIdentifier(
                                    "serviceaccount","raw",context.getPackageName()));
                    // Your code goes here
                    googleCredentials = GoogleCredentials
                            .fromStream(jsonFile)
                            .createScoped(Arrays.asList(SCOPES));

                    googleCredentials.refreshAccessToken().getTokenValue();
                    beaererToken = googleCredentials.refreshAccessToken().getTokenValue();

                    // Log.d("BearerToken", beaererToken);
                    jsonFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return thread;
    }

    public static String getAccessToken(){
        return beaererToken;
    }
}
