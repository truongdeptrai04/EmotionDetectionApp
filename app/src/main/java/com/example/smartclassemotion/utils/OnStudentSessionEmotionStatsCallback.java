package com.example.smartclassemotion.utils;

import java.util.Map;

public interface OnStudentSessionEmotionStatsCallback {
    void onStudentSessionEmotionStatsLoaded(Map<String, Float> emotionStats);
}
