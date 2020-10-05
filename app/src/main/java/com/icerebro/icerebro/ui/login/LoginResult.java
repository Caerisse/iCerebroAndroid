package com.icerebro.icerebro.ui.login;

import androidx.annotation.Nullable;

/**
 * Authentication result : success (user details) or error message.
 */
class LoginResult {
    @Nullable
    private String success;
    @Nullable
    private Integer error;

    LoginResult(@Nullable Integer error) {
        this.error = error;
    }

    LoginResult(@Nullable String authToken) {
        this.success = authToken;
    }

    @Nullable
    String getSuccess() {
        return success;
    }

    @Nullable
    Integer getError() {
        return error;
    }
}