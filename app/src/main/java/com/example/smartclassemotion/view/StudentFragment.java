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
import android.widget.ArrayAdapter;
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
import com.example.smartclassemotion.models.ClassItem;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.models.StudentClasses;
import com.example.smartclassemotion.utils.ClassListCallback;
import com.example.smartclassemotion.utils.OnImageUploadedCallback;
import com.example.smartclassemotion.utils.OnOperationCompleteCallback;
import com.example.smartclassemotion.utils.OnStudentActionListener;
import com.example.smartclassemotion.utils.OnStudentCountCallback;
import com.example.smartclassemotion.utils.StudentListCallback;
import com.example.smartclassemotion.viewmodel.StudentAdapter;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        userId = getArguments() != null ? getArguments().getString("user_id") : firebaseHelper.getCurrentUserId();
        studentList = new ArrayList<>();
        originalStudentList = new ArrayList<>();

        binding.studentRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentAdapter = new StudentAdapter(studentList, this);
        binding.studentRecycleView.setAdapter(studentAdapter);

        if (userId != null) {
            loadStudentsByUserId(userId);
        } else {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        }

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

        // Thêm sự kiện khi thoát khỏi SearchView
        binding.studentSearch.setOnCloseListener(() -> {
            filterStudent("");
            return false;
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                Toast.makeText(getContext(), "Image selected: " + selectedImageUri.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.addBtn.setOnClickListener(v -> showAddStudentDialog());

        setupMenuBar();

        return root;
    }

    private void showAddStudentDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        FragmentAddStudentBinding dialogBinding = FragmentAddStudentBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        List<ClassItem> classList = new ArrayList<>();
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.classSpinner.setAdapter(classAdapter);

        if (userId != null) {
            firebaseHelper.getClasses(userId, new ClassListCallback() {
                @Override
                public void onClassesLoaded(List<ClassItem> classItems) {
                    classList.clear();
                    classList.addAll(classItems);
                    List<String> classNames = new ArrayList<>();
                    for (ClassItem classItem : classItems) {
                        classNames.add(classItem.getClassName());
                    }
                    classAdapter.clear();
                    classAdapter.addAll(classNames);
                    classAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError(String errorMessage) {

                }
            });
        } else {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        }

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
            int classPosition = dialogBinding.classSpinner.getSelectedItemPosition();
            String classId = classPosition >= 0 && classPosition < classList.size() ? classList.get(classPosition).getClassId() : null;

            if (name.isEmpty() || dateOfBirthObj == null || gender.isEmpty() || email.isEmpty() || phone.isEmpty() || classId == null) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            }

            Timestamp dateOfBirth = new Timestamp(((Long) dateOfBirthObj) / 1000, 0);
            String studentId = UUID.randomUUID().toString();

            firebaseHelper.getAllStudentCount(new OnStudentCountCallback() {
                @Override
                public void onCountLoaded(long count) {
                    String studentCode = String.format("HS%03d", count + 1);
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

                    if (selectedImageUri != null) {
                        firebaseHelper.uploadImage(selectedImageUri, "avatars/" + studentId, new OnImageUploadedCallback() {
                            @Override
                            public void onImageUploaded(String imageUrl) {
                                student.setAvatarUrl(imageUrl);
                                saveStudent(student, classId, name, dialog);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error uploading image", e);
                                Toast.makeText(getContext(), "Error uploading image", Toast.LENGTH_SHORT).show();
                                student.setAvatarUrl("ic_profile");
                                saveStudent(student, classId, name, dialog);
                            }
                        });
                    } else {
                        student.setAvatarUrl("ic_profile");
                        saveStudent(student, classId, name, dialog);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error getting student count", e);
                    Toast.makeText(getContext(), "Error getting student count", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    private void saveStudent(Student student, String classId, String studentName, BottomSheetDialog dialog) {
        firebaseHelper.addStudent(student, new OnOperationCompleteCallback() {
            @Override
            public void onSuccess() {
                String studentClassesId = UUID.randomUUID().toString();
                StudentClasses studentClasses = new StudentClasses(studentClassesId, student.getStudentId(), classId, Timestamp.now());
                firebaseHelper.addStudentClass(studentClasses, new OnOperationCompleteCallback() {
                    @Override
                    public void onSuccess() {
                        studentList.add(student);
                        originalStudentList.add(student);
                        studentAdapter.notifyDataSetChanged();
                        binding.studentCount.setText(String.valueOf(studentList.size()));
                        Toast.makeText(getContext(), "Student " + studentName + " added successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error adding student class", e);
                        Toast.makeText(getContext(), "Error adding student class", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error adding student", e);
                Toast.makeText(getContext(), "Error adding student", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "No students found", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), "Error loading students: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterStudent(String query) {
        List<Student> filteredList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            // Nếu query rỗng, khôi phục danh sách gốc
            filteredList.addAll(originalStudentList);
        } else {
            // Lọc danh sách dựa trên query
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
        Bundle bundle = new Bundle();
        bundle.putString("student_id", student.getStudentId());
        bundle.putString("user_id", userId); // Đảm bảo userId cũng được truyền
        NavController navController = NavHostFragment.findNavController(this);
        android.util.Log.d("StudentFragment", "Navigating with student_id: " + student.getStudentId());
        navController.navigate(R.id.action_studentFragment_to_studentProfileFragment, bundle);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onDelete(Student student) {
        firebaseHelper.deleteStudent(student.getStudentId(), new OnOperationCompleteCallback() {
            @Override
            public void onSuccess() {
                if(getActivity() != null){
                    studentList.remove(student);
                    originalStudentList.remove(student);
                    studentAdapter.notifyDataSetChanged();
                    binding.studentCount.setText(String.valueOf(studentList.size()));
                    Toast.makeText(getContext(), "Student deleted successfully", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error deleting student", e);
                Toast.makeText(getContext(), "Error deleting student", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMenuBar() {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(v -> {
            if (userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            } else {
                navigateToHomeFragment(userId);
            }
        });

        binding.menuReports.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Reports clicked", Toast.LENGTH_SHORT).show();
        });

        binding.menuSettings.setOnClickListener(v -> {
            if (userId == null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            } else {
                navigateToSettingFragment(userId);
            }
        });
    }

    private void navigateToHomeFragment(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_studentFragment_to_homeFragment, bundle);
        Log.d(TAG, "Navigating to HomeFragment with userId: " + userId);
    }

    private void navigateToSettingFragment(String userId) {
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_studentFragment_to_settingFragment, bundle);
        Log.d(TAG, "Navigating to SettingFragment with userId: " + userId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}