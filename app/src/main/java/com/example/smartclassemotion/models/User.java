package com.example.smartclassemotion.models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class User {
    @PropertyName("userId")
    private String userId;

    @PropertyName("username")
    private String username;

    @PropertyName("email")
    private String email;

    @PropertyName("role")
    private String role;

    @PropertyName("createdAt")
    @ServerTimestamp
    private Date CreatedAt;

    public User(){}

    public User(String username, String userId, String email, String role, Date CreatedAt) {
        this.username = username;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.CreatedAt = CreatedAt;
    }
    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }
    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }
    @PropertyName("username")
    public String getUsername() {
        return username;
    }
    @PropertyName("username")
    public void setUsername(String username) {
        this.username = username;
    }
    @PropertyName("email")
    public String getEmail() {
        return email;
    }
    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }
    @PropertyName("role")
    public String getRole() {return role;}
    @PropertyName("role")
    public void setRole(String role) {
        this.role = role;
    }
    @PropertyName("createdAt")
    public Date getCreatedAt() {return CreatedAt;}
    @PropertyName("createdAt")
    public void setCreatedAt(Date createdAt) {CreatedAt = createdAt;}
}
