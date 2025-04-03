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

import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentHomeBinding;
import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.viewmodel.ClassAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private FirebaseHelper firebaseHelper;
    private List<ClassItem> classList;
    private ClassAdapter classAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        classList = new ArrayList<>();

        String userId = getArguments() != null ? getArguments().getString("user_id") : null;
        if (userId == null) {
            userId = firebaseHelper.getCurrentUserId();
            if (userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Welcome back, UserID: " + userId, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Welcome, UserID: " + userId, Toast.LENGTH_SHORT).show();
        }

        binding.classRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        classAdapter = new ClassAdapter(classList);
        binding.classRecyclerView.setAdapter(classAdapter);

        loadClassesFromFireStore(userId);
        setupMenuBar();
        setupAddClassButton(userId);

        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadClassesFromFireStore(String userId) {
        if (userId == null) return;

        firebaseHelper.getClasses(userId, classes -> {
            if (getActivity() != null) {
                classList.clear();
                classList.addAll(classes);
                classAdapter.notifyDataSetChanged();
                binding.activeClassCount.setText(classList.size() + " ");
                Log.d(TAG, "Classes loaded: " + classList.size());
            }
        });
    }

    private void setupAddClassButton(String userId) {
        binding.addBtn.setOnClickListener(view -> {
            Log.d(TAG, "addBtn clicked");
            showAddClassFrame();
        });

        binding.submitBtn.setOnClickListener(view -> {
            String className = binding.classNameInput.getText().toString().trim();
            String classTime = binding.classTimeInput.getText().toString().trim();
            Log.d(TAG, "Submit clicked - ClassName: " + className + ", Time: " + classTime);

            if (className.isEmpty() || classTime.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> classData = new HashMap<>();
            classData.put("userId", userId);
            classData.put("className", className);
            classData.put("time", classTime);
            classData.put("studentCount", 0);
            classData.put("emotion", "neutral");

            firebaseHelper.getDb().collection("Classes")
                    .add(classData)
                    .addOnSuccessListener(documentReference -> {
                        String newClassId = documentReference.getId();
                        ClassItem newClass = new ClassItem(newClassId, className, 0, classTime, "neutral");
                        classList.add(newClass);
                        classAdapter.notifyDataSetChanged();
                        binding.activeClassCount.setText(classList.size() + " ");
                        hideAddClassFrame();
                        Toast.makeText(getContext(), "Class added successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Class added: " + newClassId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to add class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to add class: " + e.getMessage());
                    });
        });

        binding.cancelBtn.setOnClickListener(view -> {
            Log.d(TAG, "Cancel clicked");
            hideAddClassFrame();
        });
    }

    private void showAddClassFrame() {
        // Đặt frame ngoài màn hình dưới cùng
        binding.addClassFrame.setTranslationY(1000f);
        binding.addClassFrame.setVisibility(View.VISIBLE);
        binding.addClassFrame.animate()
                .translationY(0f) // Trượt lên giữa màn hình
                .setDuration(300)
                .start();
        Log.d(TAG, "Showing add class frame");
    }

    private void hideAddClassFrame() {
        binding.addClassFrame.animate()
                .translationY(1000f) // Trượt xuống dưới
                .setDuration(300)
                .withEndAction(() -> binding.addClassFrame.setVisibility(View.GONE))
                .start();
        binding.classNameInput.setText("");
        binding.classTimeInput.setText("");
        Log.d(TAG, "Hiding add class frame");
    }

    private void setupMenuBar() {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(view -> {
            Toast.makeText(getContext(), "Already on Classes", Toast.LENGTH_SHORT).show();
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