package com.handstalk.signdetect.network;

import com.handstalk.signdetect.utilities.Constants;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RetrofitService {

    @POST(Constants.FCM_SEND_ENDPOINT)
    Call<String> sendRemoteMessage(
            @HeaderMap HashMap<String, String> headers,
            @Body String remoteBody
    );

    @GET(Constants.ICE_SERVER_API_ENDPOINT)
    Call<String> fetchIceServer(@Query("apiKey") String apiKey);

    @POST(Constants.OPENAI_API_ENDPOINT)
    Call<String> sentenceCompletion(
            @HeaderMap HashMap<String, String> headers,
            @Body String promptBody
    );

}
