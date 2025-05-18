package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentClassDetailBinding;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.utils.OnDatesLoadedCallback;
import com.example.smartclassemotion.utils.OnEmotionStatsLoadedCallback;
import com.example.smartclassemotion.utils.OnStudentEmotionStatsLoadedCallback;
import com.example.smartclassemotion.viewmodel.StudentEmotionAdapter;
import com.example.smartclassemotion.viewmodel.StudentSelectAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.eazegraph.lib.models.PieModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassDetailFragment extends Fragment {
    private static final String TAG = "ClassDetailFragment";
    private FragmentClassDetailBinding binding;
    private FirebaseHelper firebaseHelper;
    private String classId;
    private String className;
    private String userId;
    private List<Student> studentList = new ArrayList<>();
    private List<Map<String, Float>> emotionStatsList = new ArrayList<>();
    private StudentEmotionAdapter studentEmotionAdapter;
    private List<String> availableDates = new ArrayList<>();
    private ArrayAdapter<String> dateAdapter;

    public ClassDetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClassDetailBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        firebaseHelper = new FirebaseHelper();

        initializeArgument();
        setupRecyclerView();
        setupDateSpinner();
        setupBackButton();
        setupMenuBar();
        setupAddStudentButton();

        if (classId != null) {
            binding.className.setText(className);
            loadAvailableDates();
        } else {
            Log.w(TAG, "Class ID is null, cannot load class data");
            showToast("No class data available");
        }

        return root;
    }

    private void initializeArgument() {
        Bundle args = getArguments();
        if (args != null) {
            classId = args.getString("class_id");
            className = args.getString("class_name");
            userId = args.getString("user_id");
            Log.d(TAG, "Received args - classId: " + classId + ", className: " + className + ", userId: " + userId);
        } else {
            Log.w(TAG, "No arguments received");
            Toast.makeText(getContext(), "No class data received", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        binding.studentRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentEmotionAdapter = new StudentEmotionAdapter(studentList, emotionStatsList);
        binding.studentRecycleView.setAdapter(studentEmotionAdapter);
    }

    private void setupDateSpinner() {
        dateAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, availableDates);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.dateSpinner.setAdapter(dateAdapter);
        binding.dateSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedDate = availableDates.get(position);
                loadClassData(classId, selectedDate);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadAvailableDates() {
        firebaseHelper.getAvailableDates(classId, new OnDatesLoadedCallback() {
            @Override
            public void onDatesLoaded(List<String> dates) {
                if (getActivity() == null) return;
                availableDates.clear();
                availableDates.addAll(dates);
                Collections.sort(availableDates, Collections.reverseOrder()); // Sắp xếp từ mới nhất
                dateAdapter.notifyDataSetChanged();
                if (!availableDates.isEmpty()) {
                    binding.dateSpinner.setSelection(0); // Chọn ngày gần nhất
                    loadClassData(classId, availableDates.get(0));
                } else {
                    showToast("No emotion reports available");
                }
            }
        });
    }

    private void setupBackButton() {
        binding.backBtn.setOnClickListener(v -> navigateToHomeFragment());
    }

    private void setupAddStudentButton() {
        binding.addStudentBtn.setOnClickListener(v -> showAddStudentDialog());
    }

    private void showAddStudentDialog() {
        if (userId == null || classId == null) {
            showToast("User ID or Class ID not found");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_student, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.student_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<Student> availableStudents = new ArrayList<>();
        StudentSelectAdapter studentAdapter = new StudentSelectAdapter(availableStudents, null);
        recyclerView.setAdapter(studentAdapter);

        studentAdapter.setOnStudentClickListener(student -> {
            addStudentToClass(student, availableStudents, studentAdapter);
        });

        loadAvailableStudents(students -> {
            if (students.isEmpty()) {
                showToast("No available students to add");
                builder.setView(dialogView);
                builder.setMessage("No students available to add to this class.");
                AlertDialog dialog = builder.create();
                dialog.show();
                dialogView.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
                return;
            }

            availableStudents.clear();
            availableStudents.addAll(students);
            studentAdapter.notifyDataSetChanged();

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            dialog.show();

            dialogView.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.add_button).setOnClickListener(v -> {
                showToast("Please select a student to add");
            });
        });
    }

    private void loadAvailableStudents(OnStudentsLoadedCallback callback) {
        List<Student> availableStudents = new ArrayList<>();

        firebaseHelper.getDb().collection("Students")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(studentSnapshot -> {
                    List<String> studentIdsInClass = new ArrayList<>();

                    firebaseHelper.getDb().collection("StudentClasses")
                            .whereEqualTo("classId", classId)
                            .get()
                            .addOnSuccessListener(classSnapshot -> {
                                for (QueryDocumentSnapshot doc : classSnapshot) {
                                    String studentId = doc.getString("studentId");
                                    if (studentId != null) {
                                        studentIdsInClass.add(studentId);
                                    }
                                }

                                for (QueryDocumentSnapshot doc : studentSnapshot) {
                                    Student student = doc.toObject(Student.class);
                                    if (!studentIdsInClass.contains(student.getStudentId())) {
                                        availableStudents.add(student);
                                    }
                                }

                                callback.onStudentsLoaded(availableStudents);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to load StudentClasses: " + e.getMessage());
                                showToast("Failed to load student list");
                                callback.onStudentsLoaded(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load Students: " + e.getMessage());
                    showToast("Failed to load student list");
                    callback.onStudentsLoaded(new ArrayList<>());
                });
    }

    private void addStudentToClass(Student student, List<Student> availableStudents, StudentSelectAdapter studentAdapter) {
        if (classId == null || student.getStudentId() == null) {
            showToast("Invalid class or student ID");
            return;
        }

        Map<String, Object> studentClassData = new HashMap<>();
        studentClassData.put("studentId", student.getStudentId());
        studentClassData.put("classId", classId);
        studentClassData.put("joinedAt", Timestamp.now());

        firebaseHelper.getDb().collection("StudentClasses")
                .add(studentClassData)
                .addOnSuccessListener(documentReference -> {
                    int position = studentList.size();
                    studentList.add(student);
                    emotionStatsList.add(new HashMap<>()); // Thêm HashMap rỗng cho học sinh mới
                    Log.d(TAG, "Added student: " + student.getStudentName() +
                            ", studentList size: " + studentList.size() +
                            ", emotionStatsList size: " + emotionStatsList.size());
                    binding.studentCount.setText(studentList.size() + " ");
                    studentEmotionAdapter.notifyItemInserted(position);

                    int adapterPosition = availableStudents.indexOf(student);
                    if (adapterPosition != -1) {
                        availableStudents.remove(adapterPosition);
                        studentAdapter.notifyItemRemoved(adapterPosition);
                        Log.d(TAG, "Removed student " + student.getStudentName() + " from dialog, remaining: " + availableStudents.size());
                    }

                    showToast("Added " + student.getStudentName() + " to class");
                    Log.d(TAG, "Added student " + student.getStudentId() + " to class " + classId);
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to add student: " + e.getMessage());
                    Log.e(TAG, "Failed to add student: " + e.getMessage());
                });
    }

    interface OnStudentsLoadedCallback {
        void onStudentsLoaded(List<Student> students);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadClassData(String classId, String selectedDate) {
        firebaseHelper.getEmotionStatsByDate(classId, selectedDate, new OnEmotionStatsLoadedCallback() {
            @Override
            public void onEmotionStatsLoaded(Map<String, Float> emotionStats) {
                if (getActivity() == null) {
                    Log.w(TAG, "Activity is null, cannot update pie chart");
                    return;
                }
                Log.d(TAG, "Emotion stats loaded for date " + selectedDate + ": " + emotionStats);
                setupPieChart(emotionStats);
            }
        });

        firebaseHelper.getStudentEmotionStatsByDate(classId, selectedDate, new OnStudentEmotionStatsLoadedCallback() {
            @Override
            public void onStudentEmotionStatsLoaded(List<Student> students, List<Map<String, Float>> emotionStats) {
                if (getActivity() == null) {
                    Log.w(TAG, "Activity is null, cannot update student list");
                    return;
                }
                Log.d(TAG, "Students loaded: " + students.size() + ", Emotion stats: " + emotionStats.size());
                studentList.clear();
                studentList.addAll(students);
                emotionStatsList.clear();
                emotionStatsList.addAll(emotionStats);
                // Đảm bảo emotionStatsList khớp với studentList
                while (emotionStatsList.size() < studentList.size()) {
                    Map<String, Float> emptyStats = new HashMap<>();
                    emptyStats.put("happy", 0f);
                    emptyStats.put("sad", 0f);
                    emptyStats.put("angry", 0f);
                    emptyStats.put("neutral", 0f);
                    emptyStats.put("fear", 0f);
                    emptyStats.put("surprise", 0f);
                    emotionStatsList.add(emptyStats);
                    Log.w(TAG, "Added empty emotion stats for student at index: " + emotionStatsList.size());
                }
                binding.studentCount.setText(studentList.size() + " ");
                studentEmotionAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setupPieChart(Map<String, Float> emotionStats) {
        binding.emotionPieChart.clearChart();
        if (!hasValidEmotionData(emotionStats)) {
            Log.d(TAG, "No valid emotion data to display");
            binding.emotionPieChart.addPieSlice(new PieModel(
                    "No Data",
                    100,
                    0xFF9E9E9E // Gray
            ));
            binding.emotionPieChart.startAnimation();
            showToast("No data available");
            return;
        }
        int[] colors = {
                0xFF4CAF50, // Happy - Xanh lá
                0xFFF44336, // Sad - Đỏ
                0xFFFF9800, // Angry - Cam
                0xFF9E9E9E, // Neutral - Xám
                0xFF06B3C9, // Fear - Cyan
                0xFFC910C6  // Surprise - Tím
        };
        int colorIndex = 0;
        for (Map.Entry<String, Float> entry : emotionStats.entrySet()) {
            float value = entry.getValue();
            if (value > 0) {
                binding.emotionPieChart.addPieSlice(new PieModel(
                        entry.getKey(),
                        entry.getValue(),
                        colors[colorIndex % colors.length]
                ));
                colorIndex++;
            }
        }
        binding.emotionPieChart.setInnerValueUnit("%");
        binding.emotionPieChart.setDrawValueInPie(true);
        binding.emotionPieChart.startAnimation();
    }

    private boolean hasValidEmotionData(Map<String, Float> emotionStats) {
        if (emotionStats == null || emotionStats.isEmpty()) {
            Log.w(TAG, "Emotion stats is null or empty");
            return false;
        }
        for (Float value : emotionStats.values()) {
            if (value != null && value > 0) {
                return true;
            }
        }
        return false;
    }

    private void setupMenuBar() {
        binding.menuClasses.setOnClickListener(v -> navigateToHomeFragment());
        binding.menuReports.setOnClickListener(v -> navigateToReportFragment());
        binding.menuStudent.setOnClickListener(v -> navigateToStudentFragment());
        binding.menuSettings.setOnClickListener(v -> navigateToSettingFragment());
    }

    private void navigateToHomeFragment() {
        navigateTo(R.id.action_classDetailFragment_to_homeFragment);
    }

    private void navigateToStudentFragment() {
        navigateTo(R.id.action_classDetailFragment_to_studentFragment);
    }

    private void navigateToSettingFragment() {
        navigateTo(R.id.action_classDetailFragment_to_settingFragment);
    }

    private void navigateToReportFragment() {
        navigateTo(R.id.action_classDetailFragment_to_alertFragment);
    }

    private void navigateTo(int actionId) {
        if (userId == null) {
            showToast("User ID not found");
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(actionId, bundle);
            Log.d(TAG, "Navigating to actionId: " + actionId + " with userId: " + userId);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}