package com.example.smartclassemotion.view;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentStudentProfileBinding;
import com.example.smartclassemotion.models.Student;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.eazegraph.lib.models.PieModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentProfileFragment extends Fragment {
    private static final String TAG = "StudentProfileFragment";
    private FragmentStudentProfileBinding binding;
    private FirebaseHelper firebaseHelper;
    private String studentId;
    private String userId;
    private Student student;
    private boolean isEditMode = false;
    private List<String> className;
    private List<String> classIds;
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        className = new ArrayList<>();
        classIds = new ArrayList<>();
        className.add("All Classes");
        classIds.add("");

        // Nhận studentId và userId từ Bundle
        if (getArguments() != null) {
            studentId = getArguments().getString("student_id");
            userId = getArguments().getString("user_id");
            Log.d(TAG, "Received student_id: " + studentId + ", user_id: " + userId);
        }

        if (studentId == null) {
            Log.e(TAG, "Student ID is null");
            Toast.makeText(getContext(), "Student ID not found", Toast.LENGTH_SHORT).show();
            return root;
        }

        binding.tvNamestudent.setText("Loading...");
        binding.tvMood.setText("Loading...");
        binding.tvGender.setText("Loading...");
        binding.tvPhone.setText("Loading...");
        binding.tvEmail.setText("Loading...");
        binding.edtNote.setText("");

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.status_list)){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView)view;
                if (position == 0) {
                    textView.setTextColor(0xFF000000);
                } else {
                    textView.setTextColor(0xFFFF0000);
                }
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setTextColor(0xFF000000); // Màu đen
                } else {
                    textView.setTextColor(0xFFFF0000); // Màu đỏ
                }
                return view;
            }
        };

        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.statusSpinner.setAdapter(statusAdapter);

        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                className
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerClass.setAdapter(spinnerAdapter);

        loadStudentData();
        setupClassSpinner();
        setupMenuBar();
        setupEditMode();
        return root;
    }

    private void loadStudentData() {
        Log.d(TAG, "Attempting to load student data for student_id: " + studentId);
        firebaseHelper.getDb().collection("Students").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        student = documentSnapshot.toObject(Student.class);
                        if (student != null) {
                            Log.d(TAG, "Student loaded: " + student.getStudentName() + ", data: " + documentSnapshot.getData());
                            // Hiển thị thông tin học sinh
                            binding.tvNamestudent.setText(student.getStudentName() != null ? student.getStudentName() : "N/A");
                            binding.tvGender.setText(student.getGender() != null ? student.getGender() : "N/A");
                            binding.tvPhone.setText(student.getPhone() != null ? student.getPhone() : "N/A");
                            binding.tvEmail.setText(student.getEmail() != null ? student.getEmail() : "N/A");
                            binding.edtNote.setText(student.getNotes() != null ? student.getNotes() : "");
                            String status = student.getStatus() != null ? student.getStatus() : "Active";
                            binding.statusSpinner.setSelection(status.equals("Active") ? 0 : 1);
                            binding.statusSpinner.setEnabled(false);
                            updateEmotionChart("");
                        } else {
                            Log.e(TAG, "Failed to parse student object from Firestore");
                            Toast.makeText(getContext(), "Failed to parse student data", Toast.LENGTH_SHORT).show();
                            resetFields();
                        }
                    } else {
                        Log.e(TAG, "Student document does not exist for student_id: " + studentId);
                        Toast.makeText(getContext(), "Student not found in database", Toast.LENGTH_SHORT).show();
                        resetFields();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load student data: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to load student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetFields();
                });
    }

    private void resetFields() {
        binding.tvNamestudent.setText("N/A");
        binding.tvMood.setText("N/A");
        binding.tvGender.setText("N/A");
        binding.tvPhone.setText("N/A");
        binding.tvEmail.setText("N/A");
        binding.edtNote.setText("");
        binding.statusSpinner.setSelection(0);
    }

    private void setupClassSpinner() {
        if (userId == null) {
            Log.e(TAG, "User ID is null, cannot load classes");
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Setting up class spinner for user_id: " + userId);
        firebaseHelper.getDb().collection("Classes")
                .whereEqualTo("userId", userId)
                .orderBy("className")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    className.clear();
                    classIds.clear();
                    className.add("All Classes");
                    classIds.add("");
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String className1 = doc.getString("className");
                        String classId = doc.getId();
                        if (className != null && classId != null) {
                            className.add(className1);
                            classIds.add(classId);
                        }
                    }
                    spinnerAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Class list loaded: " + className.size() + " classes");
                    if(!classIds.isEmpty()){
                        updateEmotionChart("");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load classes: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to load classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        binding.spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (className.isEmpty()) {
                    Log.w(TAG, "Spinner selected but className is empty");
                    return;
                }
                String selectedClassId = classIds.get(position);
                Log.d(TAG, "Spinner selected: " + selectedClassId);
                updateEmotionChart(selectedClassId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Spinner nothing selected");
            }
        });
    }

    private void updateEmotionChart(String classId) {
        binding.chartEmotion.clearChart();
        if(studentId == null){
            Log.e(TAG, "Student ID is null, cannot update emotion chart");
            return;
        }

        Query query = firebaseHelper.getDb().collection("StudentEmotionStats")
                .whereEqualTo("studentId", studentId)
                .orderBy("createAt", Query.Direction.DESCENDING)
                .limit(1);
        if (!classId.isEmpty()) {
            query = query.whereEqualTo("classId", classId);
        }

        query.get()
                .addOnSuccessListener(querySnapshot ->{
                    if(!querySnapshot.isEmpty()){
                        Map<String, Object> data = querySnapshot.getDocuments().get(0).getData();
                        float happy = data.get("happy") != null ? ((Number) data.get("happy")).floatValue() : 0f;
                        float sad = data.get("sad") != null ? ((Number) data.get("sad")).floatValue() : 0f;
                        float neutral = data.get("neutral") != null ? ((Number) data.get("neutral")).floatValue() : 0f;
                        float angry = data.get("angry") != null ? ((Number) data.get("angry")).floatValue() : 0f;
                        float fear = data.get("fear") != null ? ((Number) data.get("fear")).floatValue() : 0f;
                        float surprise = data.get("surprise") != null ? ((Number) data.get("surprise")).floatValue() : 0f;

                        if (happy > 0) binding.chartEmotion.addPieSlice(new PieModel("Happy", happy, 0xFF4CAF50));
                        if (neutral > 0) binding.chartEmotion.addPieSlice(new PieModel("Neutral", neutral, 0xFFFFC107));
                        if (sad > 0) binding.chartEmotion.addPieSlice(new PieModel("Sad", sad, 0xFFD81B60));
                        if (angry > 0) binding.chartEmotion.addPieSlice(new PieModel("Angry", angry, 0xFFE91E63));
                        if (fear > 0) binding.chartEmotion.addPieSlice(new PieModel("Fear", fear, 0xFF9C27B0));
                        if (surprise > 0) binding.chartEmotion.addPieSlice(new PieModel("Surprise", surprise, 0xFF2196F3));
                        Log.d(TAG, "Emotion stats loaded: happy=" + happy + ", sad=" + sad + ", neutral=" + neutral + ", angry=" + angry + ", fear=" + fear + ", surprise=" + surprise);
                    }else{
                        Log.d(TAG, "No emotion stats found for student_id: " + studentId + ", class_id: " + classId);
                        binding.chartEmotion.addPieSlice(new PieModel("No Data", 100, 0xFF9E9E9E));
                    }
                    binding.chartEmotion.startAnimation();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load emotion stats: " + e.getMessage(), e);
                    binding.chartEmotion.addPieSlice(new PieModel("Error", 100, 0xFF9E9E9E));
                    binding.chartEmotion.startAnimation();
                    Toast.makeText(getContext(), "Failed to load emotion stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupEditMode() {
        binding.btnSave.setOnClickListener(v -> {
            if (isEditMode) {
                saveStudentData();
            } else {
                isEditMode = true;
                toggleEditMode(true);
            }
        });
    }

    private void toggleEditMode(boolean enable) {
        binding.edtNamestudent.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvNamestudent.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.edtMood.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvMood.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.edtGender.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvGender.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.edtPhone.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvPhone.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.edtEmail.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvEmail.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.statusSpinner.setEnabled(enable);

        if (enable) {
            binding.edtNamestudent.setText(binding.tvNamestudent.getText());
            binding.edtMood.setText(binding.tvMood.getText());
            binding.edtGender.setText(binding.tvGender.getText());
            binding.edtPhone.setText(binding.tvPhone.getText());
            binding.edtEmail.setText(binding.tvEmail.getText());
            binding.btnSave.setImageResource(R.drawable.baseline_check_24_white);
        } else {
            binding.btnSave.setImageResource(R.drawable.ic_edit_white);
        }
    }

    private void saveStudentData() {
        if (student == null) {
            Toast.makeText(getContext(), "No student data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = binding.edtNamestudent.getText().toString().trim();
        String mood = binding.edtMood.getText().toString().trim();
        String gender = binding.edtGender.getText().toString().trim();
        String phone = binding.edtPhone.getText().toString().trim();
        String email = binding.edtEmail.getText().toString().trim();
        String notes = binding.edtNote.getText().toString().trim();
        String status = binding.statusSpinner.getSelectedItem() != null ? binding.statusSpinner.getSelectedItem().toString() : "Active";

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("studentName", name);
        updates.put("mood", mood);
        updates.put("gender", gender);
        updates.put("phone", phone);
        updates.put("email", email);
        updates.put("notes", notes);
        updates.put("status", status);

        firebaseHelper.getDb().collection("Students").document(studentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student updated: " + name);
                    Toast.makeText(getContext(), "Student updated successfully", Toast.LENGTH_SHORT).show();
                    isEditMode = false;
                    toggleEditMode(false);
                    binding.tvNamestudent.setText(name);
                    binding.tvMood.setText(mood);
                    binding.tvGender.setText(gender);
                    binding.tvPhone.setText(phone);
                    binding.tvEmail.setText(email);
                    binding.statusSpinner.setSelection(status.equals("Active") ? 0 : 1);
                    binding.statusSpinner.setEnabled(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update student: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to update student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupMenuBar() {
        NavController navController = NavHostFragment.findNavController(this);
        binding.menuClasses.setOnClickListener(v -> {
            if(userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            }else{
                navigateToHomeFragment(userId);
            }
        });

        binding.menuReports.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Reports clicked", Toast.LENGTH_SHORT).show();
        });

        binding.menuStudent.setOnClickListener(v -> {
            if(userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            }else{
                navigateToStudentFragment(userId);
            }
        });
        binding.menuSettings.setOnClickListener(v -> {
            if(userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            }else{
                navigateToSettingFragment(userId);
            }
        });
    }
    private void navigateToHomeFragment(String userId){
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_studentProfileFragment_to_homeFragment, bundle);
        Log.d(TAG, "Navigating to HomeFragment with userId: " + userId);
    }
    private void navigateToStudentFragment(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_studentProfileFragment_to_studentFragment, bundle);
        Log.d(TAG, "Navigating to StudentFragment with userId: " + userId);
    }
    private void navigateToSettingFragment(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_studentProfileFragment_to_settingFragment, bundle);
        Log.d(TAG, "Navigating to SettingFragment with userId: " + userId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}