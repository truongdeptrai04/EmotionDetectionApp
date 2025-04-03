package com.example.smartclassemotion.models;

public class ClassItem {
    private String classId;
    private String className;
    private int studentCount;
    private String classTime;
    private String emotionRating;

    public ClassItem(String classId, String className, int studentCount, String classTime, String emotionRating) {
        this.classId = classId;
        this.className = className;
        this.studentCount = studentCount;
        this.classTime = classTime;
        this.emotionRating = emotionRating;
    }

    public String getClassName() { return className; }
    public int getStudentCount() { return studentCount; }
    public String getClassTime() { return classTime; }
    public String getEmotionRating() { return emotionRating; }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public void setEmotionRating(String emotionRating) {
        this.emotionRating = emotionRating;
    }

    public void setClassTime(String classTime) {
        this.classTime = classTime;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }
}