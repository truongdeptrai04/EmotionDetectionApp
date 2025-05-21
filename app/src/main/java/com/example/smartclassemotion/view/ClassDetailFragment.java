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
import com.example.smartclassemotion.utils.StudentListCallback;
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
        firebaseHelper = new FirebaseHelper(getContext());

        initializeArgument();
        setupRecyclerView();
        setupDateSpinner();
        setupBackButton();
        setupMenuBar();
        setupAddStudentButton();

        if (classId != null) {
            binding.className.setText(className);
            loadStudentsAndDates();
        } else {
            Log.w(TAG, "Class ID is null, cannot load class data");
        }

        return root;
    }

    private void initializeArgument() {
        Bundle args = getArguments();
        if (args != null) {
            classId = args.getString("class_id");
            className = args.getString("class_name");
            userId = args.getString("user_id");
            Log.d(TAG, "Nhận tham số - classId: " + classId + ", className: " + className + ", userId: " + userId);
        } else {
            Log.w(TAG, "Không nhận được tham số");
            Toast.makeText(getContext(), "Không nhận được dữ liệu lớp học", Toast.LENGTH_SHORT).show();
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
                loadEmotionData(classId, selectedDate);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Không làm gì
            }
        });
    }

    private void loadStudentsAndDates() {
        // Tải danh sách học sinh trước
        firebaseHelper.getStudentsByClassId(classId, new StudentListCallback() {
            @Override
            public void onStudentsLoaded(List<Student> students) {
                if (getActivity() == null) return;
                studentList.clear();
                studentList.addAll(students);
                // Khởi tạo emotionStatsList với giá trị mặc định
                emotionStatsList.clear();
                for (int i = 0; i < studentList.size(); i++) {
                    Map<String, Float> defaultStats = new HashMap<>();
                    defaultStats.put("happy", 0f);
                    defaultStats.put("sad", 0f);
                    defaultStats.put("angry", 0f);
                    defaultStats.put("neutral", 0f);
                    defaultStats.put("fear", 0f);
                    defaultStats.put("surprise", 0f);
                    emotionStatsList.add(defaultStats);
                }
                binding.studentCount.setText(studentList.size() + " ");
                studentEmotionAdapter.notifyDataSetChanged();
                Log.d(TAG, "Đã tải " + studentList.size() + " học sinh cho lớp " + classId);

                // Sau khi tải học sinh, tải danh sách ngày
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
                            loadEmotionData(classId, availableDates.get(0));
                        } else {
                            // Hiển thị biểu đồ trống
                            setupPieChart(new HashMap<>());
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {

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
                builder.setView(dialogView);
                builder.setMessage("Không có học sinh nào để thêm vào lớp này.");
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
                                Log.e(TAG, "Lỗi khi tải StudentClasses: " + e.getMessage());
                                callback.onStudentsLoaded(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải Students: " + e.getMessage());
                    callback.onStudentsLoaded(new ArrayList<>());
                });
    }

    private void addStudentToClass(Student student, List<Student> availableStudents, StudentSelectAdapter studentAdapter) {
        if (classId == null || student.getStudentId() == null) {
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
                    Map<String, Float> defaultStats = new HashMap<>();
                    defaultStats.put("happy", 0f);
                    defaultStats.put("sad", 0f);
                    defaultStats.put("angry", 0f);
                    defaultStats.put("neutral", 0f);
                    defaultStats.put("fear", 0f);
                    defaultStats.put("surprise", 0f);
                    emotionStatsList.add(defaultStats);
                    Log.d(TAG, "Đã thêm học sinh: " + student.getStudentName() +
                            ", studentList size: " + studentList.size() +
                            ", emotionStatsList size: " + emotionStatsList.size());
                    binding.studentCount.setText(studentList.size() + " ");
                    studentEmotionAdapter.notifyItemInserted(position);

                    int adapterPosition = availableStudents.indexOf(student);
                    if (adapterPosition != -1) {
                        availableStudents.remove(adapterPosition);
                        studentAdapter.notifyItemRemoved(adapterPosition);
                        Log.d(TAG, "Đã xóa học sinh " + student.getStudentName() + " khỏi dialog, còn lại: " + availableStudents.size());
                    }

                    Log.d(TAG, "Đã thêm học sinh " + student.getStudentId() + " vào lớp " + classId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thêm học sinh: " + e.getMessage());
                });
    }

    interface OnStudentsLoadedCallback {
        void onStudentsLoaded(List<Student> students);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadEmotionData(String classId, String selectedDate) {
        firebaseHelper.getEmotionStatsByDate(classId, selectedDate, new OnEmotionStatsLoadedCallback() {
            @Override
            public void onEmotionStatsLoaded(Map<String, Float> emotionStats) {
                if (getActivity() == null) {
                    Log.w(TAG, "Activity là null, không thể cập nhật biểu đồ");
                    return;
                }
                Log.d(TAG, "Thống kê cảm xúc đã tải cho ngày " + selectedDate + ": " + emotionStats);
                setupPieChart(emotionStats);
            }
        });

        firebaseHelper.getStudentEmotionStatsByDate(classId, selectedDate, new OnStudentEmotionStatsLoadedCallback() {
            @Override
            public void onStudentEmotionStatsLoaded(List<Student> students, List<Map<String, Float>> emotionStats) {
                if (getActivity() == null) {
                    Log.w(TAG, "Activity là null, không thể cập nhật danh sách học sinh");
                    return;
                }
                Log.d(TAG, "Học sinh đã tải: " + students.size() + ", Thống kê cảm xúc: " + emotionStats.size());
                // Cập nhật emotionStatsList dựa trên danh sách học sinh hiện tại
                emotionStatsList.clear();
                for (Student student : studentList) {
                    Map<String, Float> stats = null;
                    for (int i = 0; i < students.size(); i++) {
                        if (students.get(i).getStudentId().equals(student.getStudentId())) {
                            stats = emotionStats.get(i);
                            break;
                        }
                    }
                    if (stats == null) {
                        stats = new HashMap<>();
                        stats.put("happy", 0f);
                        stats.put("sad", 0f);
                        stats.put("angry", 0f);
                        stats.put("neutral", 0f);
                        stats.put("fear", 0f);
                        stats.put("surprise", 0f);
                    }
                    emotionStatsList.add(stats);
                }
                studentEmotionAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setupPieChart(Map<String, Float> emotionStats) {
        binding.emotionPieChart.clearChart();
        if (!hasValidEmotionData(emotionStats)) {
            Log.d(TAG, "Không có dữ liệu cảm xúc hợp lệ để hiển thị");
            binding.emotionPieChart.addPieSlice(new PieModel(
                    "Không có dữ liệu",
                    100,
                    0xFF9E9E9E // Xám
            ));
            binding.emotionPieChart.startAnimation();
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
            Log.w(TAG, "Thống kê cảm xúc là null hoặc rỗng");
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
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(actionId, bundle);
            Log.d(TAG, "Điều hướng đến actionId: " + actionId + " với userId: " + userId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
