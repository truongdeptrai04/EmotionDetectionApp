package com.example.smartclassemotion.utils;

import com.example.smartclassemotion.models.ClassItem;

import java.util.List;

public interface ClassListCallback {
    void onClassesLoaded(List<ClassItem> classItems);
    void onError(String errorMessage);
}
