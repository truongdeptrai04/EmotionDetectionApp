package com.example.smartclassemotion.database;

import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.utils.ClassListCallback;
import com.example.smartclassemotion.utils.EmotionStatsCallback;
import com.example.smartclassemotion.utils.FirebaseCallback;
import com.example.smartclassemotion.utils.StudentEmotionStatsCallback;
import com.example.smartclassemotion.utils.StudentListCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FirebaseHelper {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public FirebaseHelper() {
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb(){
        return db;
    }

    public void signUp(String username, String email, String password, FirebaseCallback callback){
        if (username == null || email == null || password == null) {
            callback.onSuccess(false, null);
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", userId);
                        userData.put("username", username);
                        userData.put("email", email);
                        userData.put("role", "teacher");

                        db.collection("User").document(userId).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    callback.onSuccess(true, userId); // Chuyển Fragment chỉ khi Firestore thành công
                                })
                                .addOnFailureListener(e -> {
                                    // Log lỗi và gọi callback với thất bại
                                    android.util.Log.e("FirebaseHelper", "Failed to save user data: " + e.getMessage());
                                    callback.onSuccess(false, null);
                                });
                    }else{
                        android.util.Log.e("FirebaseHelper", "Sign up failed: " + task.getException().getMessage());
                        callback.onSuccess(false, null);
                    }
        });
    }

    public void login(String email, String password, FirebaseCallback callback){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        String userId = mAuth.getCurrentUser().getUid();
                        callback.onSuccess(true, userId);
                    }else{
                        callback.onSuccess(false, null);
                    }
                });
    }

    public void signOut(){
        mAuth.signOut();
    }
    public String getCurrentUserId(){
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    public void getAllStudentsByUserId(String userId, StudentListCallback callback) {
        db.collection("Classes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(classSnapshot ->{
                    List<String> classIds = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : classSnapshot){
                        classIds.add(doc.getId());
                    }
                    if(classIds.isEmpty()){
                        android.util.Log.d("FirebaseHelper", "No classes found for user: " + userId);
                        callback.onStudentsLoaded(new ArrayList<>());
                        return;
                    }

                    db.collection("Students")
                            .whereIn("classId", classIds)
                            .orderBy("studentName")
                            .get()
                            .addOnSuccessListener(studentSnapshot -> {
                                List<Student> students = studentSnapshot.toObjects(Student.class);
                                android.util.Log.d("FirebaseHelper", "Students loaded: " + students.size() + " students for userId: " + userId);
                                callback.onStudentsLoaded(students);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirebaseHelper", "Failed to load students: " + e.getMessage());
                                callback.onStudentsLoaded(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load classes: " + e.getMessage());
                    callback.onStudentsLoaded(new ArrayList<>());
                });
    }

    public void getStudents(String classId, StudentListCallback callback) {
        db.collection("Students")
                .whereEqualTo("classId", classId)
                .orderBy("studentName")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Student> students = querySnapshot.toObjects(Student.class);
                    callback.onStudentsLoaded(students);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load students: " + e.getMessage());
                    callback.onStudentsLoaded(new ArrayList<>());
                });
    }
    public void deleteStudent(String studentId, FirebaseCallback callback) {
        db.collection("Students").document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(true, null))
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to delete student: " + e.getMessage());
                    callback.onSuccess(false, null);
                });
    }
    public void getStudentEmotionStats(String classId, StudentEmotionStatsCallback callback) {
        List<Student> students = new ArrayList<>();
        List<Map<String, Float>> allEmotionStats = new ArrayList<>();
        db.collection("Students")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(studentSnapshot -> {
                    for(DocumentSnapshot doc : studentSnapshot){
                        Student student = doc.toObject(Student.class);
                        students.add(student);
                    }
                    db.collection("StudentEmotionStats")
                            .whereEqualTo("classId", classId)
                            .get()
                            .addOnSuccessListener(statsSnapshot ->{
                                for (DocumentSnapshot doc: statsSnapshot){
                                    Map<String, Float> stats = new HashMap<>();
                                    stats.put("happy", doc.getDouble("happy").floatValue());
                                    stats.put("sad", doc.getDouble("sad").floatValue());
                                    stats.put("angry", doc.getDouble("angry").floatValue());
                                    stats.put("neutral", doc.getDouble("neutral").floatValue());
                                    allEmotionStats.add(stats);
                                }
                                callback.onStatsLoaded(students, allEmotionStats);
                            })
                            .addOnFailureListener(e -> callback.onStatsLoaded(students, new ArrayList<>()));
                    })
                    .addOnFailureListener(e -> callback.onStatsLoaded(new ArrayList<>(), new ArrayList<>()));
    }

    public void getEmotionStats(String classId, EmotionStatsCallback callback){
        db.collection("ClassEmotionStats")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(querySnapshot ->{
                    Map<String, Float> emotionStats = new HashMap<>();
                    if(!querySnapshot.isEmpty()){
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        emotionStats.put("happy", doc.getDouble("happy").floatValue());
                        emotionStats.put("sad", doc.getDouble("sad").floatValue());
                        emotionStats.put("angry", doc.getDouble("angry").floatValue());
                        emotionStats.put("neutral", doc.getDouble("neutral").floatValue());
                    }
                    callback.onStatsLoaded(emotionStats);
                })
                .addOnFailureListener( e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load emotions: " + e.getMessage());
                    callback.onStatsLoaded(new HashMap<>());
                });
    }
    public void getClasses(String userId, ClassListCallback callback){
        db.collection("Classes")
                .whereEqualTo("userId", userId)
                .orderBy("className")
                .get()
                .addOnSuccessListener(querySnapshot ->{
                    List<ClassItem> classes = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : querySnapshot){
                        String classId = doc.getId();
                        String className = doc.getString("className");
                        Long studentCount=  doc.getLong("studentCount");
                        String time = doc.getString("time");
                        String emotion = doc.getString("emotion");
                        classes.add(new ClassItem(classId, className, studentCount != null ? studentCount.intValue() : 0, time, emotion));
                    }
                    callback.onClassesLoaded(classes);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load classes: " + e.getMessage());
                    callback.onClassesLoaded(new ArrayList<>());
                });
    }
}

