package com.example.smartclassemotion.utils;

import com.example.smartclassemotion.models.Student;

import java.util.List;
import java.util.Map;

public interface OnStudentEmotionStatsLoadedCallback {
    void onStudentEmotionStatsLoaded(List<Student> students, List<Map<String, Float>> emotionStatsList);
}
