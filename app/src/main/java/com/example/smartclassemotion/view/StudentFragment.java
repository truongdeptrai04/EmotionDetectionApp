package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentAddStudentBinding;
import com.example.smartclassemotion.databinding.FragmentStudentBinding;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.utils.OnMaxStudentIdCallback;
import com.example.smartclassemotion.utils.OnMultipleImagesUploadedCallback;
import com.example.smartclassemotion.utils.OnOperationCompleteCallback;
import com.example.smartclassemotion.utils.OnStudentActionListener;
import com.example.smartclassemotion.utils.StudentListCallback;
import com.example.smartclassemotion.viewmodel.StudentAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentFragment extends Fragment implements OnStudentActionListener {
    private static final String TAG = "StudentFragment";
    private FragmentStudentBinding binding;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private List<Student> studentList;
    private List<Student> originalStudentList;
    private StudentAdapter studentAdapter;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private List<Uri> selectedImageUris;
    private Uri selectedAvatarUri;
    private ImageAdapter imageAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper(getContext());
        userId = getArguments() != null ? getArguments().getString("user_id") : firebaseHelper.getCurrentUserId();
        studentList = new ArrayList<>();
        originalStudentList = new ArrayList<>();
        selectedImageUris = new ArrayList<>();
        selectedAvatarUri = null;

        setupRecyclerView();
        setupSearchView();
        setupImagePicker();
        setupAddButton();
        setupMenuBar();

        if (userId != null) {
            loadStudentsByUserId(userId);
        } else {
            showToast("User ID không tìm thấy");
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
                List<Uri> newImageUris = new ArrayList<>();
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                        newImageUris.add(imageUri);
                    }
                } else if (result.getData().getData() != null) {
                    newImageUris.add(result.getData().getData());
                }

                // Kiểm tra trùng lặp dựa trên ID ảnh
                Set<String> existingImageIds = new HashSet<>();
                for (Uri uri : selectedImageUris) {
                    String imageId = getImageIdFromUri(uri);
                    if (imageId != null) {
                        existingImageIds.add(imageId);
                    }
                }

                for (Uri newUri : newImageUris) {
                    String newImageId = getImageIdFromUri(newUri);
                    if (newImageId != null && !existingImageIds.contains(newImageId)) {
                        selectedImageUris.add(newUri);
                        existingImageIds.add(newImageId);
                    }
                }

                Log.d(TAG, "Tổng số ảnh đã chọn: " + selectedImageUris.size() + ", URIs: " + selectedImageUris);
                showToast(selectedImageUris.size() + " ảnh đã được chọn");

                if (imageAdapter != null) {
                    imageAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Cập nhật image_recycler_view với " + selectedImageUris.size() + " ảnh");
                }
            }
        });
    }

    private String getImageIdFromUri(Uri uri) {
        String imageId = null;
        ContentResolver resolver = getContext().getContentResolver();
        String[] projection = { MediaStore.Images.Media._ID };
        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                imageId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy ID ảnh từ Uri: " + uri, e);
        }
        return imageId;
    }

    private void setupAddButton() {
        binding.addBtn.setOnClickListener(v -> showAddStudentDialog());
    }

    private void showAddStudentDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        FragmentAddStudentBinding dialogBinding = FragmentAddStudentBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setPeekHeight(0);
        }

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

        imageAdapter = new ImageAdapter(selectedImageUris, selectedAvatarUri, uri -> {
            selectedAvatarUri = uri;
            Log.d(TAG, "Avatar được chọn: " + (selectedAvatarUri != null ? selectedAvatarUri : "Không có"));
            showToast("Đã chọn avatar: " + (selectedAvatarUri != null ? selectedAvatarUri.toString() : "Không có"));
            imageAdapter.notifyDataSetChanged();
        });
        dialogBinding.imageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        dialogBinding.imageRecyclerView.setAdapter(imageAdapter);

        dialogBinding.uploadAvatarBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            if (!selectedImageUris.isEmpty()) {
                ClipData clipData = ClipData.newUri(getContext().getContentResolver(), "Selected Images", selectedImageUris.get(0));
                for (int i = 1; i < selectedImageUris.size(); i++) {
                    clipData.addItem(new ClipData.Item(selectedImageUris.get(i)));
                }
                intent.setClipData(clipData);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            imagePickerLauncher.launch(intent);
        });

        dialogBinding.cancelBtn.setOnClickListener(v -> {
            selectedImageUris.clear();
            selectedAvatarUri = null;
            imageAdapter = null;
            dialog.dismiss();
        });

        dialogBinding.saveBtn.setOnClickListener(v -> {
            if (selectedImageUris.size() < 7) {
                showToast("Vui lòng chọn ít nhất 7 ảnh");
                return;
            }

            String name = dialogBinding.nameInput.getText() != null ? dialogBinding.nameInput.getText().toString().trim() : "";
            Object dateOfBirthObj = dialogBinding.dateOfBirthInput.getTag();
            String gender = dialogBinding.genderSpinner.getSelectedItem() != null ? dialogBinding.genderSpinner.getSelectedItem().toString() : "";
            String email = dialogBinding.emailInput.getText() != null ? dialogBinding.emailInput.getText().toString().trim() : "";
            String phone = dialogBinding.phoneInput.getText() != null ? dialogBinding.phoneInput.getText().toString().trim() : "";

            if (!validateInput(name, dateOfBirthObj, gender, email, phone)) {
                return;
            }

            if (selectedAvatarUri == null) {
                showToast("Vui lòng chọn một avatar từ các ảnh đã tải lên");
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

                    // Chuyển avatar thành base64
                    String avatarBase64 = firebaseHelper.convertImageToBase64(selectedAvatarUri);
                    if (avatarBase64 == null) {
                        showToast("Lỗi khi xử lý ảnh avatar");
                        Log.e(TAG, "Không thể chuyển avatar thành base64");
                        return;
                    }
                    student.setAvatarUrl(avatarBase64);

                    dialogBinding.uploadProgress.setVisibility(View.VISIBLE);
                    firebaseHelper.uploadMultipleImages(studentId, name, selectedImageUris, new OnMultipleImagesUploadedCallback() {
                        @Override
                        public void onImagesUploaded() {
                            mainHandler.post(() -> {
                                dialogBinding.uploadProgress.setVisibility(View.GONE);
                                Log.d(TAG, "Gửi ảnh đến server thành công cho học sinh: " + name);
                                saveStudent(student, name, dialog);
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            mainHandler.post(() -> {
                                dialogBinding.uploadProgress.setVisibility(View.GONE);
                                Log.e(TAG, "Lỗi khi gửi ảnh đến server: " + e.getMessage(), e);
                                // Kiểm tra Firestore xem server đã lưu chưa
                                firebaseHelper.getDb().collection("Students").document(studentId).get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                Log.d(TAG, "Học sinh " + studentId + " đã được server lưu vào Firestore");
                                                saveStudent(student, name, dialog);
                                            } else {
                                                showToast("Lỗi khi gửi ảnh đến server, vui lòng thử lại");
                                            }
                                        })
                                        .addOnFailureListener(err -> {
                                            Log.e(TAG, "Lỗi khi kiểm tra Firestore: " + err.getMessage(), err);
                                            showToast("Lỗi khi gửi ảnh đến server, vui lòng thử lại");
                                        });
                            });
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    mainHandler.post(() -> {
                        Log.e(TAG, "Lỗi khi lấy số lượng học sinh: " + e.getMessage(), e);
                        showToast("Lỗi khi lấy số lượng học sinh");
                    });
                }
            });
        });
        dialog.show();
    }

    private boolean validateInput(String name, Object dateOfBirthObj, String gender, String email, String phone) {
        if (name.isEmpty()) {
            showToast("Tên không được để trống");
            return false;
        }
        if (dateOfBirthObj == null) {
            showToast("Vui lòng chọn ngày sinh");
            return false;
        }
        if (gender.isEmpty()) {
            showToast("Vui lòng chọn giới tính");
            return false;
        }
        if (email.isEmpty()) {
            showToast("Email không được để trống");
            return false;
        }
        if (!email.matches("[A-Za-z0-9+_.-]+@(.+)$")) {
            showToast("Định dạng email không hợp lệ");
            return false;
        }
        if (phone.isEmpty()) {
            showToast("Số điện thoại không được để trống");
            return false;
        }
        if (!phone.matches("^[0-9+]{9,15}$")) {
            showToast("Số điện thoại không hợp lệ (9-15 chữ số)");
            return false;
        }
        return true;
    }

    private void saveStudent(Student student, String studentName, BottomSheetDialog dialog) {
        if (student.getStudentId() == null) {
            showToast("Không tìm thấy ID học sinh");
            return;
        }
        firebaseHelper.addStudent(student, new OnOperationCompleteCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    studentList.add(student);
                    originalStudentList.add(student);
                    studentAdapter.notifyDataSetChanged();
                    binding.studentCount.setText(String.valueOf(studentList.size()));
                    showToast("Học sinh " + studentName + " được thêm thành công");
                    selectedImageUris.clear();
                    selectedAvatarUri = null;
                    imageAdapter = null;
                    dialog.dismiss();
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    Log.e(TAG, "Lỗi khi thêm học sinh: " + e.getMessage(), e);
                    showToast("Lỗi khi thêm học sinh");
                });
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadStudentsByUserId(String userId) {
        firebaseHelper.getAllStudentsByUserId(userId, new StudentListCallback() {
            @Override
            public void onStudentsLoaded(List<Student> students) {
                mainHandler.post(() -> {
                    if (getActivity() != null) {
                        studentList.clear();
                        originalStudentList.clear();
                        studentList.addAll(students);
                        originalStudentList.addAll(students);
                        studentAdapter.notifyDataSetChanged();
                        binding.studentCount.setText(String.valueOf(studentList.size()));
                        if (studentList.isEmpty()) {
                            showToast("Không tìm thấy học sinh nào");
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    showToast("Lỗi khi tải danh sách học sinh: " + errorMessage);
                });
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
            showToast("Không tìm thấy ID người dùng");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("student_id", student.getStudentId());
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        Log.d(TAG, "Chuyển đến StudentProfileFragment với student_id: " + student.getStudentId());
        navController.navigate(R.id.action_studentFragment_to_studentProfileFragment, bundle);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onDelete(Student student) {
        firebaseHelper.deleteStudent(student.getStudentId(), new OnOperationCompleteCallback() {
            @Override
            public void onSuccess() {
                mainHandler.post(() -> {
                    if (getActivity() != null) {
                        studentList.remove(student);
                        originalStudentList.remove(student);
                        studentAdapter.notifyDataSetChanged();
                        binding.studentCount.setText(String.valueOf(studentList.size()));
                        showToast("Học sinh đã được xóa thành công");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                mainHandler.post(() -> {
                    Log.e(TAG, "Lỗi khi xóa học sinh: " + e.getMessage(), e);
                    showToast("Lỗi khi xóa học sinh");
                });
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
            showToast("Không tìm thấy ID người dùng");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(actionId, bundle);
        Log.d(TAG, "Chuyển đến actionId: " + actionId + " với userId: " + userId);
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        imageAdapter = null;
    }

    private static class ImageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private final List<Uri> imageUris;
        private Uri selectedAvatarUri;
        private final OnImageClickListener listener;

        interface OnImageClickListener {
            void onImageClick(Uri uri);
        }

        ImageAdapter(List<Uri> imageUris, Uri selectedAvatarUri, OnImageClickListener listener) {
            this.imageUris = imageUris;
            this.selectedAvatarUri = selectedAvatarUri;
            this.listener = listener;
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            Uri uri = imageUris.get(position);
            holder.imageView.setImageURI(uri);
            holder.itemView.setBackgroundResource(
                    uri.equals(selectedAvatarUri) ? R.drawable.image_selected_background : 0
            );
            holder.itemView.setOnClickListener(v -> listener.onImageClick(uri));
        }

        @Override
        public int getItemCount() {
            return imageUris.size();
        }

        static class ImageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            androidx.appcompat.widget.AppCompatImageView imageView;

            ImageViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_view);
            }
        }
    }
}
