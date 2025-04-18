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
    private String userId;
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
            userId = args.getString("user_id");
            Log.d(TAG, "Received args - classId: " + classId + ", className: " + className + ", userId: " + userId);
            binding.classNameTv.setText(className != null ? className : "Unknown Class");
        } else {
            Log.w(TAG, "No arguments received");
            Toast.makeText(getContext(), "No class data received", Toast.LENGTH_SHORT).show();
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
            navigateToHomeFragment(navController);
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
                android.graphics.Color.parseColor("#9E9E9E"), // Neutral - Xám
                android.graphics.Color.parseColor("#06b3c9"),
                android.graphics.Color.parseColor("#c910c6"),
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
            navigateToHomeFragment(navController);
        });

        binding.menuReports.setOnClickListener(v -> {
            navigateToReportFragment(navController);
        });

        binding.menuStudent.setOnClickListener(v -> {
            navigateToStudentFragment(navController);
        });

        binding.menuSettings.setOnClickListener(v -> {
            navigateToSettingFragment(navController);
        });
    }
    private void navigateToHomeFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_classDetailFragment_to_homeFragment, bundle);
            Log.d(TAG, "Navigating to HomeFragment with userId: " + userId);
        }
    }
    private void navigateToStudentFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_classDetailFragment_to_studentFragment, bundle);
            Log.d(TAG, "Navigating to StudentFragment with userId: " + userId);
        }
    }
    private void navigateToSettingFragment(NavController navController){
        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_classDetailFragment_to_settingFragment, bundle);
            Log.d(TAG, "Navigating to SettingFragment with userId: " + userId);
        }
    }
    private void navigateToReportFragment(NavController navController){
        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_classDetailFragment_to_reportFragment, bundle);
            Log.d(TAG, "Navigating to ReportFragment with userId: " + userId);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}