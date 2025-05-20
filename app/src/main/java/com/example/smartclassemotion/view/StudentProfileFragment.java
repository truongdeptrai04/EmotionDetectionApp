package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.smartclassemotion.utils.OnStudentSessionEmotionStatsCallback;
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
    private ArrayAdapter<String> classSpinnerAdapter;
    private boolean isInitialLoad = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper(getContext());
        className = new ArrayList<>();
        classIds = new ArrayList<>();

        initializeArgument();
        if (studentId == null || userId == null) {
            Log.e(TAG, "Student ID or User ID is null");
            showToast("Student ID or User ID is null");
            return root;
        }

        setupStatusSpinner();
        setupClassSpinner();
        setupNavigation();
        setupEditMode();
        loadStudentData();
        return root;
    }

    private void initializeArgument() {
        if (getArguments() != null) {
            studentId = getArguments().getString("student_id");
            userId = getArguments().getString("user_id");
            Log.d(TAG, "Received student_id: " + studentId + ", user_id: " + userId);
        }
    }

    private void setupStatusSpinner() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.status_list)
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(position == 0 ? 0xFF000000 : 0xFFFF0000);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(position == 0 ? 0xFF000000 : 0xFFFF0000);
                return view;
            }
        };
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.statusSpinner.setAdapter(statusAdapter);
        binding.statusSpinner.setEnabled(false);
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
                            updateStudentUI();
                        } else {
                            Log.e(TAG, "Failed to parse student object from Firestore");
                            showToast("Failed to load student data");
                            resetFields();
                        }
                    } else {
                        Log.e(TAG, "Student document does not exist for student_id: " + studentId);
                        showToast("Student not found");
                        resetFields();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load student data: " + e.getMessage(), e);
                    showToast("Failed to load student data: " + e.getMessage());
                    resetFields();
                });
    }

    private void updateStudentUI() {
        binding.tvNamestudent.setText(getStringOrDefault(student.getStudentName(), "N/A"));
        binding.edtNamestudent.setText(getStringOrDefault(student.getStudentName(), ""));
        binding.tvGender.setText(getStringOrDefault(student.getGender(), "N/A"));
        setSpinnerSelection(binding.spinnerGender, getResources().getStringArray(R.array.gender_options), student.getGender());
        binding.tvPhone.setText(getStringOrDefault(student.getPhone(), "N/A"));
        binding.edtPhone.setText(getStringOrDefault(student.getPhone(), ""));
        binding.tvEmail.setText(getStringOrDefault(student.getEmail(), "N/A"));
        binding.edtEmail.setText(getStringOrDefault(student.getEmail(), ""));
        binding.edtNote.setText(getStringOrDefault(student.getNotes(), ""));
        setSpinnerSelection(binding.statusSpinner, getResources().getStringArray(R.array.status_list), student.getStatus());
        binding.statusSpinner.setEnabled(false);
    }

    private void resetFields() {
        binding.tvNamestudent.setText("N/A");
        binding.edtNamestudent.setText("");
        binding.tvGender.setText("N/A");
        binding.spinnerGender.setSelection(0);
        binding.tvPhone.setText("N/A");
        binding.edtPhone.setText("");
        binding.tvEmail.setText("N/A");
        binding.edtEmail.setText("");
        binding.edtNote.setText("");
        binding.statusSpinner.setSelection(0);
    }

    private void setupClassSpinner() {
        classSpinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                className
        );
        classSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerClass.setAdapter(classSpinnerAdapter);
        binding.spinnerClass.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isInitialLoad) {
                    isInitialLoad = false;
                    Log.d(TAG, "Initial spinner selection ignored");
                    return;
                }
                if (className.isEmpty()) {
                    Log.e(TAG, "Class list is empty");
                    return;
                }
                String selectedClassId = classIds.get(position);
                Log.d(TAG, "Selected class: " + className.get(position) + ", classId: " + selectedClassId);
                updateEmotionChart(selectedClassId);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                Log.d(TAG, "No class selected");
            }
        });
        loadClasses();
    }

    private void loadClasses() {
        if (userId == null) {
            Log.e(TAG, "User ID is null, cannot load classes");
            showToast("User ID not found");
            return;
        }
        firebaseHelper.getDb().collection("StudentClasses")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(studentClassesSnapshot -> {
                    className.clear();
                    classIds.clear();

                    List<String> classIdList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : studentClassesSnapshot) {
                        String classId = doc.getString("classId");
                        if (classId != null) {
                            classIdList.add(classId);
                        }
                    }

                    if (classIdList.isEmpty()) {
                        Log.w(TAG, "No classes found for student_id: " + studentId);
                        showToast("No classes found for this student");
                        classSpinnerAdapter.notifyDataSetChanged();
                        binding.chartEmotion.clearChart();
                        binding.chartEmotion.addPieSlice(new PieModel("No Classes", 100, 0xFF9E9E9E));
                        binding.chartEmotion.startAnimation();
                        return;
                    }

                    firebaseHelper.getDb().collection("Classes")
                            .whereIn("classId", classIdList)
                            .orderBy("className")
                            .get()
                            .addOnSuccessListener(classesSnapshot -> {
                                for (QueryDocumentSnapshot doc : classesSnapshot) {
                                    String classNameItem = doc.getString("className");
                                    String classId = doc.getString("classId");
                                    if (classNameItem != null && classId != null) {
                                        className.add(classNameItem);
                                        classIds.add(classId);
                                    }
                                }
                                classSpinnerAdapter.notifyDataSetChanged();
                                Log.d(TAG, "Class list loaded: " + className.size() + " classes for student_id: " + studentId);
                                if (!className.isEmpty()) {
                                    binding.spinnerClass.setSelection(0);
                                    updateEmotionChart(classIds.get(0));
                                } else {
                                    Log.w(TAG, "No matching classes found in Classes collection");
                                    showToast("No matching classes found");
                                    binding.chartEmotion.clearChart();
                                    binding.chartEmotion.addPieSlice(new PieModel("No Classes", 100, 0xFF9E9E9E));
                                    binding.chartEmotion.startAnimation();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to load classes: " + e.getMessage(), e);
                                showToast("Failed to load classes: " + e.getMessage());
                                binding.chartEmotion.clearChart();
                                binding.chartEmotion.addPieSlice(new PieModel("Error", 100, 0xFF9E9E9E));
                                binding.chartEmotion.startAnimation();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load student classes: " + e.getMessage(), e);
                    showToast("Failed to load student classes: " + e.getMessage());
                    binding.chartEmotion.clearChart();
                    binding.chartEmotion.addPieSlice(new PieModel("Error", 100, 0xFF9E9E9E));
                    binding.chartEmotion.startAnimation();
                });
    }

    private void updateEmotionChart(String classId) {
        binding.chartEmotion.clearChart();
        if (studentId == null || classId == null || classId.isEmpty()) {
            Log.e(TAG, "Student ID or Class ID is null/empty, cannot update emotion chart");
            binding.chartEmotion.addPieSlice(new PieModel("No Data", 100, 0xFF9E9E9E));
            binding.chartEmotion.startAnimation();
            return;
        }

        firebaseHelper.getStudentSessionEmotionStats(studentId, classId, new OnStudentSessionEmotionStatsCallback() {
            @Override
            public void onStudentSessionEmotionStatsLoaded(Map<String, Float> emotionStats) {
                binding.chartEmotion.clearChart();
                float happy = emotionStats.getOrDefault("happy", 0f);
                float sad = emotionStats.getOrDefault("sad", 0f);
                float angry = emotionStats.getOrDefault("angry", 0f);
                float neutral = emotionStats.getOrDefault("neutral", 0f);
                float fear = emotionStats.getOrDefault("fear", 0f);
                float surprise = emotionStats.getOrDefault("surprise", 0f);

                if (happy > 0) binding.chartEmotion.addPieSlice(new PieModel("Happy", happy, 0xFF4CAF50));
                if (neutral > 0) binding.chartEmotion.addPieSlice(new PieModel("Neutral", neutral, 0xFFFFC107));
                if (sad > 0) binding.chartEmotion.addPieSlice(new PieModel("Sad", sad, 0xFFD81B60));
                if (angry > 0) binding.chartEmotion.addPieSlice(new PieModel("Angry", angry, 0xFFE91E63));
                if (fear > 0) binding.chartEmotion.addPieSlice(new PieModel("Fear", fear, 0xFF9C27B0));
                if (surprise > 0) binding.chartEmotion.addPieSlice(new PieModel("Surprise", surprise, 0xFF2196F3));

                if (happy == 0 && sad == 0 && angry == 0 && neutral == 0 && fear == 0 && surprise == 0) {
                    Log.d(TAG, "No emotion stats found for student_id: " + studentId + ", class_id: " + classId);
                    binding.chartEmotion.addPieSlice(new PieModel("No Data", 100, 0xFF9E9E9E));
                }

                Log.d(TAG, "Emotion stats loaded: happy=" + happy + ", sad=" + sad + ", neutral=" + neutral + ", angry=" + angry + ", fear=" + fear + ", surprise=" + surprise);
                binding.chartEmotion.startAnimation();
            }
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
        binding.spinnerGender.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvGender.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.edtPhone.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvPhone.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.edtEmail.setVisibility(enable ? View.VISIBLE : View.GONE);
        binding.tvEmail.setVisibility(enable ? View.GONE : View.VISIBLE);
        binding.statusSpinner.setEnabled(enable);
        binding.edtNote.setEnabled(enable);

        if (enable) {
            binding.edtNamestudent.setText(binding.tvNamestudent.getText());
            setSpinnerSelection(binding.spinnerGender, getResources().getStringArray(R.array.gender_options), binding.tvGender.getText().toString());
            binding.edtPhone.setText(binding.tvPhone.getText());
            binding.edtEmail.setText(binding.tvEmail.getText());
            binding.btnSave.setImageResource(R.drawable.baseline_check_24_white);
        } else {
            binding.btnSave.setImageResource(R.drawable.ic_edit_white);
        }
    }

    private void saveStudentData() {
        if (student == null) {
            showToast("Student data is null");
            return;
        }

        String name = binding.edtNamestudent.getText().toString().trim();
        String gender = binding.spinnerGender.getSelectedItem() != null ? binding.spinnerGender.getSelectedItem().toString() : "";
        String phone = binding.edtPhone.getText().toString().trim();
        String email = binding.edtEmail.getText().toString().trim();
        String notes = binding.edtNote.getText().toString().trim();
        String status = binding.statusSpinner.getSelectedItem() != null ? binding.statusSpinner.getSelectedItem().toString() : "Active";

        if (!validateInput(name, gender, phone, email)) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("studentName", name);
        updates.put("gender", gender);
        updates.put("phone", phone);
        updates.put("email", email);
        updates.put("notes", notes);
        updates.put("status", status);

        firebaseHelper.getDb().collection("Students").document(studentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student updated: " + name);
                    showToast("Student updated successfully");
                    isEditMode = false;
                    toggleEditMode(false);
                    binding.tvNamestudent.setText(name);
                    binding.tvGender.setText(gender);
                    binding.tvPhone.setText(phone);
                    binding.tvEmail.setText(email);
                    setSpinnerSelection(binding.statusSpinner, getResources().getStringArray(R.array.status_list), status);
                    binding.statusSpinner.setEnabled(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update student: " + e.getMessage(), e);
                    showToast("Failed to update student: " + e.getMessage());
                });
    }

    private void setupNavigation() {
        binding.menuClasses.setOnClickListener(v -> navigateTo(R.id.action_studentProfileFragment_to_homeFragment));
        binding.menuReports.setOnClickListener(v -> navigateTo(R.id.action_studentProfileFragment_to_alertFragment));
        binding.menuStudents.setOnClickListener(v -> navigateTo(R.id.action_studentProfileFragment_to_studentFragment));
        binding.menuSettings.setOnClickListener(v -> navigateTo(R.id.action_studentProfileFragment_to_settingFragment));
    }

    private void navigateTo(int actionId) {
        if (userId == null) {
            showToast("User ID not found");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(actionId, bundle);
        Log.d(TAG, "Navigating to Action " + actionId + " with userId: " + userId);
    }

    private boolean validateInput(String name, String gender, String phone, String email) {
        if (name.isEmpty()) {
            showToast("Name cannot be empty");
            return false;
        }
        if (gender.isEmpty()) {
            showToast("Gender cannot be empty");
            return false;
        }
        if (!phone.isEmpty() && !phone.matches("^[0-9+]{9,15}$")) {
            showToast("Phone number is invalid (10 - 15 digits)");
            return false;
        }
        if (!email.isEmpty() && !email.matches("[A-ZA-z0-9+_.-]+@(.+)$")) {
            showToast("Invalid email format");
            return false;
        }
        return true;
    }

    private String getStringOrDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private void setSpinnerSelection(android.widget.Spinner spinner, String[] items, String value) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}