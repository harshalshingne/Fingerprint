package com.example.fingerprint;

public class UserObject {


    private boolean loginOption;
    public UserObject( boolean loginOption) {
        this.loginOption = loginOption;
    }

    public boolean isLoginOption() {
        return loginOption;
    }
}
