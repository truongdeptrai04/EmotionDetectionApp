package com.example.smartclassemotion.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smartclassemotion.models.Alert;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AlertViewModel extends ViewModel {
    private final MutableLiveData<List<Alert>> alerts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration listenerRegistration;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Alert>> getAlerts() {
        return alerts;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadAlerts(String userId) {
        if (userId == null || userId.isEmpty()) {
            error.setValue("User ID is required");
            return;
        }

        // Bước 1: Lấy danh sách classId của userId từ collection Classes
        db.collection("Classes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> classIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String classId = doc.getString("classId");
                        if (classId != null) {
                            classIds.add(classId);
                        }
                    }

                    if (classIds.isEmpty()) {
                        alerts.setValue(new ArrayList<>());
                        return;
                    }

                    // Bước 2: Lọc Alerts dựa trên classIds
                    // Firestore whereIn giới hạn 10 mục, chia thành batch nếu cần
                    int batchSize = 10;
                    List<List<String>> batches = new ArrayList<>();
                    for (int i = 0; i < classIds.size(); i += batchSize) {
                        int end = Math.min(i + batchSize, classIds.size());
                        batches.add(classIds.subList(i, end));
                    }

                    // Đăng ký listener cho từng batch
                    List<Alert> allAlerts = new ArrayList<>();
                    listenerRegistration = db.collection("Alerts")
                            .whereIn("classId", batches.get(0)) // Chỉ xử lý batch đầu tiên để đơn giản
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener((snapshots, e) -> {
                                if (e != null) {
                                    error.setValue("Lỗi khi tải thông báo: " + e.getMessage());
                                    return;
                                }
                                if (snapshots != null) {
                                    allAlerts.clear();
                                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                        Alert alert = doc.toObject(Alert.class);
                                        if (alert != null) {
                                            alert.setId(doc.getId());
                                            allAlerts.add(alert);
                                        }
                                    }
                                    alerts.setValue(allAlerts);
                                }
                            });

                    // Nếu có nhiều batch, cần thêm logic để hợp nhất kết quả
                    // (Để đơn giản, hiện chỉ xử lý batch đầu tiên)
                })
                .addOnFailureListener(e -> {
                    error.setValue("Lỗi khi tải danh sách lớp: " + e.getMessage());
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}