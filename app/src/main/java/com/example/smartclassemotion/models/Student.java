package com.example.smartclassemotion.models;

import com.google.firebase.Timestamp;

public class Student {
    private String studentId;
    private String classId;
    private String studentCode;
    private String studentName;
    private Timestamp dateOfBirth;
    private String gender;
    private String email;
    private String phone;
    private String status;
    private String mood;
    private String notes;
    public Student() {
    }

    public Student(String studentId, String classId, String studentCode, String studentName,
                   Timestamp dateOfBirth, String gender, String email, String phone, String status, String mood, String notes) {
        this.studentId = studentId;
        this.classId = classId;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.mood = mood;
        this.notes = notes;
    }

    // Getter và Setter
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public Timestamp getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Timestamp dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}