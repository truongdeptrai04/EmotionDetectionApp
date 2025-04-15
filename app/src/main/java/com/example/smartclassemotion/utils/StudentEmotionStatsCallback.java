package com.example.smartclassemotion.utils;

import com.example.smartclassemotion.models.Student;

import java.util.List;
import java.util.Map;

public interface StudentEmotionStatsCallback {
    void onStatsLoaded(List<Student> students, List<Map<String, Float>> emotionStats);
}
