package com.example.smartclassemotion.models;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Alert {
    private String id;
    private String title;
    private String content;
    private Timestamp timestamp;
    private String classId;

    public Alert() {
    }

    public Alert(String title, String content, Timestamp timestamp, String classId) {
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.classId = classId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @PropertyName("timestamp")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    @PropertyName("timestamp")
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }
    public long getTimestampInMillis() {
        return timestamp != null ? timestamp.toDate().getTime() : System.currentTimeMillis();
    }
}

