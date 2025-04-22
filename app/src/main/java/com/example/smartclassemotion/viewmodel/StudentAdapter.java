package com.example.smartclassemotion.viewmodel;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.databinding.ItemStudentBinding;
import com.example.smartclassemotion.models.Student;
import com.example.smartclassemotion.utils.OnStudentActionListener;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private final List<Student> studentList;
    private final OnStudentActionListener listener;

    public StudentAdapter(List<Student> studentList, OnStudentActionListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentBinding binding = ItemStudentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Student> newList) {
        studentList.clear();
        studentList.addAll(newList);
        notifyDataSetChanged();
    }

    // ViewHolder với binding
    public class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentBinding binding;

        public StudentViewHolder(ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Student student) {
            binding.studentName.setText(student.getStudentName());
            binding.studentStatus.setText(student.getStatus());
            binding.studentStatus.setTextColor(student.getStatus().equals("Active") ?
                    0xFF4CAF50 : 0xFFFF0000); // Xanh cho Active, đỏ cho Inactive

            binding.editStudentBtn.setOnClickListener(v -> listener.onEdit(student));
            binding.deleteStudentBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(binding.getRoot().getContext())
                        .setTitle("Confirm Delete")
                        .setMessage("Are you sure you want to delete " + student.getStudentName() + "?")
                        .setIcon(R.drawable.ic_dialog_alert)
                        .setPositiveButton("Yes", (dialog, which) -> {
                            listener.onDelete(student);
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .setCancelable(true)
                        .show();
            });
        }
    }
}