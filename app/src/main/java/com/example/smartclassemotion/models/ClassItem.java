package com.example.smartclassemotion.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class ClassItem {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @PropertyName("classId")
    private String classId;
    @PropertyName("userId")
    private String userId;
    @PropertyName("className")
    private String className;
    @PropertyName("startTime")
    private Timestamp startTime;
    @PropertyName("endTime")
    private Timestamp endTime;
    @PropertyName("dayOfWeek")
    private String dayOfWeek;
    @PropertyName("description")
    private String description;

    public ClassItem(String classId, String userId, String className, Timestamp startTime, Timestamp endTime, String dayOfWeek, String description) {
        this.classId = classId;
        this.userId = userId;
        this.className = className;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
        this.description = description;
    }

    public ClassItem() {
    }
    @PropertyName("classId")
    public String getClassId() {
        return classId;
    }
    @PropertyName("classId")
    public void setClassId(String classId) {
        this.classId = classId;
    }
    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }
    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }
    @PropertyName("className")
    public String getClassName() {
        return className;
    }
    @PropertyName("className")
    public void setClassName(String className) {
        this.className = className;
    }
    @PropertyName("startTime")
    public Timestamp getStartTime() {
        return startTime;
    }
    @PropertyName("startTime")
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    @PropertyName("endTime")
    public Timestamp getEndTime() {
        return endTime;
    }
    @PropertyName("endTime")
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    @PropertyName("dayOfWeek")
    public String getDayOfWeek() {
        return dayOfWeek;
    }
    @PropertyName("dayOfWeek")
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
    @PropertyName("description")
    public String getDescription() {
        return description;
    }
    @PropertyName("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormattedTime(){
        if(startTime == null || endTime == null || dayOfWeek == null){
            return "N/A";
        }
        SDF.setTimeZone(TimeZone.getDefault());
        String start = SDF.format(startTime.toDate());
        String end = SDF.format(endTime.toDate());
        return String.format("%s - %s, %s", start, end, dayOfWeek);
    }
}