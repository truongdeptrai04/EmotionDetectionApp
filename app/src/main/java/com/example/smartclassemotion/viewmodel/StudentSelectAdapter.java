package com.example.smartclassemotion.viewmodel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.models.Student;

import java.util.List;

public class StudentSelectAdapter extends RecyclerView.Adapter<StudentSelectAdapter.StudentViewHolder> {
    private final List<Student> studentList;
    private OnStudentClickListener clickListener;

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
    }

    public StudentSelectAdapter(List<Student> studentList, OnStudentClickListener clickListener) {
        this.studentList = studentList;
        this.clickListener = clickListener;
    }

    // Phương thức để cập nhật listener
    public void setOnStudentClickListener(OnStudentClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_select, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student);
    }
    @Override
    public int getItemCount() {
        return studentList.size();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView studentName;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.student_name);
        }

        public void bind(Student student) {
            studentName.setText(student.getStudentName());
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onStudentClick(student);
                }
            });
        }
    }
}