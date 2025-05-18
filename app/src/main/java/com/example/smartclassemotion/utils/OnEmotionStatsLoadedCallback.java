package com.example.smartclassemotion.utils;

import java.util.Map;

public interface OnEmotionStatsLoadedCallback {
    void onEmotionStatsLoaded(Map<String, Float> emotionStats);
}
