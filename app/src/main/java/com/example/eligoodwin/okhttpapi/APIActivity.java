package com.example.eligoodwin.okhttpapi;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;



import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class APIActivity extends AppCompatActivity {

    private AuthorizationService mAuthorizationService;
    private AuthState mAuthState;
    private OkHttpClient mOkHttpClient;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String key = "AAIzaSyBj3K0P9gsEWLSyqOTa_Y3qqJBRZf3DTWw";
    private final String clietID = "595194933922-b363iel5f0rbd7lgnogmci1r5gjg6m63.apps.googleusercontent.com";

    private final String TAG = APIActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences authPreference = getSharedPreferences("auth", MODE_PRIVATE);
        setContentView(R.layout.activity_api);
        mAuthorizationService = new AuthorizationService(this);
        ((Button)findViewById(R.id.getPosts)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                        @Override
                        public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException e) {
                            if(e == null){
                                mOkHttpClient = new OkHttpClient();
                                HttpUrl reqUrl = HttpUrl.parse("https://www.googleapis.com/plusDomains/v1/people/me/activities/user");
                                reqUrl = reqUrl.newBuilder().addQueryParameter("key", key).build();
                                Request request = new Request.Builder()
                                        .url(reqUrl)
                                        .addHeader("Authorization", "Bearer " + accessToken)
                                        .build();
                                mOkHttpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        e.printStackTrace();
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        String r = response.body().string();
                                        try {
                                            JSONObject j = new JSONObject(r);
                                            JSONArray items = j.getJSONArray("items");
                                            List<Map<String,String>> posts = new ArrayList<Map<String,String>>();
                                            for(int i = 0; i < items.length(); i++){
                                                HashMap<String, String> m = new HashMap<String, String>();
                                                m.put("published", items.getJSONObject(i).getString("published"));
                                                m.put("title",items.getJSONObject(i).getString("title"));
                                                posts.add(m);
                                            }
                                            final SimpleAdapter postAdapter = new SimpleAdapter(
                                                    APIActivity.this,
                                                    posts,
                                                    R.layout.gplus_items,
                                                    new String[]{"published", "title"},
                                                    new int[]{R.id.publishDate, R.id.whatever});
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((ListView)findViewById(R.id.postList)).setAdapter(postAdapter);
                                                }
                                            });
                                        } catch (JSONException e1) {
                                            e1.printStackTrace();
                                        }

                                    }
                                });
                            }
                        }
                    });
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        ((Button)findViewById(R.id.submitPost)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                        @Override
                        public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException e) {
                            if(e == null){
                                EditText mEdit;
                                mEdit = (EditText)findViewById(R.id.textForPost);
                                String userInput = mEdit.getText().toString();
                                String json = "{'object': {'originalContent': '" + userInput + "'},'access': {'domainRestricted': true}}";
                                mOkHttpClient = new OkHttpClient();
                                HttpUrl reqUrl = HttpUrl.parse("https://www.googleapis.com/plusDomains/v1/people/me/activities");
                                reqUrl = reqUrl.newBuilder().addQueryParameter("key", key).build();
                                RequestBody body = RequestBody.create(JSON, json);
                                Request request = new Request.Builder()
                                        .url(reqUrl)
                                        .post(body)
                                        .addHeader("Authorization", "Bearer " + accessToken)
                                        .build();
                                mOkHttpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        e.printStackTrace();
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        Log.d(TAG, response.body().string());
                                    }
                                });
                            }
                        }
                    });
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart(){
        mAuthState = getOrCreateAuthState();
        super.onStart();

    }

    AuthState getOrCreateAuthState(){
        AuthState auth = null;
        SharedPreferences authPreference = getSharedPreferences("auth", MODE_PRIVATE);
        String stateJson = authPreference.getString("stateJson", null);
        if(stateJson != null){
            try {
                auth = AuthState.jsonDeserialize(stateJson);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        if( auth != null && auth.getAccessToken() != null){
            return auth;
        } else {
            updateAuthState();
            return null;
        }
    }

    void updateAuthState(){

        Uri authEndpoint = new Uri.Builder().scheme("https").authority("accounts.google.com").path("/o/oauth2/v2/auth").build();
        Uri tokenEndpoint = new Uri.Builder().scheme("https").authority("www.googleapis.com").path("/oauth2/v4/token").build();
        Uri redirect = new Uri.Builder().scheme("com.eligoodwin.okhttpapi").path("foo").build();

        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint, null);
        AuthorizationRequest req = new AuthorizationRequest.Builder(config, clietID, ResponseTypeValues.CODE, redirect)
                .setScopes("https://www.googleapis.com/auth/plus.me", "https://www.googleapis.com/auth/plus.stream.write", "https://www.googleapis.com/auth/plus.stream.read")
                .build();

        Intent authComplete = new Intent(this, AuthCompleteActivity.class);
        mAuthorizationService.performAuthorizationRequest(req, PendingIntent.getActivity(this, req.hashCode(), authComplete, 0));
    }
}