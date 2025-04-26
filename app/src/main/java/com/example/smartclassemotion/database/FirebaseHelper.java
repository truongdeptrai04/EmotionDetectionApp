package com.example.smartclassemotion.database;

import android.net.Uri;
import android.util.Log;

import com.example.smartclassemotion.models.Alert;
import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.models.StudentClasses;
import com.example.smartclassemotion.utils.ClassListCallback;
import com.example.smartclassemotion.utils.EmotionStatsCallback;
import com.example.smartclassemotion.utils.FirebaseCallback;
import com.example.smartclassemotion.utils.OnImageUploadedCallback;
import com.example.smartclassemotion.utils.OnMaxClassIdCallback;
import com.example.smartclassemotion.utils.OnMaxStudentIdCallback;
import com.example.smartclassemotion.utils.OnOperationCompleteCallback;
import com.example.smartclassemotion.utils.OnStudentCountCallback;
import com.example.smartclassemotion.utils.StudentEmotionStatsCallback;
import com.example.smartclassemotion.utils.StudentListCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
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

                        db.collection("User").document(userId).set(userData)
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
        db.collection("Students")
                .whereEqualTo("userId", userId)
                .orderBy("studentName", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Student> students = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Student student = doc.toObject(Student.class);
                        students.add(student);
                    }
                    Log.d(TAG, "Students loaded: " + students.size() + " for userId: " + userId);
                    callback.onStudentsLoaded(students);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to load students: " + e.getMessage());
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
        WriteBatch batch = db.batch();

        db.collection("StudentClasses")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(querySnapshot ->{
                    for(QueryDocumentSnapshot doc : querySnapshot){
                        batch.delete(doc.getReference());
                    }
                    db.collection("StudentEmotionStats")
                            .whereEqualTo("studentId", studentId)
                            .get()
                            .addOnSuccessListener(statsSnapshot ->{
                                for (QueryDocumentSnapshot doc : statsSnapshot) {
                                    batch.delete(doc.getReference());
                                }

                                batch.delete(db.collection("Students").document(studentId));

                                batch.commit()
                                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("FirebaseHelper", "Failed to delete student: " + e.getMessage());
                                            callback.onFailure(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirebaseHelper", "Failed to query StudentEmotionStats: " + e.getMessage());
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to query StudentClasses: " + e.getMessage());
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

    public void getMaxClassId(String userId, OnMaxClassIdCallback callback){
        db.collection("Classes")
                .whereEqualTo("userId", userId)
                .orderBy("classId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(queryDocumentSnapshots.isEmpty()){
                        callback.onMaxClassIdFount(0);
                    }else{
                        String maxClassId = queryDocumentSnapshots.getDocuments().get(0).getString("classId");
                        if(maxClassId != null && maxClassId.matches("cl_\\d{3}")){
                            int maxNumber = Integer.parseInt(maxClassId.substring(3));
                            callback.onMaxClassIdFount(maxNumber);
                        }else{
                            callback.onMaxClassIdFount(0);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get max class ID: " + e.getMessage());
                    callback.onError(e);
                });
    }
    public void getMaxStudentId(String userId, OnMaxStudentIdCallback callback) {
        db.collection("Students")
                .whereEqualTo("userId", userId)
                .orderBy("studentId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onMaxStudentIdFound(0);
                    } else {
                        String maxStudentId = queryDocumentSnapshots.getDocuments().get(0).getString("studentId");
                        if (maxStudentId != null && maxStudentId.matches("std_\\d{3}")) {
                            int maxNumber = Integer.parseInt(maxStudentId.substring(4));
                            callback.onMaxStudentIdFound(maxNumber);
                        } else {
                            callback.onMaxStudentIdFound(0);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get max student ID: " + e.getMessage());
                    callback.onError(e);
                });
    }

    public void addClass(Map<String, Object> classData, String baseClassId, int startNumber, OnOperationCompleteCallback callback) {
        int attemptNumber = startNumber;
        String classId = String.format("cl_%03d", attemptNumber);

        DocumentReference classRef = db.collection("Classes").document(classId);
        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(classRef);
                    if (snapshot.exists()) {
                        // Nếu classId đã tồn tại, ném lỗi để thử số tiếp theo
                        throw new FirebaseFirestoreException("Class ID đã tồn tại",
                                FirebaseFirestoreException.Code.ABORTED);
                    }
                    classData.put("classId", classId); // Đảm bảo classId được lưu
                    transaction.set(classRef, classData);
                    return classId;
                })
                .addOnSuccessListener(result -> {
                    Log.d("FirebaseHelper", "Thêm lớp thành công: " + classId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (e.getMessage().equals("Class ID đã tồn tại")) {
                        // Thử classId tiếp theo
                        addClass(classData, baseClassId, attemptNumber + 1, callback);
                    } else {
                        Log.e("FirebaseHelper", "Lỗi khi thêm lớp: " + e.getMessage());
                        callback.onFailure(e);
                    }
                });
    }
    public void addAlert(String title, String content, String classId, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        Alert alert = new Alert(title, content, Timestamp.now(), classId);
        db.collection("Alerts")
                .add(alert)
                .addOnSuccessListener(documentReference -> successListener.onSuccess(null))
                .addOnFailureListener(failureListener);
    }
    public void deleteClass(String classId, OnOperationCompleteCallback callback){
        WriteBatch batch = db.batch();

        batch.delete(db.collection("Classes").document(classId));
        db.collection("StudentClasses")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(querySnapshot ->{
                    for(QueryDocumentSnapshot doc : querySnapshot){
                        batch.delete(doc.getReference());
                    }
                    db.collection("ClassEmotionStats")
                            .whereEqualTo("classId", classId)
                            .get()
                            .addOnSuccessListener(statSnapshot ->{
                                for(QueryDocumentSnapshot doc : statSnapshot){
                                    batch.delete(doc.getReference());
                                }

                                db.collection("Alerts")
                                        .whereEqualTo("classId", classId)
                                        .get()
                                        .addOnSuccessListener(alertSnapshot -> {
                                            for (QueryDocumentSnapshot doc : alertSnapshot) {
                                                batch.delete(doc.getReference());
                                            }
                                            db.collection("StudentEmotionStats")
                                                    .whereEqualTo("classId", classId)
                                                    .get()
                                                    .addOnSuccessListener(studentStatsSnapshot ->{
                                                        for (QueryDocumentSnapshot doc : studentStatsSnapshot) {
                                                            batch.delete(doc.getReference());
                                                        }
                                                        batch.commit()
                                                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                                                .addOnFailureListener(e -> {
                                                                    android.util.Log.e("FirebaseHelper", "Failed to delete class: " + e.getMessage());
                                                                    callback.onFailure(e);
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        android.util.Log.e("FirebaseHelper", "Failed to delete student stats: " + e.getMessage());
                                                        callback.onFailure(e);
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("FirebaseHelper", "Failed to delete alerts: " + e.getMessage());
                                            callback.onFailure(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("FirebaseHelper", "Failed to delete class stats: " + e.getMessage());
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseHelper", "Failed to delete student classes: " + e.getMessage());
                    callback.onFailure(e);
                });
    }
}