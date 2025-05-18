package com.example.smartclassemotion.view;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentAddStudentBinding;
import com.example.smartclassemotion.databinding.FragmentStudentBinding;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.utils.OnImageUploadedCallback;
import com.example.smartclassemotion.utils.OnMaxStudentIdCallback;
import com.example.smartclassemotion.utils.OnOperationCompleteCallback;
import com.example.smartclassemotion.utils.OnStudentActionListener;
import com.example.smartclassemotion.utils.StudentListCallback;
import com.example.smartclassemotion.viewmodel.StudentAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StudentFragment extends Fragment implements OnStudentActionListener {
    private FragmentStudentBinding binding;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private List<Student> studentList;
    private List<Student> originalStudentList;
    private StudentAdapter studentAdapter;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        // Đặt adjustResize khi vào StudentFragment
//        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        userId = getArguments() != null ? getArguments().getString("user_id") : firebaseHelper.getCurrentUserId();
        studentList = new ArrayList<>();
        originalStudentList = new ArrayList<>();

        setupRecyclerView();
        setupSearchView();
        setupImagePicker();
        setupAddButton();
        setupMenuBar();

        if (userId != null) {
            loadStudentsByUserId(userId);
        } else {
            showToast("User ID not found");
        }

        return root;
    }

    private void setupRecyclerView() {
        binding.studentRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentAdapter = new StudentAdapter(studentList, this);
        binding.studentRecycleView.setAdapter(studentAdapter);
    }

    private void setupSearchView() {
        binding.studentSearch.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterStudent(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterStudent(newText);
                return true;
            }
        });
        binding.studentSearch.setOnCloseListener(() -> {
            filterStudent("");
            return false;
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                showToast("Image selected: " + selectedImageUri.toString());
            }
        });
    }

    private void setupAddButton() {
        binding.addBtn.setOnClickListener(v -> showAddStudentDialog());
    }

    private void showAddStudentDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        FragmentAddStudentBinding dialogBinding = FragmentAddStudentBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        // Đặt chiều cao tối thiểu và trạng thái mở rộng cho BottomSheetDialog
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setPeekHeight(0); // Loại bỏ trạng thái thu gọn
        }

        // Tự động cuộn đến trường đang focus
        ScrollView scrollView = (ScrollView) dialogBinding.getRoot();
        dialogBinding.emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.post(() -> scrollView.smoothScrollTo(0, dialogBinding.emailInputLayout.getTop()));
            }
        });
        dialogBinding.phoneInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.post(() -> scrollView.smoothScrollTo(0, dialogBinding.phoneInputLayout.getTop()));
            }
        });

        dialogBinding.dateOfBirthInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        dialogBinding.dateOfBirthInput.setTag(calendar.getTimeInMillis());
                        dialogBinding.dateOfBirthInput.setText(String.format("%d-%02d-%02d", year, month + 1, dayOfMonth));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        dialogBinding.uploadAvatarBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        dialogBinding.cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.saveBtn.setOnClickListener(v -> {
            String name = dialogBinding.nameInput.getText() != null ? dialogBinding.nameInput.getText().toString().trim() : "";
            Object dateOfBirthObj = dialogBinding.dateOfBirthInput.getTag();
            String gender = dialogBinding.genderSpinner.getSelectedItem() != null ? dialogBinding.genderSpinner.getSelectedItem().toString() : "";
            String email = dialogBinding.emailInput.getText() != null ? dialogBinding.emailInput.getText().toString().trim() : "";
            String phone = dialogBinding.phoneInput.getText() != null ? dialogBinding.phoneInput.getText().toString().trim() : "";

            if (!validateInput(name, dateOfBirthObj, gender, email, phone)) {
                return;
            }

            Timestamp dateOfBirth = new Timestamp(((Long) dateOfBirthObj) / 1000, 0);

            firebaseHelper.getMaxStudentId(userId, new OnMaxStudentIdCallback() {
                @Override
                public void onMaxStudentIdFound(int maxNumber) {
                    int newNumber = maxNumber + 1;
                    String studentId = String.format("std_%03d", newNumber);
                    String studentCode = String.format("HS%03d", newNumber);
                    Student student = new Student();
                    student.setStudentId(studentId);
                    student.setStudentCode(studentCode);
                    student.setStudentName(name);
                    student.setDateOfBirth(dateOfBirth);
                    student.setGender(gender);
                    student.setEmail(email);
                    student.setPhone(phone);
                    student.setStatus("Active");
                    student.setNotes("");
                    student.setUserId(userId);

                    if (selectedImageUri != null) {
                        firebaseHelper.uploadImage(selectedImageUri, "avatars/" + studentId, new OnImageUploadedCallback() {
                            @Override
                            public void onImageUploaded(String imageUrl) {
                                student.setAvatarUrl(imageUrl);
                                saveStudent(student, name, dialog);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error uploading image: " + e.getMessage(), e);
                                showToast("Error uploading image");
                                student.setAvatarUrl("ic_profile");
                                saveStudent(student, name, dialog);
                            }
                        });
                    } else {
                        student.setAvatarUrl("ic_profile");
                        saveStudent(student, name, dialog);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error getting student count: " + e.getMessage(), e);
                    showToast("Error getting student count");
                }
            });
        });
        dialog.show();
    }

    private boolean validateInput(String name, Object dateOfBirthObj, String gender, String email, String phone) {
        if (name.isEmpty()) {
            showToast("Name cannot be empty");
            return false;
        }
        if (dateOfBirthObj == null) {
            showToast("Please select date of birth");
            return false;
        }
        if (gender.isEmpty()) {
            showToast("Please select gender");
            return false;
        }
        if (email.isEmpty()) {
            showToast("Email cannot be empty");
            return false;
        }
        if (!email.matches("[A-Za-z0-9+_.-]+@(.+)$")) {
            showToast("Invalid email format");
            return false;
        }
        if (phone.isEmpty()) {
            showToast("Phone cannot be empty");
            return false;
        }
        if (!phone.matches("^[0-9+]{9,15}$")) {
            showToast("Phone number is invalid (9-15 digits)");
            return false;
        }
        return true;
    }

    private void saveStudent(Student student, String studentName, BottomSheetDialog dialog) {
        if (student.getStudentId() == null) {
            showToast("Student ID not found");
            return;
        }
        firebaseHelper.addStudent(student, new OnOperationCompleteCallback() {
            @Override
            public void onSuccess() {
                studentList.add(student);
                originalStudentList.add(student);
                studentAdapter.notifyDataSetChanged();
                binding.studentCount.setText(String.valueOf(studentList.size()));
                showToast("Student " + studentName + " added successfully");
                dialog.dismiss();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error adding student: " + e.getMessage(), e);
                showToast("Error adding student");
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadStudentsByUserId(String userId) {
        firebaseHelper.getAllStudentsByUserId(userId, new StudentListCallback() {
            @Override
            public void onStudentsLoaded(List<Student> students) {
                if (getActivity() != null) {
                    studentList.clear();
                    originalStudentList.clear();
                    studentList.addAll(students);
                    originalStudentList.addAll(students);
                    studentAdapter.notifyDataSetChanged();
                    binding.studentCount.setText(String.valueOf(studentList.size()));
                    if (studentList.isEmpty()) {
                        showToast("No students found");
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                showToast("Error loading students: " + errorMessage);
            }
        });
    }

    private void filterStudent(String query) {
        List<Student> filteredList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(originalStudentList);
        } else {
            for (Student student : originalStudentList) {
                if (student.getStudentName().toLowerCase().contains(query.toLowerCase()) ||
                        student.getStudentCode().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(student);
                }
            }
        }
        studentAdapter.updateList(filteredList);
        binding.studentCount.setText(String.valueOf(filteredList.size()));
    }

    @Override
    public void onEdit(Student student) {
        if (userId == null) {
            showToast("User ID not found");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("student_id", student.getStudentId());
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        Log.d(TAG, "Navigating to StudentProfileFragment with student_id: " + student.getStudentId());
        navController.navigate(R.id.action_studentFragment_to_studentProfileFragment, bundle);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onDelete(Student student) {
        firebaseHelper.deleteStudent(student.getStudentId(), new OnOperationCompleteCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    studentList.remove(student);
                    originalStudentList.remove(student);
                    studentAdapter.notifyDataSetChanged();
                    binding.studentCount.setText(String.valueOf(studentList.size()));
                    showToast("Student deleted successfully");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error deleting student: " + e.getMessage(), e);
                showToast("Error deleting student");
            }
        });
    }

    private void setupMenuBar() {
        binding.menuClasses.setOnClickListener(v -> navigateToHomeFragment());
        binding.menuReports.setOnClickListener(v -> navigateToReportFragment());
        binding.menuSettings.setOnClickListener(v -> navigateToSettingFragment());
    }

    private void navigateToHomeFragment() {
        navigateTo(R.id.action_studentFragment_to_homeFragment);
    }

    private void navigateToReportFragment() {
        navigateTo(R.id.action_studentFragment_to_alertFragment);
    }

    private void navigateToSettingFragment() {
        navigateTo(R.id.action_studentFragment_to_settingFragment);
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
        Log.d(TAG, "Navigating to actionId: " + actionId + " with userId: " + userId);
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        binding = null;
    }
}