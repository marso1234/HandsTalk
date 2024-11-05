package com.handstalk.signdetect.model;

import java.io.Serializable;

public class User implements Serializable {

    private String username, email, userId, fcm_token, profilePic;

    public User() {
    }
    public User (String username, String email, String userId, String fcm_token) {
        this.username = username;
        this.email = email;
        this.userId = userId;
        this.fcm_token = fcm_token;
    }

    public String getUsername(){
        return username;
    }

    public String getEmail(){
        return email;
    }

    public String getUserId(){
        return userId;
    }

    public String getFcm_token(){return fcm_token;}

    public String getProfilePic() {
        return profilePic;
    }

    public void setUsername(String username){this.username = username;}

    public void setUserId(String userId){
        this.userId = userId;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setFcm_token(String fcm_token){ this.fcm_token = fcm_token;}

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
}
