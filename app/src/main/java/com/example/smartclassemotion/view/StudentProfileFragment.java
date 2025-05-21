package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
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
            Log.e(TAG, "Student ID hoặc User ID là null");
            showToast("Student ID hoặc User ID là null");
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
            Log.d(TAG, "Nhận student_id: " + studentId + ", user_id: " + userId);
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
        Log.d(TAG, "Đang tải dữ liệu học sinh cho student_id: " + studentId);
        firebaseHelper.getDb().collection("Students").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        student = documentSnapshot.toObject(Student.class);
                        if (student != null) {
                            Log.d(TAG, "Đã tải học sinh: " + student.getStudentName() + ", dữ liệu: " + documentSnapshot.getData());
                            updateStudentUI();
                            loadAvatarImage(student.getAvatarUrl());
                        } else {
                            Log.e(TAG, "Không thể parse đối tượng học sinh từ Firestore");
                            showToast("Lỗi khi tải dữ liệu học sinh");
                            resetFields();
                        }
                    } else {
                        Log.e(TAG, "Tài liệu học sinh không tồn tại cho student_id: " + studentId);
                        showToast("Không tìm thấy học sinh");
                        resetFields();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải dữ liệu học sinh: " + e.getMessage(), e);
                    showToast("Lỗi khi tải dữ liệu học sinh: " + e.getMessage());
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
        binding.imvProfile.setImageResource(R.drawable.ic_profile); // Ảnh mặc định
    }

    private void loadAvatarImage(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            Log.w(TAG, "avatarUrl là null hoặc rỗng cho student_id: " + studentId);
            binding.imvProfile.setImageResource(R.drawable.ic_profile);
            return;
        }

        // Giải mã Base64 ngoài luồng chính
        new DecodeBase64Task().execute(avatarUrl);
    }

    private class DecodeBase64Task extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            String base64Image = params[0];
            try {
                // Giải mã Base64 thành byte array
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                // Tùy chọn để giảm kích thước ảnh nếu cần
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Giảm kích thước ảnh xuống 1/2 để tối ưu bộ nhớ
                // Chuyển byte array thành Bitmap
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Chuỗi Base64 không hợp lệ: " + e.getMessage(), e);
                return null;
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Lỗi bộ nhớ khi giải mã Base64: " + e.getMessage(), e);
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi giải mã Base64: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                binding.imvProfile.setImageBitmap(bitmap);
                Log.d(TAG, "Đã gán ảnh avatar cho student_id: " + studentId);
            } else {
                binding.imvProfile.setImageResource(R.drawable.ic_profile);
                Log.w(TAG, "Không thể giải mã ảnh avatar, sử dụng ảnh mặc định cho student_id: " + studentId);
            }
        }
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
                    Log.d(TAG, "Bỏ qua lựa chọn spinner ban đầu");
                    return;
                }
                if (className.isEmpty()) {
                    Log.e(TAG, "Danh sách lớp rỗng");
                    return;
                }
                String selectedClassId = classIds.get(position);
                Log.d(TAG, "Đã chọn lớp: " + className.get(position) + ", classId: " + selectedClassId);
                updateEmotionChart(selectedClassId);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                Log.d(TAG, "Không có lớp nào được chọn");
            }
        });
        loadClasses();
    }

    private void loadClasses() {
        if (userId == null) {
            Log.e(TAG, "User ID là null, không thể tải danh sách lớp");
            showToast("Không tìm thấy User ID");
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
                        Log.w(TAG, "Không tìm thấy lớp nào cho student_id: " + studentId);
                        showToast("Không tìm thấy lớp nào cho học sinh này");
                        classSpinnerAdapter.notifyDataSetChanged();
                        binding.chartEmotion.clearChart();
                        binding.chartEmotion.addPieSlice(new PieModel("Không có lớp", 100, 0xFF9E9E9E));
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
                                Log.d(TAG, "Đã tải danh sách lớp: " + className.size() + " lớp cho student_id: " + studentId);
                                if (!className.isEmpty()) {
                                    binding.spinnerClass.setSelection(0);
                                    updateEmotionChart(classIds.get(0));
                                } else {
                                    Log.w(TAG, "Không tìm thấy lớp nào trong collection Classes");
                                    showToast("Không tìm thấy lớp phù hợp");
                                    binding.chartEmotion.clearChart();
                                    binding.chartEmotion.addPieSlice(new PieModel("Không có lớp", 100, 0xFF9E9E9E));
                                    binding.chartEmotion.startAnimation();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi khi tải danh sách lớp: " + e.getMessage(), e);
                                showToast("Lỗi khi tải danh sách lớp: " + e.getMessage());
                                binding.chartEmotion.clearChart();
                                binding.chartEmotion.addPieSlice(new PieModel("Lỗi", 100, 0xFF9E9E9E));
                                binding.chartEmotion.startAnimation();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải StudentClasses: " + e.getMessage(), e);
                    showToast("Lỗi khi tải danh sách lớp của học sinh: " + e.getMessage());
                    binding.chartEmotion.clearChart();
                    binding.chartEmotion.addPieSlice(new PieModel("Lỗi", 100, 0xFF9E9E9E));
                    binding.chartEmotion.startAnimation();
                });
    }

    private void updateEmotionChart(String classId) {
        binding.chartEmotion.clearChart();
        if (studentId == null || classId == null || classId.isEmpty()) {
            Log.e(TAG, "Student ID hoặc Class ID là null/rỗng, không thể cập nhật biểu đồ cảm xúc");
            binding.chartEmotion.addPieSlice(new PieModel("Không có dữ liệu", 100, 0xFF9E9E9E));
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
                    Log.d(TAG, "Không tìm thấy thống kê cảm xúc cho student_id: " + studentId + ", class_id: " + classId);
                    binding.chartEmotion.addPieSlice(new PieModel("Không có dữ liệu", 100, 0xFF9E9E9E));
                }

                Log.d(TAG, "Đã tải thống kê cảm xúc: happy=" + happy + ", sad=" + sad + ", neutral=" + neutral + ", angry=" + angry + ", fear=" + fear + ", surprise=" + surprise);
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
            showToast("Dữ liệu học sinh là null");
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
                    Log.d(TAG, "Đã cập nhật học sinh: " + name);
                    showToast("Cập nhật học sinh thành công");
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
                    Log.e(TAG, "Lỗi khi cập nhật học sinh: " + e.getMessage(), e);
                    showToast("Lỗi khi cập nhật học sinh: " + e.getMessage());
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
            showToast("Không tìm thấy User ID");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(actionId, bundle);
        Log.d(TAG, "Điều hướng đến Action " + actionId + " với userId: " + userId);
    }

    private boolean validateInput(String name, String gender, String phone, String email) {
        if (name.isEmpty()) {
            showToast("Tên không được để trống");
            return false;
        }
        if (gender.isEmpty()) {
            showToast("Giới tính không được để trống");
            return false;
        }
        if (!phone.isEmpty() && !phone.matches("^[0-9+]{9,15}$")) {
            showToast("Số điện thoại không hợp lệ (9-15 chữ số)");
            return false;
        }
        if (!email.isEmpty() && !email.matches("[A-Za-z0-9+_.-]+@(.+)$")) {
            showToast("Định dạng email không hợp lệ");
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