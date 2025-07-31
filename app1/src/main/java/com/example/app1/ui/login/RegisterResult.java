package com.example.app1.ui.login;

import androidx.annotation.Nullable;

public class RegisterResult {
    @Nullable
    private Integer error;
    @Nullable
    private RegisteredUserView success;

    public RegisterResult(@Nullable Integer error) {
        this.error = error;
        this.success = null;
    }

    public RegisterResult(@Nullable RegisteredUserView success) {
        this.success = success;
        this.error = null;
    }

    @Nullable
    public Integer getError() {
        return error;
    }

    @Nullable
    public RegisteredUserView getSuccess() {
        return success;
    }
}