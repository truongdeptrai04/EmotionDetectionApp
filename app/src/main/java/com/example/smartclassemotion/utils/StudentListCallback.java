package com.example.smartclassemotion.utils;

import com.example.smartclassemotion.models.Student;

import java.util.List;
import java.util.Map;

public interface StudentListCallback {
    void onStudentsLoaded(List<Student> students);
}
