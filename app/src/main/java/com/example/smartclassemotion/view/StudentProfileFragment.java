package com.example.smartclassemotion.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentStudentProfileBinding;
import com.example.smartclassemotion.models.Student;
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
    private List<String> classList;
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        classList = new ArrayList<>();
        classList.add("All Classes"); // Giá trị mặc định để tránh rỗng

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

        // Khởi tạo giao diện mặc định
        binding.tvNamestudent.setText("Loading...");
        binding.tvMood.setText("Loading...");
        binding.tvGender.setText("Loading...");
        binding.tvPhone.setText("Loading...");
        binding.tvEmail.setText("Loading...");
        binding.edtNote.setText("");

        // Khởi tạo Spinner adapter
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                classList
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerClass.setAdapter(spinnerAdapter);

        loadStudentData();
        setupClassSpinner();
        setupEmotionChart();
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
                            binding.tvMood.setText(student.getMood() != null ? student.getMood() : "Neutral");
                            binding.tvGender.setText(student.getGender() != null ? student.getGender() : "N/A");
                            binding.tvPhone.setText(student.getPhone() != null ? student.getPhone() : "N/A");
                            binding.tvEmail.setText(student.getEmail() != null ? student.getEmail() : "N/A");
                            binding.edtNote.setText(student.getNotes() != null ? student.getNotes() : "");
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
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    classList.clear();
                    classList.add("All Classes");
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String className = doc.getString("className");
                        if (className != null) {
                            classList.add(className);
                        }
                    }
                    spinnerAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Class list loaded: " + classList.size() + " classes");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load classes: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to load classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        binding.spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (classList.isEmpty()) {
                    Log.w(TAG, "Spinner selected but classList is empty");
                    return;
                }
                String selectedClass = classList.get(position);
                Log.d(TAG, "Spinner selected: " + selectedClass);
                updateEmotionChart(selectedClass);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Spinner nothing selected");
            }
        });
    }

    private void setupEmotionChart() {
        binding.chartEmotion.addPieSlice(new PieModel("Happy", 50, 0xFF4CAF50));
        binding.chartEmotion.addPieSlice(new PieModel("Neutral", 30, 0xFFFFC107));
        binding.chartEmotion.addPieSlice(new PieModel("Sad", 20, 0xFFD81B60));
        binding.chartEmotion.startAnimation();
    }

    private void updateEmotionChart(String selectedClass) {
        binding.chartEmotion.clearChart();
        if (selectedClass.equals("All Classes")) {
            binding.chartEmotion.addPieSlice(new PieModel("Happy", 50, 0xFF4CAF50));
            binding.chartEmotion.addPieSlice(new PieModel("Neutral", 30, 0xFFFFC107));
            binding.chartEmotion.addPieSlice(new PieModel("Sad", 20, 0xFFD81B60));
        } else {
            binding.chartEmotion.addPieSlice(new PieModel("Happy", 60, 0xFF4CAF50));
            binding.chartEmotion.addPieSlice(new PieModel("Neutral", 25, 0xFFFFC107));
            binding.chartEmotion.addPieSlice(new PieModel("Sad", 15, 0xFFD81B60));
        }
        binding.chartEmotion.startAnimation();
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
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update student: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to update student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupMenuBar() {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(v -> {
            navController.navigate(R.id.action_studentProfileFragment_to_homeFragment);
        });

        binding.menuReports.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Reports clicked", Toast.LENGTH_SHORT).show();
        });

        binding.menuStudent.setOnClickListener(v -> {
            navController.navigate(R.id.action_studentProfileFragment_to_studentFragment);
        });

        binding.menuSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Settings clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}