package com.cloudide.cloudide.dto;

public class LoginResponse {

    private String token;
    private AuthUserResponse user;

    public LoginResponse() {
    }

    public LoginResponse(String token, AuthUserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AuthUserResponse getUser() {
        return user;
    }

    public void setUser(AuthUserResponse user) {
        this.user = user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private AuthUserResponse user;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder user(AuthUserResponse user) {
            this.user = user;
            return this;
        }

        public LoginResponse build() {
            return new LoginResponse(token, user);
        }
    }
}
