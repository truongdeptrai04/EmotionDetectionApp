package com.example.smartclassemotion.database;


import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.example.smartclassemotion.models.Alert;
import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.models.StudentClasses;
import com.example.smartclassemotion.utils.ClassListCallback;
import com.example.smartclassemotion.utils.EmotionStatsCallback;
import com.example.smartclassemotion.utils.FirebaseCallback;
import com.example.smartclassemotion.utils.OnDatesLoadedCallback;
import com.example.smartclassemotion.utils.OnEmotionStatsLoadedCallback;
import com.example.smartclassemotion.utils.OnImageUploadedCallback;
import com.example.smartclassemotion.utils.OnMaxClassIdCallback;
import com.example.smartclassemotion.utils.OnMaxStudentIdCallback;
import com.example.smartclassemotion.utils.OnMultipleImagesUploadedCallback;
import com.example.smartclassemotion.utils.OnOperationCompleteCallback;
import com.example.smartclassemotion.utils.OnStudentCountCallback;
import com.example.smartclassemotion.utils.OnStudentEmotionStatsLoadedCallback;
import com.example.smartclassemotion.utils.OnStudentSessionEmotionStatsCallback;
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
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String SERVER_URL = "https://5433-58-187-196-90.ngrok-free.app/add_student_with_images";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;

    public FirebaseHelper(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();

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
        // Chuyển Student object thành Map để update
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("studentId", student.getStudentId());
        studentData.put("studentCode", student.getStudentCode());
        studentData.put("studentName", student.getStudentName());
        studentData.put("dateOfBirth", student.getDateOfBirth());
        studentData.put("gender", student.getGender());
        studentData.put("email", student.getEmail());
        studentData.put("phone", student.getPhone());
        studentData.put("avatarUrl", student.getAvatarUrl());
        studentData.put("status", student.getStatus());
        studentData.put("notes", student.getNotes());
        studentData.put("userId", student.getUserId());

        // Kiểm tra document tồn tại
        db.collection("Students").document(student.getStudentId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document đã tồn tại (do server tạo), dùng update
                        db.collection("Students")
                                .document(student.getStudentId())
                                .update(studentData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Cập nhật học sinh thành công: " + student.getStudentId());
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi khi cập nhật học sinh: " + e.getMessage(), e);
                                    callback.onFailure(e);
                                });
                    } else {
                        // Document chưa tồn tại, dùng set với merge
                        db.collection("Students")
                                .document(student.getStudentId())
                                .set(studentData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Thêm học sinh thành công: " + student.getStudentId());
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi khi thêm học sinh: " + e.getMessage(), e);
                                    callback.onFailure(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi kiểm tra document: " + e.getMessage(), e);
                    // Thử set với merge như dự phòng
                    db.collection("Students")
                            .document(student.getStudentId())
                            .set(studentData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Thêm học sinh thành công (dự phòng): " + student.getStudentId());
                                callback.onSuccess();
                            })
                            .addOnFailureListener(err -> {
                                Log.e(TAG, "Lỗi khi thêm học sinh (dự phòng): " + err.getMessage(), err);
                                callback.onFailure(err);
                            });
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
    public void getAvailableDates(String classId, OnDatesLoadedCallback callback) {
        List<String> dates = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        db.collection("ClassEmotionStats")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Timestamp createAt = doc.getTimestamp("createAt");
                        if (createAt != null) {
                            String date = dateFormat.format(createAt.toDate());
                            if (!dates.contains(date)) {
                                dates.add(date);
                            }
                        }
                    }
                    callback.onDatesLoaded(dates);
                })
                .addOnFailureListener(e -> {
                    callback.onDatesLoaded(new ArrayList<>());
                });
    }

    public void getEmotionStatsByDate(String classId, String selectedDate, OnEmotionStatsLoadedCallback callback) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(dateFormat.parse(selectedDate));
        } catch (Exception e) {
            callback.onEmotionStatsLoaded(new HashMap<>());
            return;
        }

        Calendar start = (Calendar) calendar.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) calendar.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        db.collection("ClassEmotionStats")
                .whereEqualTo("classId", classId)
                .whereGreaterThanOrEqualTo("createAt", new Timestamp(start.getTime()))
                .whereLessThanOrEqualTo("createAt", new Timestamp(end.getTime()))
                .orderBy("createAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Float> emotionStats = new HashMap<>();
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        emotionStats.put("happy", doc.getDouble("happy") != null ? doc.getDouble("happy").floatValue() : 0f);
                        emotionStats.put("sad", doc.getDouble("sad") != null ? doc.getDouble("sad").floatValue() : 0f);
                        emotionStats.put("angry", doc.getDouble("angry") != null ? doc.getDouble("angry").floatValue() : 0f);
                        emotionStats.put("neutral", doc.getDouble("neutral") != null ? doc.getDouble("neutral").floatValue() : 0f);
                        emotionStats.put("fear", doc.getDouble("fear") != null ? doc.getDouble("fear").floatValue() : 0f);
                        emotionStats.put("surprise", doc.getDouble("surprise") != null ? doc.getDouble("surprise").floatValue() : 0f);
                    }
                    callback.onEmotionStatsLoaded(emotionStats);
                })
                .addOnFailureListener(e -> {
                    callback.onEmotionStatsLoaded(new HashMap<>());
                });
    }

    public void getStudentEmotionStatsByDate(String classId, String selectedDate, OnStudentEmotionStatsLoadedCallback callback) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(dateFormat.parse(selectedDate));
        } catch (Exception e) {
            callback.onStudentEmotionStatsLoaded(new ArrayList<>(), new ArrayList<>());
            return;
        }

        Calendar start = (Calendar) calendar.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) calendar.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        // Lấy danh sách học sinh trong lớp
        List<Student> students = new ArrayList<>();
        List<Map<String, Float>> emotionStatsList = new ArrayList<>();

        db.collection("StudentClasses")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(classSnapshot -> {
                    List<String> studentIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : classSnapshot) {
                        String studentId = doc.getString("studentId");
                        if (studentId != null) {
                            studentIds.add(studentId);
                        }
                    }

                    if (studentIds.isEmpty()) {
                        callback.onStudentEmotionStatsLoaded(students, emotionStatsList);
                        return;
                    }

                    // Lấy thông tin học sinh
                    db.collection("Students")
                            .whereIn("studentId", studentIds)
                            .get()
                            .addOnSuccessListener(studentSnapshot -> {
                                for (QueryDocumentSnapshot doc : studentSnapshot) {
                                    Student student = doc.toObject(Student.class);
                                    students.add(student);
                                }

                                // Lấy báo cáo cảm xúc từ StudentSessionEmotionReports
                                db.collection("StudentSessionEmotionReports")
                                        .whereEqualTo("classId", classId)
                                        .whereGreaterThanOrEqualTo("createAt", new Timestamp(start.getTime()))
                                        .whereLessThanOrEqualTo("createAt", new Timestamp(end.getTime()))
                                        .get()
                                        .addOnSuccessListener(reportSnapshot -> {
                                            Map<String, Map<String, Float>> studentEmotionMap = new HashMap<>();
                                            for (QueryDocumentSnapshot doc : reportSnapshot) {
                                                String studentId = doc.getString("studentId");
                                                Map<String, Float> emotionStats = new HashMap<>();
                                                emotionStats.put("happy", doc.getDouble("happy") != null ? doc.getDouble("happy").floatValue() : 0f);
                                                emotionStats.put("sad", doc.getDouble("sad") != null ? doc.getDouble("sad").floatValue() : 0f);
                                                emotionStats.put("angry", doc.getDouble("angry") != null ? doc.getDouble("angry").floatValue() : 0f);
                                                emotionStats.put("neutral", doc.getDouble("neutral") != null ? doc.getDouble("neutral").floatValue() : 0f);
                                                emotionStats.put("fear", doc.getDouble("fear") != null ? doc.getDouble("fear").floatValue() : 0f);
                                                emotionStats.put("surprise", doc.getDouble("surprise") != null ? doc.getDouble("surprise").floatValue() : 0f);
                                                studentEmotionMap.put(studentId, emotionStats);
                                            }

                                            // Đồng bộ emotionStatsList với students
                                            for (Student student : students) {
                                                Map<String, Float> emotionStats = studentEmotionMap.getOrDefault(student.getStudentId(), null);
                                                if (emotionStats == null) {
                                                    emotionStats = new HashMap<>();
                                                    emotionStats.put("happy", 0f);
                                                    emotionStats.put("sad", 0f);
                                                    emotionStats.put("angry", 0f);
                                                    emotionStats.put("neutral", 0f);
                                                    emotionStats.put("fear", 0f);
                                                    emotionStats.put("surprise", 0f);
                                                }
                                                emotionStatsList.add(emotionStats);
                                            }

                                            callback.onStudentEmotionStatsLoaded(students, emotionStatsList);
                                        })
                                        .addOnFailureListener(e -> {
                                            callback.onStudentEmotionStatsLoaded(students, new ArrayList<>());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                callback.onStudentEmotionStatsLoaded(new ArrayList<>(), new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    callback.onStudentEmotionStatsLoaded(new ArrayList<>(), new ArrayList<>());
                });
    }
    public void getStudentSessionEmotionStats(String studentId, String classId, OnStudentSessionEmotionStatsCallback callback) {
        db.collection("StudentSessionEmotionReports")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Float> emotionStats = new HashMap<>();
                    emotionStats.put("happy", 0f);
                    emotionStats.put("sad", 0f);
                    emotionStats.put("angry", 0f);
                    emotionStats.put("neutral", 0f);
                    emotionStats.put("fear", 0f);
                    emotionStats.put("surprise", 0f);

                    if (querySnapshot.isEmpty()) {
                        callback.onStudentSessionEmotionStatsLoaded(emotionStats);
                        return;
                    }

                    int count = 0;
                    float happySum = 0f, sadSum = 0f, angrySum = 0f, neutralSum = 0f, fearSum = 0f, surpriseSum = 0f;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        happySum += doc.getDouble("happy") != null ? doc.getDouble("happy").floatValue() : 0f;
                        sadSum += doc.getDouble("sad") != null ? doc.getDouble("sad").floatValue() : 0f;
                        angrySum += doc.getDouble("angry") != null ? doc.getDouble("angry").floatValue() : 0f;
                        neutralSum += doc.getDouble("neutral") != null ? doc.getDouble("neutral").floatValue() : 0f;
                        fearSum += doc.getDouble("fear") != null ? doc.getDouble("fear").floatValue() : 0f;
                        surpriseSum += doc.getDouble("surprise") != null ? doc.getDouble("surprise").floatValue() : 0f;
                        count++;
                    }

                    if (count > 0) {
                        emotionStats.put("happy", happySum / count);
                        emotionStats.put("sad", sadSum / count);
                        emotionStats.put("angry", angrySum / count);
                        emotionStats.put("neutral", neutralSum / count);
                        emotionStats.put("fear", fearSum / count);
                        emotionStats.put("surprise", surpriseSum / count);
                    }

                    callback.onStudentSessionEmotionStatsLoaded(emotionStats);
                })
                .addOnFailureListener(e -> {
                    Map<String, Float> emotionStats = new HashMap<>();
                    emotionStats.put("happy", 0f);
                    emotionStats.put("sad", 0f);
                    emotionStats.put("angry", 0f);
                    emotionStats.put("neutral", 0f);
                    emotionStats.put("fear", 0f);
                    emotionStats.put("surprise", 0f);
                    callback.onStudentSessionEmotionStatsLoaded(emotionStats);
                });
    }

    public void uploadMultipleImages(String studentId, String studentName, List<Uri> imageUris, OnMultipleImagesUploadedCallback callback) {
        AtomicInteger uploadCount = new AtomicInteger(0);
        int totalImages = imageUris.size();

        if (totalImages == 0) {
            Log.w(TAG, "Danh sách ảnh rỗng");
            callback.onImagesUploaded();
            return;
        }

        // Chuyển tất cả ảnh thành base64
        List<String> base64Images = new ArrayList<>();
        for (int i = 0; i < totalImages; i++) {
            String base64 = convertImageToBase64(imageUris.get(i));
            if (base64 != null) {
                base64Images.add(base64);
            } else {
                base64Images.add(null);
                Log.e(TAG, "Không thể chuyển ảnh thành base64 tại index: " + i);
            }
        }

        // Tạo payload JSON
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", studentId);
        payload.put("studentName", studentName);
        payload.put("images", base64Images);

        String jsonPayload = gson.toJson(payload);
        RequestBody requestBody = RequestBody.create(jsonPayload, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(SERVER_URL + "?studentId=" + studentId)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Gửi ảnh thành công cho học sinh " + studentName);
                    callback.onImagesUploaded();
                } else {
                    Log.e(TAG, "Phản hồi server thất bại: " + response.code() + " - " + response.message());
                    callback.onError(new Exception("Server trả về mã lỗi: " + response.code()));
                }
                response.close();
            }

            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "Lỗi kết nối server: " + e.getMessage(), e);
                callback.onError(e);
            }
        });
    }

    public String convertImageToBase64(Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // Nén ảnh
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Chất lượng 70%
            byte[] bytes = baos.toByteArray();

            // Chuyển thành base64
            String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
            Log.d(TAG, "Base64 length: " + base64.length() + " for uri: " + uri);
            return base64;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi chuyển ảnh thành base64: " + e.getMessage(), e);
            return null;
        }
    }

}