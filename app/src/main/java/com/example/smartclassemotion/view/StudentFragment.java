package com.example.smartclassemotion.view;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentStudentBinding;
import com.example.smartclassemotion.databinding.FragmentStudentBinding;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.utils.OnStudentActionListener;
import com.example.smartclassemotion.viewmodel.StudentAdapter;

import java.util.ArrayList;
import java.util.List;

public class StudentFragment extends Fragment implements OnStudentActionListener {
    private FragmentStudentBinding binding;
    private FirebaseHelper firebaseHelper;
    private String classId;
    private List<Student> studentList;
    private StudentAdapter studentAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState){
        binding = FragmentStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper();
        classId = getArguments() != null ? getArguments().getString("class_id"):null;
        studentList = new ArrayList<>();

        binding.studentRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentAdapter = new StudentAdapter(studentList, this);
        binding.studentRecycleView.setAdapter(studentAdapter);

        if(classId != null){
            loadStudent(classId);
        }

        binding.studentSearch.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener(){
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
        binding.addBtn.setOnClickListener(v->{
            Toast.makeText(getContext(), "Add student clicked", Toast.LENGTH_SHORT).show();
        });

        return root;
    }
    @SuppressLint("NotifyDataSetChanged")
    private void loadStudent(String classId){
        firebaseHelper.getStudents(classId, students ->{
            if(getActivity() != null){
                studentList.clear();
                studentList.addAll(students);
                studentAdapter.notifyDataSetChanged();
            }
        });
    }
    private void filterStudent(String query){
        List<Student> filteredList = new ArrayList<>();
        for(Student student: studentList){
            if(student.getStudentName().toLowerCase().contains(query.toLowerCase()) || student.getStudentCode().toLowerCase().contains(query.toLowerCase())){
                filteredList.add(student);
            }
        }
        studentAdapter.updateList(filteredList);
    }
    @Override
    public void onEdit(Student student){
        Toast.makeText(getContext(), "Edit" + student.getStudentName(), Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onDelete(Student student){
        firebaseHelper.deleteStudent(student.getStudentId(), (success, id) -> {
            if(success && getActivity() != null){
                studentList.remove(student);
                studentAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Delete " + student.getStudentName(), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getContext(), "Failed to delete " + student.getStudentName(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onDestroyView(){
        super.onDestroyView();
        binding = null;
    }
}