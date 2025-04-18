package com.example.smartclassemotion.models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ClassEmotionStats {
    @PropertyName("classEmotionStatsId")
    private String classEmotionStatsId;

    @PropertyName("classId")
    private String classId;

    @PropertyName("angry")
    private long angry;

    @PropertyName("happy")
    private long happy;

    @PropertyName("neutral")
    private long neutral;

    @PropertyName("sad")
    private long sad;

    @PropertyName("fear")
    private long fear;

    @PropertyName("surprise")
    private long surprise;

    @PropertyName("totalDetectedStudents")
    private long totalDetectedStudents;

    @PropertyName("startTime")
    @ServerTimestamp
    private Date startTime;

    @PropertyName("endTime")
    @ServerTimestamp
    private Date endTime;

    @PropertyName("createAt")
    @ServerTimestamp
    private Date createAt;

    public ClassEmotionStats() {
    }

    public ClassEmotionStats(String classEmotionStatsId, String classId, long angry, long happy,
                             long neutral, long sad, long fear, long surprise,
                             long totalDetectedStudents, Date startTime, Date endTime, Date createAt) {
        this.classEmotionStatsId = classEmotionStatsId;
        this.classId = classId;
        this.angry = angry;
        this.happy = happy;
        this.neutral = neutral;
        this.sad = sad;
        this.fear = fear;
        this.surprise = surprise;
        this.totalDetectedStudents = totalDetectedStudents;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createAt = createAt;
    }

    @PropertyName("classEmotionStatsId")
    public String getClassEmotionStatsId() {
        return classEmotionStatsId;
    }

    @PropertyName("classEmotionStatsId")
    public void setClassEmotionStatsId(String classEmotionStatsId) {
        this.classEmotionStatsId = classEmotionStatsId;
    }

    @PropertyName("classId")
    public String getClassId() {
        return classId;
    }

    @PropertyName("classId")
    public void setClassId(String classId) {
        this.classId = classId;
    }

    @PropertyName("angry")
    public long getAngry() {
        return angry;
    }

    @PropertyName("angry")
    public void setAngry(long angry) {
        this.angry = angry;
    }

    @PropertyName("happy")
    public long getHappy() {
        return happy;
    }

    @PropertyName("happy")
    public void setHappy(long happy) {
        this.happy = happy;
    }

    @PropertyName("neutral")
    public long getNeutral() {
        return neutral;
    }

    @PropertyName("neutral")
    public void setNeutral(long neutral) {
        this.neutral = neutral;
    }

    @PropertyName("sad")
    public long getSad() {
        return sad;
    }

    @PropertyName("sad")
    public void setSad(long sad) {
        this.sad = sad;
    }

    @PropertyName("fear")
    public long getFear() {
        return fear;
    }

    @PropertyName("fear")
    public void setFear(long fear) {
        this.fear = fear;
    }

    @PropertyName("surprise")
    public long getSurprise() {
        return surprise;
    }

    @PropertyName("surprise")
    public void setSurprise(long surprise) {
        this.surprise = surprise;
    }

    @PropertyName("totalDetectedStudents")
    public long getTotalDetectedStudents() {
        return totalDetectedStudents;
    }

    @PropertyName("totalDetectedStudents")
    public void setTotalDetectedStudents(long totalDetectedStudents) {
        this.totalDetectedStudents = totalDetectedStudents;
    }

    @PropertyName("startTime")
    public Date getStartTime() {
        return startTime;
    }

    @PropertyName("startTime")
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @PropertyName("endTime")
    public Date getEndTime() {
        return endTime;
    }

    @PropertyName("endTime")
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @PropertyName("createAt")
    public Date getCreateAt() {
        return createAt;
    }

    @PropertyName("createAt")
    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }
}