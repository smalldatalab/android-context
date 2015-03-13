package org.ohmage.funf;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by changun on 3/12/15.
 */



public class DSUClient {
    private static final String BASE_URL = "https://lifestreams.smalldata.io/dsu";
    public static final String CLIENT_ID = "io.smalldata.android.context";
    public static final String CLIENT_SECRET = "IcpeG1Fbg2";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final static OkHttpClient client = new OkHttpClient();

    public static Response postData(String accessToken, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(BASE_URL + "/dataPoints")
                .header("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();
        return client.newCall(request).execute();
    }

    public static Response signin(String googleToken) throws IOException {
        RequestBody body = new FormEncodingBuilder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("google_access_token", googleToken).build();
        Request request = new Request.Builder()
                .url(BASE_URL + "/google-signin")
                .post(body)
                .build();
        return client.newCall(request).execute();
    }


    public static Response refreshToken(String refreshToken) throws IOException {
        RequestBody body = new FormEncodingBuilder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        Request request = new Request.Builder()
                .url(BASE_URL + "/oauth/token")
                .post(body)
                .build();
        return client.newCall(request).execute();
    }

}
