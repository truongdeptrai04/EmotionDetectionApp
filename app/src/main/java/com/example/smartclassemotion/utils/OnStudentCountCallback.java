package com.example.smartclassemotion.utils;

public interface OnStudentCountCallback {
    void onCountLoaded(long count);
    void onError(Exception e);
}
