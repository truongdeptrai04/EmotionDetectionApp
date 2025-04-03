package com.example.smartclassemotion.utils;

import com.example.smartclassemotion.models.Student;

public interface OnStudentActionListener {
    void onEdit(Student student);
    void onDelete(Student student);
}
