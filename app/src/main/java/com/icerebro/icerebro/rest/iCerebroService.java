package com.icerebro.icerebro.rest;

import com.icerebro.icerebro.data.model.Key;
import com.icerebro.icerebro.data.model.LoggedInUser;
import com.icerebro.icerebro.data.model.ProxyPort;
import com.icerebro.icerebro.data.model.PubKey;
import com.icerebro.icerebro.data.model.RegisteringUser;
import com.icerebro.icerebro.data.model.Token;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface iCerebroService {

    //AUTHENTICATION
    @POST("rest-auth/registration/")
    Call<RegisteringUser> createUser(
            @Body RegisteringUser user
    );

    @POST("rest-auth/login/")
    Call<Key> login(
            @Body RegisteringUser user
    );

    @POST("rest-auth/logout/")
    Call<LoggedInUser> logout();

    @POST("rest-auth/password/change/")
    Call<LoggedInUser> passwordChange();

    @POST("rest-auth/password/reset/")
    Call<LoggedInUser> passwordReset();

    @GET("rest-auth/user/")
    Call<LoggedInUser> getCurrentUser();

    @POST("rest-auth/token/")
    Call<Token> getToken();

    @POST("proxy/pubkey/")
    Call<PubKey> savePubKey(
            @Body PubKey pubKey
    );

    @GET("proxy/address/")
    Call<ProxyPort> getProxyPort();
}