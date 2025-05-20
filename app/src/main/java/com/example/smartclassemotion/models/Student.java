package com.example.smartclassemotion.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class Student {
    @PropertyName("studentId")
    private String studentId;

    @PropertyName("userId")
    private String userId;

    @PropertyName("studentCode")
    private String studentCode;

    @PropertyName("studentName")
    private String studentName;

    @PropertyName("dateOfBirth")
    private Timestamp dateOfBirth;

    @PropertyName("gender")
    private String gender;

    @PropertyName("email")
    private String email;

    @PropertyName("phone")
    private String phone;

    @PropertyName("status")
    private String status;

    @PropertyName("avatarUrl")
    private String avatarUrl;

    @PropertyName("notes")
    private String notes;
    public Student() {
    }

    public Student(String studentId,String userId, String studentCode, String studentName, Timestamp dateOfBirth,
                   String gender, String email, String phone, String avatarUrl, String note) {
        this.studentId = studentId;
        this.userId = userId;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.notes = note;
    }

    // Getter và Setter
    @PropertyName("studentId")
    public String getStudentId() { return studentId; }
    @PropertyName("studentId")
    public void setStudentId(String studentId) { this.studentId = studentId; }
    @PropertyName("studentCode")
    public String getStudentCode() { return studentCode; }
    @PropertyName("studentCode")
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    @PropertyName("studentName")
    public String getStudentName() { return studentName; }
    @PropertyName("studentName")
    public void setStudentName(String studentName) { this.studentName = studentName; }
    @PropertyName("dateOfBirth")
    public Timestamp getDateOfBirth() { return dateOfBirth; }
    @PropertyName("dateOfBirth")
    public void setDateOfBirth(Timestamp dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    @PropertyName("gender")
    public String getGender() { return gender; }
    @PropertyName("gender")
    public void setGender(String gender) { this.gender = gender; }
    @PropertyName("email")
    public String getEmail() { return email; }
    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }
    @PropertyName("phone")
    public String getPhone() { return phone; }
    @PropertyName("phone")
    public void setPhone(String phone) { this.phone = phone; }
    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }
    @PropertyName("notes")
    public String getNotes() {
        return notes;
    }
    @PropertyName("notes")
    public void setNotes(String notes) {
        this.notes = notes;
    }
    @PropertyName("avatarUrl")
    public String getAvatarUrl() {return avatarUrl;}
    @PropertyName("avatarUrl")
    public void setAvatarUrl(String avatarUrl) {this.avatarUrl = avatarUrl;}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}