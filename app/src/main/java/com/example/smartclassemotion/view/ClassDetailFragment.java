package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentClassDetailBinding;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.viewmodel.StudentEmotionAdapter;
import org.eazegraph.lib.models.PieModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassDetailFragment extends Fragment {
    private static final String TAG = "ClassDetailFragment";
    private FragmentClassDetailBinding binding;
    private FirebaseHelper firebaseHelper;
    private String classId;
    private String className;
    private int studentCount;
    private List<Student> studentList = new ArrayList<>();
    private List<Map<String, Float>> emotionStatsList = new ArrayList<>();
    private StudentEmotionAdapter studentEmotionAdapter;

    public ClassDetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClassDetailBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        firebaseHelper = new FirebaseHelper();

        Bundle args = getArguments();
        if (args != null) {
            classId = args.getString("class_id");
            className = args.getString("class_name");
            studentCount = args.getInt("student_count", 0); // Sửa lại để lấy int thay vì parse String
            Log.d(TAG, "Received args - classId: " + classId + ", className: " + className + ", studentCount: " + studentCount);
            binding.classNameTv.setText(className != null ? className : "Unknown Class");
            binding.studentCount.setText(String.valueOf(studentCount));
        } else {
            Log.w(TAG, "No arguments received");
            Toast.makeText(getContext(), "No class data received", Toast.LENGTH_SHORT).show();
            binding.classNameTv.setText("Unknown Class");
            binding.studentCount.setText("0");
        }

        binding.studentRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentEmotionAdapter = new StudentEmotionAdapter(studentList, emotionStatsList);
        binding.studentRecycleView.setAdapter(studentEmotionAdapter);

        if (classId != null) {
            loadClassData(classId);
        } else {
            Log.w(TAG, "classId is null, cannot load data");
        }

        binding.backBtn.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_classDetailFragment_to_homeFragment);
        });

        setupMenuBar();

        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadClassData(String classId) {
        firebaseHelper.getEmotionStats(classId, emotionStats -> {
            if (getActivity() != null) {
                Log.d(TAG, "Emotion stats loaded: " + emotionStats.toString());
                setupPieChart(emotionStats);
            } else {
                Log.w(TAG, "Activity is null, cannot update pie chart");
            }
        });

        firebaseHelper.getStudentEmotionStats(classId, (students, emotionStats) -> {
            if (getActivity() != null) {
                Log.d(TAG, "Students loaded: " + students.size() + ", Emotion stats: " + emotionStats.size());
                studentList.clear();
                studentList.addAll(students);
                emotionStatsList.clear();
                emotionStatsList.addAll(emotionStats);
                binding.studentCount.setText(students.size() + " ");
                studentEmotionAdapter.notifyDataSetChanged();
            } else {
                Log.w(TAG, "Activity is null, cannot update student list");
            }
        });
    }

    private void setupPieChart(Map<String, Float> emotionStats) {
        binding.emotionPieChart.clearChart();
        if (emotionStats.isEmpty()) {
            Log.w(TAG, "Emotion stats is empty, no data to display in pie chart");
            return;
        }

        int[] colors = {
                android.graphics.Color.parseColor("#4CAF50"), // Happy - Xanh lá
                android.graphics.Color.parseColor("#F44336"), // Sad - Đỏ
                android.graphics.Color.parseColor("#FF9800"), // Angry - Cam
                android.graphics.Color.parseColor("#9E9E9E")  // Neutral - Xám
        };
        int colorIndex = 0;

        for (Map.Entry<String, Float> entry : emotionStats.entrySet()) {
            binding.emotionPieChart.addPieSlice(new PieModel(
                    entry.getKey(),
                    entry.getValue(),
                    colors[colorIndex % colors.length]
            ));
            colorIndex++;
        }
        binding.emotionPieChart.setInnerValueUnit("%");
        binding.emotionPieChart.setDrawValueInPie(true);
        binding.emotionPieChart.startAnimation();
    }

    private void setupMenuBar() {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(v -> {
            navController.navigate(R.id.action_classDetailFragment_to_homeFragment);
        });

        binding.menuReports.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Reports clicked", Toast.LENGTH_SHORT).show();
        });

        binding.menuStudent.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Student clicked", Toast.LENGTH_SHORT).show();
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