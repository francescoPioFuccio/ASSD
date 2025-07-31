package com.example.app1.ui.login;

import androidx.annotation.Nullable;

/**
 * Authentication result: success (user details) or error message.
 */
public class LoginResult {
    @Nullable
    private final LoggedInUserView success;
    @Nullable
    private final Integer error;

    public LoginResult(@Nullable LoggedInUserView success, @Nullable Integer error) {
        this.success = success;
        this.error = error;
    }

    public static LoginResult success(LoggedInUserView user) {
        return new LoginResult(user, null);
    }

    public static LoginResult error(int errorMsg) {
        return new LoginResult(null, errorMsg);
    }

    @Nullable
    public LoggedInUserView getSuccess() {
        return success;
    }

    @Nullable
    public Integer getError() {
        return error;
    }

    public boolean isSuccessful() {
        return success != null;
    }
}
