package com.example.smartclassemotion.utils;

import java.util.Map;

public interface EmotionStatsCallback {
    void onStatsLoaded(Map<String, Float> emotionStats);
}
