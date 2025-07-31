package com.example.app1.ui.login;

public class RegisterFormState {
    private Integer nomeError;
    private Integer cognomeError;
    private Integer emailError;
    private Integer passwordError;
    private Integer confirmPasswordError;
    private boolean isDataValid;

    public RegisterFormState(Integer nomeError, Integer cognomeError, Integer emailError, Integer passwordError, Integer confirmPasswordError) {
        this.nomeError = nomeError;
        this.cognomeError = cognomeError;
        this.emailError = emailError;
        this.passwordError = passwordError;
        this.confirmPasswordError = confirmPasswordError;
        this.isDataValid = false;
    }

    public RegisterFormState(boolean isDataValid) {
        this.nomeError = null;
        this.cognomeError = null;
        this.emailError = null;
        this.passwordError = null;
        this.confirmPasswordError = null;
        this.isDataValid = isDataValid;
    }

    public Integer getNomeError() {
        return nomeError;
    }

    public Integer getCognomeError() {
        return cognomeError;
    }

    public Integer getEmailError() {
        return emailError;
    }

    public Integer getPasswordError() {
        return passwordError;
    }

    public Integer getConfirmPasswordError() {
        return confirmPasswordError;
    }

    public boolean isDataValid() {
        return isDataValid;
    }
}
