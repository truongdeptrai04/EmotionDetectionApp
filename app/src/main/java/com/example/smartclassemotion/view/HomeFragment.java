package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
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
import com.example.smartclassemotion.databinding.FragmentHomeBinding;
import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.utils.ClassListCallback;
import com.example.smartclassemotion.viewmodel.ClassAdapter;
import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private FirebaseHelper firebaseHelper;
    private List<ClassItem> classList;
    private ClassAdapter classAdapter;
    private String userId;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        classList = new ArrayList<>();
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("user_id");
            Log.d(TAG, "Received args - userId: " + userId);
        } else {
            Log.w(TAG, "No arguments received");
            Toast.makeText(getContext(), "No class data received", Toast.LENGTH_SHORT).show();
        }

        binding.classRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        classAdapter = new ClassAdapter(classList, userId);
        binding.classRecyclerView.setAdapter(classAdapter);

        loadClassesFromFireStore(userId);
        setupMenuBar(userId);
        setupAddClassButton(userId);

        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadClassesFromFireStore(String userId) {
        if (userId == null) return;
        firebaseHelper.getClasses(userId, new ClassListCallback() {
            @Override
            public void onClassesLoaded(List<ClassItem> classItems) {
                if (getActivity() != null) {
                    classList.clear();
                    classList.addAll(classItems);
                    classAdapter.notifyDataSetChanged();
                    binding.activeClassCount.setText(String.valueOf(classList.size()));
                    Log.d(TAG, "Classes loaded: " + classList.size());
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Failed to load classes: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load classes: " + errorMessage);
                }
            }
        });
    }

    private void setupAddClassButton(String userId) {
        binding.addBtn.setOnClickListener(view -> {
            Log.d(TAG, "addBtn clicked");
            showAddClassFrame();
        });

        binding.startTimeInput.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {
                        String time = String.format(Locale.getDefault(),"%02d:%02d", hourOfDay, minute);
                        binding.startTimeInput.setText(time);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            );
        dialog.show();
        });

        binding.endTimeInput.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(
                    requireContext(),
                    (view12, hourOfDay, minute) -> {
                        String time = String.format(Locale.getDefault(),"%02d:%02d", hourOfDay, minute);
                        binding.endTimeInput.setText(time);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            );
            dialog.show();
        });

        binding.submitBtn.setOnClickListener(view -> {
            String className = binding.classNameInput.getText().toString().trim();
            String startTimeStr = binding.startTimeInput.getText().toString().trim();
            String endTimeStr = binding.endTimeInput.getText().toString().trim();
            String dayOfWeek = binding.dayOfWeekInput.getSelectedItem() != null ? binding.dayOfWeekInput.getSelectedItem().toString() : "";
            String description = binding.descriptionInput.getText().toString().trim();

            Log.d(TAG, "Submit clicked - ClassName: " + className + ", StartTime: " + startTimeStr + ", EndTime: " + endTimeStr + ", DayOfWeek: " + dayOfWeek + ", Description: " + description);

            if (className.isEmpty()) {
                Toast.makeText(getContext(), "Class name is required", Toast.LENGTH_SHORT).show();
            }
            if (startTimeStr.isEmpty()) {
                Toast.makeText(getContext(), "Start time is required", Toast.LENGTH_SHORT).show();
            }
            if (endTimeStr.isEmpty()) {
                Toast.makeText(getContext(), "End time is required", Toast.LENGTH_SHORT).show();
            }
            if (dayOfWeek.isEmpty()) {
                Toast.makeText(getContext(), "Day of week is required", Toast.LENGTH_SHORT).show();
            }

            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            try{
                startCal.setTime(TIME_FORMAT.parse(startTimeStr));
                endCal.setTime(TIME_FORMAT.parse(endTimeStr));
            }catch (Exception e){
                Toast.makeText(getContext(), "Invalid time format. Use HH:mm (e.g., 10:30)", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid time format: startTime = " + startTimeStr + ", endTime = " + endTimeStr, e);
                return;
            }
            if(!startCal.before(endCal)){
                Toast.makeText(getContext(), "Start time must be before end time", Toast.LENGTH_SHORT).show();
                return;
            }

            Timestamp startTime = new Timestamp(startCal.getTime());
            Timestamp endTime = new Timestamp(endCal.getTime());

            Map<String, Object> classData = new HashMap<>();
            classData.put("userId", userId);
            classData.put("className", className);
            classData.put("startTime", startTime);
            classData.put("endTime", endTime);
            classData.put("dayOfWeek", dayOfWeek);
            classData.put("description", description);

            firebaseHelper.getDb().collection("Classes")
                    .add(classData)
                    .addOnSuccessListener(documentReference -> {
                        String newClassId = documentReference.getId();
                        ClassItem newClass = new ClassItem(newClassId, userId, className, startTime, endTime, dayOfWeek, description);
                        classList.add(newClass);
                        classAdapter.notifyDataSetChanged();
                        binding.activeClassCount.setText(String.valueOf(classList.size()));
                        hideAddClassFrame();
                        Toast.makeText(getContext(), "Class added successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG,"Class added: " + newClassId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to add class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to add class" + e.getMessage());
                    });
        });

        binding.cancelBtn.setOnClickListener(view -> {
            Log.d(TAG, "Cancel clicked");
            hideAddClassFrame();
        });
    }

    private void showAddClassFrame() {
        binding.addClassFrame.setTranslationY(1000f);
        binding.addClassFrame.setVisibility(View.VISIBLE);
        binding.addClassFrame.animate()
                .translationY(0f)
                .setDuration(300)
                .start();
        Log.d(TAG, "Showing add class frame");
    }

    private void hideAddClassFrame() {
        binding.addClassFrame.animate()
                .translationY(1000f)
                .setDuration(300)
                .withEndAction(() -> binding.addClassFrame.setVisibility(View.GONE))
                .start();
        binding.classNameInput.setText("");
        binding.startTimeInput.setText("");
        binding.endTimeInput.setText("");
        binding.descriptionInput.setText("");
        binding.dayOfWeekInput.setSelection(0);
        Log.d(TAG, "Hiding add class frame");
    }

    private void setupMenuBar(String userId) {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(view -> {
        });

        binding.menuReports.setOnClickListener(v -> {
            if(userId == null){
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            } else {
                navigateToReportFragment(userId);
            }
        });

        binding.menuStudent.setOnClickListener(v -> {
            if (userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            } else {
                navigateToStudentFragment(userId);
            }
        });

        binding.menuSettings.setOnClickListener(v -> {
            if (userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            } else {
                navigateToSettingFragment(userId);
            }
        });
    }
    private void navigateToStudentFragment(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_homeFragment_to_studentFragment, bundle);
        Log.d(TAG, "Navigating to StudentFragment with userId: " + userId);
    }

    private void navigateToSettingFragment(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_homeFragment_to_settingFragment, bundle);
        Log.d(TAG, "Navigating to SettingFragment with userId: " + userId);
    }
    private void navigateToReportFragment(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_homeFragment_to_reportFragment, bundle);
        Log.d(TAG, "Navigating to ReportFragment with userId: " + userId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}