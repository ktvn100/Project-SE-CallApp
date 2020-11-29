package com.hcmus.callapp.model;

import java.io.Serializable;

public class User implements Serializable {
    public String status, androidID, username;

    public User(String status,String androidID, String username) {
        this.status = status;
        this.androidID = androidID;
        this.username = username;
    }

    public User(User user) {

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAndroidID() {
        return androidID;
    }

    public void setAndroidID(String androidID) {
        this.androidID = androidID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
