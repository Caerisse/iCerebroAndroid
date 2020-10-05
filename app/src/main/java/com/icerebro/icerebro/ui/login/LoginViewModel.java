package com.icerebro.icerebro.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.SharedPreferences;
import android.util.Log;
import android.util.Patterns;

import com.icerebro.icerebro.data.model.Key;
import com.icerebro.icerebro.R;
import com.icerebro.icerebro.data.model.RegisteringUser;
import com.icerebro.icerebro.rest.ServiceGenerator;
import com.icerebro.icerebro.rest.iCerebroService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    private iCerebroService client;

    LoginViewModel(iCerebroService client) {
        this.client = client;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        RegisteringUser registeringUser = new RegisteringUser();
        if (username.contains("@")) {
            registeringUser.setEmail(username);
        } else {
            registeringUser.setUsername(username);
        }
        registeringUser.setPassword(password);
        Call<Key> call = client.login(registeringUser);
        call.enqueue(new Callback<Key>() {
            @Override
            public void onResponse(Call<Key> call, Response<Key> response) {
                if (!response.isSuccessful() && response.errorBody() != null) {
                    try {
                        JSONObject error = new JSONObject(response.errorBody().string());
                        // TODO: modify to accept JSONError and use it to mark errors
                        loginResult.setValue(new LoginResult(R.string.login_failed));
                    } catch (IOException | JSONException e) {
                        Log.e("Login - onResponse", e.getMessage());
                    }
                } else {
                    if(response.body().getKey() != null) {
                        String authToken = response.body().getKey();
                        Log.i("authToken", authToken);
                        loginResult.setValue(new LoginResult(authToken));
                    } else {
                        loginResult.setValue(new LoginResult(R.string.login_failed));
                    }
                }
            }

            @Override
            public void onFailure(Call<Key> call, Throwable t) {
                // TODO: modify to accept onFailure throwable or simply put another string saying its that
                loginResult.setValue(new LoginResult(R.string.login_failed));
            }
        });
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}