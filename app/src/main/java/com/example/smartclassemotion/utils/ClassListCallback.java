package com.example.smartclassemotion.utils;

import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.models.Student;

import java.util.List;

public interface ClassListCallback {
    void onClassesLoaded(List<ClassItem> classes);
}
