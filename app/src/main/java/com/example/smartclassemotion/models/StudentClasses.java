package com.example.smartclassemotion.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class StudentClasses {
    @PropertyName("studentClassesId")
    private String studentClassesId;

    @PropertyName("studentId")
    private String studentId;

    @PropertyName("classId")
    private String classId;

    @PropertyName("joinedAt")
    @ServerTimestamp
    private Timestamp joinedAt;

    // Constructor mặc định
    public StudentClasses() {
    }

    // Constructor đầy đủ
    public StudentClasses(String studentClassesId, String studentId, String classId, Timestamp joinedAt) {
        this.studentClassesId = studentClassesId;
        this.studentId = studentId;
        this.classId = classId;
        this.joinedAt = joinedAt;
    }

    // Getters và Setters
    @PropertyName("studentClassesId")
    public String getStudentClassesId() {
        return studentClassesId;
    }

    @PropertyName("studentClassesId")
    public void setStudentClassesId(String studentClassesId) {
        this.studentClassesId = studentClassesId;
    }

    @PropertyName("studentId")
    public String getStudentId() {
        return studentId;
    }

    @PropertyName("studentId")
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    @PropertyName("classId")
    public String getClassId() {
        return classId;
    }

    @PropertyName("classId")
    public void setClassId(String classId) {
        this.classId = classId;
    }

    @PropertyName("joinedAt")
    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    @PropertyName("joinedAt")
    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }
}