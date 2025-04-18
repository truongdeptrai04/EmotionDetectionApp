package com.example.smartclassemotion.database;

import android.net.Uri;
import android.util.Log;

import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.models.StudentClasses;
import com.example.smartclassemotion.utils.ClassListCallback;
import com.example.smartclassemotion.utils.EmotionStatsCallback;
import com.example.smartclassemotion.utils.FirebaseCallback;
import com.example.smartclassemotion.utils.OnImageUploadedCallback;
import com.example.smartclassemotion.utils.OnOperationCompleteCallback;
import com.example.smartclassemotion.utils.OnStudentCountCallback;
import com.example.smartclassemotion.utils.StudentEmotionStatsCallback;
import com.example.smartclassemotion.utils.StudentListCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    public FirebaseHelper() {
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public void signUp(String username, String email, String password, FirebaseCallback callback) {
        if (username == null || email == null || password == null) {
            callback.onSuccess(false, null);
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", userId);
                        userData.put("username", username);
                        userData.put("email", email);
                        userData.put("role", "teacher");

                        db.collection("Users").document(userId).set(userData)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(true, userId))
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("FirebaseHelper", "Failed to save user data: " + e.getMessage());
                                    callback.onSuccess(false, null);
                                });
                    } else {
                        android.util.Log.e("FirebaseHelper", "Sign up failed: " + task.getException().getMessage());
                        callback.onSuccess(false, null);
                    }
                });
    }

    public void login(String email, String password, FirebaseCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        callback.onSuccess(true, userId);
                    } else {
                        callback.onSuccess(false, null);
                    }
                });
    }

    public void signOut() {
        mAuth.signOut();
    }

    public String getCurrentUserId() {
        return mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
    }

    public void getAllStudentsByUserId(String userId, StudentListCallback callback) {
        db.collection("Classes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(classSnapshot -> {
                    List<String> classIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : classSnapshot) {
                        classIds.add(doc.getId());
                    }
                    if (classIds.isEmpty()) {
                        android.util.Log.d("FirebaseHelper", "No classes found for user: " + userId);
                        callback.onStudentsLoaded(new ArrayList<>());
                        return;
                    }

                    db.collection("StudentClasses")
                            .whereIn("classId", classIds)
                            .get()
                            .addOnSuccessListener(studentClassesSnapshot -> {
                                List<String> studentIds = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : studentClassesSnapshot) {
                                    String studentId = doc.getString("studentId");
                                    if (studentId != null) {
                                        studentIds.add(studentId);
                                    }
                                }
                                if (studentIds.isEmpty()) {
                                    android.util.Log.d("FirebaseHelper", "No students found for classes: " + classIds);
                                    callback.onStudentsLoaded(new ArrayList<>());
                                    return;
                                }

                                db.collection("Students")
                                        .whereIn("studentId", studentIds)
                                        .orderBy("studentName")
                                        .get()
                                        .addOnSuccessListener(studentSnapshot -> {
                                            List<Student> students = studentSnapshot.toObjects(Student.class);
                                            android.util.Log.d("FirebaseHelper", "Students loaded: " + students.size() + " for userId: " + userId);
                                            callback.onStudentsLoaded(students);
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("FirebaseHelper", "Failed to load students: " + e.getMessage());
                                            callback.onStudentsLoaded(new ArrayList<>());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirebaseHelper", "Failed to load student classes: " + e.getMessage());
                                callback.onStudentsLoaded(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load classes: " + e.getMessage());
                    callback.onStudentsLoaded(new ArrayList<>());
                });
    }

    public void getAllClassesByUserId(String userId, ClassListCallback callback){
        List<ClassItem> classes = new ArrayList<>();
        db.collection("Classes")
                .whereEqualTo("userId", userId)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots){
                        ClassItem classItem = doc.toObject(ClassItem.class);
                        classes.add(classItem);
                    }
                    callback.onClassesLoaded(classes);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load classes: " + e.getMessage());
                    callback.onClassesLoaded(new ArrayList<>());
                });
    }

    public void getStudentsByClassId(String classId, StudentListCallback callback) {
        db.collection("StudentClasses")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(studentClassesSnapshot -> {
                    List<String> studentIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : studentClassesSnapshot) {
                        String studentId = doc.getString("studentId");
                        if (studentId != null) {
                            studentIds.add(studentId);
                        }
                    }
                    if (studentIds.isEmpty()) {
                        android.util.Log.d("FirebaseHelper", "No students found for classId: " + classId);
                        callback.onStudentsLoaded(new ArrayList<>());
                        return;
                    }

                    db.collection("Students")
                            .whereIn("studentId", studentIds)
                            .orderBy("studentName")
                            .get()
                            .addOnSuccessListener(studentSnapshot -> {
                                List<Student> students = studentSnapshot.toObjects(Student.class);
                                android.util.Log.d("FirebaseHelper", "Students loaded: " + students.size() + " for classId: " + classId);
                                callback.onStudentsLoaded(students);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirebaseHelper", "Failed to load students: " + e.getMessage());
                                callback.onStudentsLoaded(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load student classes: " + e.getMessage());
                    callback.onStudentsLoaded(new ArrayList<>());
                });
    }

    public void deleteStudent(String studentId, OnOperationCompleteCallback callback) {
        db.collection("Students").document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to delete student: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    public void getStudentEmotionStats(String classId, StudentEmotionStatsCallback callback) {
        List<Student> students = new ArrayList<>();
        List<Map<String, Float>> allEmotionStats = new ArrayList<>();
        db.collection("StudentClasses")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(studentClassesSnapshot -> {
                    List<String> studentIds = new ArrayList<>();
                    for (DocumentSnapshot doc : studentClassesSnapshot) {
                        String studentId = doc.getString("studentId");
                        if (studentId != null) {
                            studentIds.add(studentId);
                        }
                    }
                    if (studentIds.isEmpty()) {
                        android.util.Log.d("FirebaseHelper", "No students found for classId: " + classId);
                        callback.onStatsLoaded(students, new ArrayList<>());
                        return;
                    }

                    db.collection("Students")
                            .whereIn("studentId", studentIds)
                            .get()
                            .addOnSuccessListener(studentSnapshot -> {
                                for (DocumentSnapshot doc : studentSnapshot) {
                                    Student student = doc.toObject(Student.class);
                                    if (student != null) {
                                        students.add(student);
                                    }
                                }
                                db.collection("StudentEmotionStats")
                                        .whereEqualTo("classId", classId)
                                        .orderBy("createAt", Query.Direction.DESCENDING)
                                        .get()
                                        .addOnSuccessListener(statsSnapshot -> {
                                            for (DocumentSnapshot doc : statsSnapshot) {
                                                Map<String, Float> stats = new HashMap<>();
                                                stats.put("happy", doc.getDouble("happy") != null ? doc.getDouble("happy").floatValue() : 0f);
                                                stats.put("sad", doc.getDouble("sad") != null ? doc.getDouble("sad").floatValue() : 0f);
                                                stats.put("angry", doc.getDouble("angry") != null ? doc.getDouble("angry").floatValue() : 0f);
                                                stats.put("neutral", doc.getDouble("neutral") != null ? doc.getDouble("neutral").floatValue() : 0f);
                                                stats.put("fear", doc.getDouble("fear") != null ? doc.getDouble("fear").floatValue() : 0f);
                                                stats.put("surprise", doc.getDouble("surprise") != null ? doc.getDouble("surprise").floatValue() : 0f);
                                                allEmotionStats.add(stats);
                                            }
                                            android.util.Log.d("FirebaseHelper", "Student emotion stats loaded: " + allEmotionStats.size() + " for classId: " + classId);
                                            callback.onStatsLoaded(students, allEmotionStats);
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("FirebaseHelper", "Failed to load student emotion stats: " + e.getMessage());
                                            callback.onStatsLoaded(students, new ArrayList<>());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirebaseHelper", "Failed to load students: " + e.getMessage());
                                callback.onStatsLoaded(new ArrayList<>(), new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load student classes: " + e.getMessage());
                    callback.onStatsLoaded(new ArrayList<>(), new ArrayList<>());
                });
    }

    public void getEmotionStats(String classId, EmotionStatsCallback callback) {
        db.collection("ClassEmotionStats")
                .whereEqualTo("classId", classId)
                .orderBy("createAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Float> emotionStats = new HashMap<>();
                    emotionStats.put("happy", 0f);
                    emotionStats.put("sad", 0f);
                    emotionStats.put("angry", 0f);
                    emotionStats.put("neutral", 0f);
                    emotionStats.put("fear", 0f);
                    emotionStats.put("surprise", 0f);

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        emotionStats.put("happy", doc.getDouble("happy") != null ? doc.getDouble("happy").floatValue() : 0f);
                        emotionStats.put("sad", doc.getDouble("sad") != null ? doc.getDouble("sad").floatValue() : 0f);
                        emotionStats.put("angry", doc.getDouble("angry") != null ? doc.getDouble("angry").floatValue() : 0f);
                        emotionStats.put("neutral", doc.getDouble("neutral") != null ? doc.getDouble("neutral").floatValue() : 0f);
                        emotionStats.put("fear", doc.getDouble("fear") != null ? doc.getDouble("fear").floatValue() : 0f);
                        emotionStats.put("surprise", doc.getDouble("surprise") != null ? doc.getDouble("surprise").floatValue() : 0f);
                    }
                    android.util.Log.d("FirebaseHelper", "Class emotion stats loaded: " + emotionStats + " for classId: " + classId);
                    callback.onStatsLoaded(emotionStats);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load emotions: " + e.getMessage());
                    callback.onStatsLoaded(new HashMap<>());
                });
    }

    public void getClasses(String userId, ClassListCallback callback) {
        db.collection("Classes")
                .whereEqualTo("userId", userId)
                .orderBy("className")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClassItem> classes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ClassItem classObj = doc.toObject(ClassItem.class);
                        classes.add(classObj);
                    }
                    android.util.Log.d("FirebaseHelper", "Classes loaded: " + classes.size() + " for userId: " + userId);
                    callback.onClassesLoaded(classes);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load classes: " + e.getMessage());
                    callback.onClassesLoaded(new ArrayList<>());
                });
    }

    public void getAllStudentCount(OnStudentCountCallback callback) {
        db.collection("Students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onCountLoaded(queryDocumentSnapshots.size());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to get student count: " + e.getMessage());
                    callback.onError(e);
                });
    }
    public void addStudent(Student student, OnOperationCompleteCallback callback) {
        db.collection("Students").document(student.getStudentId())
                .set(student)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to add student: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    public void addStudentClass(StudentClasses studentClasses, OnOperationCompleteCallback callback){
        db.collection("StudentClasses").document(studentClasses.getStudentClassesId())
                .set(studentClasses)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to add student class: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    public void uploadImage(Uri imageUri, String path, OnImageUploadedCallback callback){
        StorageReference storageRef = storage.getReference().child(path);
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onImageUploaded(uri.toString()))
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseHelper", "Failed to get download URL: " + e.getMessage());
                            callback.onError(e);
                        }))
                .addOnFailureListener(e -> {
                    Log.e("FirebaseHelper", "Failed to upload image: " + e.getMessage());
                    callback.onError(e);
                });
    }
}