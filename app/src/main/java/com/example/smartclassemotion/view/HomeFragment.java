package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import com.example.smartclassemotion.utils.OnMaxClassIdCallback;
import com.example.smartclassemotion.utils.OnOperationCompleteCallback;
import com.example.smartclassemotion.viewmodel.ClassAdapter;
import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements ClassAdapter.OnClassActionListener {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private FirebaseHelper firebaseHelper;
    private List<ClassItem> classList;
    private ClassAdapter classAdapter;
    private String userId;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private boolean isEditMode = false; // Trạng thái: thêm mới (false) hay chỉnh sửa (true)
    private String editingClassId; // Lưu classId của lớp đang chỉnh sửa

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
        classAdapter = new ClassAdapter(classList, userId, this); // Truyền this làm OnClassActionListener
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
            isEditMode = false; // Chế độ thêm mới
            editingClassId = null;
            showAddClassFrame();
            Log.d(TAG, "addBtn clicked");
        });

        binding.startTimeInput.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {
                        String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
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
                        String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
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
                return;
            }
            if (startTimeStr.isEmpty()) {
                Toast.makeText(getContext(), "Start time is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (endTimeStr.isEmpty()) {
                Toast.makeText(getContext(), "End time is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dayOfWeek.isEmpty()) {
                Toast.makeText(getContext(), "Day of week is required", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            try {
                startCal.setTime(TIME_FORMAT.parse(startTimeStr));
                endCal.setTime(TIME_FORMAT.parse(endTimeStr));
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Invalid time format. Use HH:mm (e.g., 10:30)", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid time format: startTime = " + startTimeStr + ", endTime = " + endTimeStr, e);
                return;
            }
            if (!startCal.before(endCal)) {
                Toast.makeText(getContext(), "Start time must be before end time", Toast.LENGTH_SHORT).show();
                return;
            }

            Timestamp startTime = new Timestamp(startCal.getTime());
            Timestamp endTime = new Timestamp(endCal.getTime());

            if (isEditMode) {
                // Chế độ chỉnh sửa
                updateClass(editingClassId, className, startTime, endTime, dayOfWeek, description);
            } else {
                // Chế độ thêm mới
                firebaseHelper.getMaxClassId(userId, new OnMaxClassIdCallback() {
                    @Override
                    public void onMaxClassIdFount(int maxNumber) {
                        Map<String, Object> classData = new HashMap<>();
                        classData.put("userId", userId);
                        classData.put("className", className);
                        classData.put("startTime", startTime);
                        classData.put("endTime", endTime);
                        classData.put("dayOfWeek", dayOfWeek);
                        classData.put("description", description);

                        firebaseHelper.addClass(classData, "", maxNumber + 1, new OnOperationCompleteCallback() {
                            @Override
                            public void onSuccess() {
                                String newClassId = String.format("cl_%03d", maxNumber + 1);
                                ClassItem newClass = new ClassItem(newClassId, userId, className, startTime, endTime, dayOfWeek, description);
                                classList.add(newClass);
                                classAdapter.notifyDataSetChanged();
                                binding.activeClassCount.setText(String.valueOf(classList.size()));
                                hideAddClassFrame();
                                Toast.makeText(getContext(), "Thêm lớp thành công", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Thêm lớp: " + newClassId);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Lỗi khi thêm lớp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Lỗi khi thêm lớp: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Lỗi khi tạo mã lớp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Lỗi khi lấy classId lớn nhất: " + e.getMessage());
                    }
                });
            }
        });

        binding.cancelBtn.setOnClickListener(view -> {
            Log.d(TAG, "Cancel clicked");
            hideAddClassFrame();
        });
    }

    private void showAddClassFrame() {
        binding.addClassFrame.setTranslationY(1000f);
        binding.addClassFrame.setVisibility(View.VISIBLE);
        binding.addClassTitle.setText("Add New Class"); // Tiêu đề khi thêm mới
        binding.addClassFrame.animate()
                .translationY(0f)
                .setDuration(300)
                .start();
        // Xóa dữ liệu cũ
        binding.classNameInput.setText("");
        binding.startTimeInput.setText("");
        binding.endTimeInput.setText("");
        binding.descriptionInput.setText("");
        binding.dayOfWeekInput.setSelection(0);
        Log.d(TAG, "Showing add class frame");
    }

    private void showAddClassFrame(ClassItem classItem) {
        isEditMode = true; // Chế độ chỉnh sửa
        editingClassId = classItem.getClassId();
        binding.addClassFrame.setTranslationY(1000f);
        binding.addClassFrame.setVisibility(View.VISIBLE);
        binding.addClassTitle.setText("Edit Class"); // Tiêu đề khi chỉnh sửa
        binding.addClassFrame.animate()
                .translationY(0f)
                .setDuration(300)
                .start();
        // Điền thông tin lớp học
        binding.classNameInput.setText(classItem.getClassName());
        binding.startTimeInput.setText(TIME_FORMAT.format(classItem.getStartTime().toDate()));
        binding.endTimeInput.setText(TIME_FORMAT.format(classItem.getEndTime().toDate()));
        binding.descriptionInput.setText(classItem.getDescription());
        // Đặt dayOfWeek
        String[] days = getResources().getStringArray(R.array.day_of_week);
        for (int i = 0; i < days.length; i++) {
            if (days[i].equals(classItem.getDayOfWeek())) {
                binding.dayOfWeekInput.setSelection(i);
                break;
            }
        }
        Log.d(TAG, "Showing edit class frame for classId: " + classItem.getClassId());
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
        binding.addClassTitle.setText("Add New Class"); // Reset tiêu đề
        isEditMode = false; // Reset chế độ
        editingClassId = null;
        Log.d(TAG, "Hiding add class frame");
    }

    private void updateClass(String classId, String className, Timestamp startTime, Timestamp endTime, String dayOfWeek, String description) {
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid class ID", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid class ID for update");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("className", className);
        updates.put("startTime", startTime);
        updates.put("endTime", endTime);
        updates.put("dayOfWeek", dayOfWeek);
        updates.put("description", description);

        firebaseHelper.getDb().collection("Classes")
                .document(classId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật classList
                    for (int i = 0; i < classList.size(); i++) {
                        if (classList.get(i).getClassId().equals(classId)) {
                            ClassItem updatedClass = new ClassItem(classId, userId, className, startTime, endTime, dayOfWeek, description);
                            classList.set(i, updatedClass);
                            classAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    binding.activeClassCount.setText(String.valueOf(classList.size()));
                    hideAddClassFrame();
                    Toast.makeText(getContext(), "Cập nhật lớp thành công", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Cập nhật lớp: " + classId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi cập nhật lớp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi khi cập nhật lớp: " + e.getMessage());
                });
    }

    @Override
    public void onEditClass(ClassItem classItem) {
        showAddClassFrame(classItem);
    }

    @Override
    public void onDeleteClass(ClassItem classItem){
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to delete class " + classItem.getClassName() + "?")
                .setPositiveButton("Yes", (dialog, which)->{
                    firebaseHelper.deleteClass(classItem.getClassId(), new OnOperationCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            int position = -1;
                            for(int i = 0; i < classList.size(); i++){
                                if(classList.get(i).getClassId().equals(classItem.getClassId())){
                                    position = i;
                                    break;
                                }
                            }
                            if(position != -1){
                                classList.remove(position);
                                classAdapter.notifyItemRemoved(position);
                                binding.activeClassCount.setText(String.valueOf(classList.size()));
                            }
                            Toast.makeText(requireContext(), "Class deleted successfully", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Class deleted successfully: " + classItem.getClassId());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "Failed to delete class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to delete class: " + e.getMessage());
                        }
                    });
                })
                .setNegativeButton("No", null)
                .setIcon(R.drawable.ic_dialog_alert)
                .show();
    }

    private void setupMenuBar(String userId) {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(view -> {
        });

        binding.menuReports.setOnClickListener(v -> {
            if (userId == null) {
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
        navController.navigate(R.id.action_homeFragment_to_alertFragment, bundle);
        Log.d(TAG, "Navigating to ReportFragment with userId: " + userId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}