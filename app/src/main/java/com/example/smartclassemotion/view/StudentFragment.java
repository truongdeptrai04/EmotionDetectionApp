package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentStudentBinding;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.utils.OnStudentActionListener;
import com.example.smartclassemotion.viewmodel.StudentAdapter;

import java.util.ArrayList;
import java.util.List;

public class StudentFragment extends Fragment implements OnStudentActionListener {
    private FragmentStudentBinding binding;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private List<Student> studentList;
    private List<Student> originalStudentList;
    private StudentAdapter studentAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        userId = getArguments() != null ? getArguments().getString("user_id") : null;
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

        binding.addBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Add student clicked", Toast.LENGTH_SHORT).show();
        });

        setupMenuBar();

        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadStudentsByUserId(String userId) {
        firebaseHelper.getAllStudentsByUserId(userId, students -> {
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
        firebaseHelper.deleteStudent(student.getStudentId(), (success, id) -> {
            if (success && getActivity() != null) {
                studentList.remove(student);
                originalStudentList.remove(student);
                studentAdapter.notifyDataSetChanged();
                binding.studentCount.setText(String.valueOf(studentList.size()));
                Toast.makeText(getContext(), "Delete " + student.getStudentName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete " + student.getStudentName(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupMenuBar() {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(v -> {
            navController.navigate(R.id.action_studentFragment_to_homeFragment);
            Toast.makeText(getContext(), "Classes clicked", Toast.LENGTH_SHORT).show();
        });

        binding.menuReports.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Reports clicked", Toast.LENGTH_SHORT).show();
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