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

    public void loadAlerts() {
        listenerRegistration = db.collection("Alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        error.setValue("Lỗi khi tải thông báo: " + e.getMessage());
                        return;
                    }
                    if (snapshots != null) {
                        List<Alert> alertList = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Alert alert = doc.toObject(Alert.class);
                            if (alert != null) {
                                alert.setId(doc.getId());
                                alertList.add(alert);
                            }
                        }
                        alerts.setValue(alertList);
                    }
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