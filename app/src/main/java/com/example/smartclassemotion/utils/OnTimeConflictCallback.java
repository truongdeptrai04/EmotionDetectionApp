package com.example.smartclassemotion.utils;

public interface OnTimeConflictCallback {
    void onNoConflict();
    void onConflict(String conflictClassName);
    void onError(Exception e);
}
