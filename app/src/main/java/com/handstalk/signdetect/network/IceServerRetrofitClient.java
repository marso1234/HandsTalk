package com.handstalk.signdetect.network;

import static com.handstalk.signdetect.utilities.Constants.ICE_SERVER_API_URL;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class IceServerRetrofitClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if(retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl(ICE_SERVER_API_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

}
