package com.example.smartclassemotion.viewmodel;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.databinding.ClassItemBinding;
import com.example.smartclassemotion.models.ClassItem;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<ClassItem> classList;

    public ClassAdapter(List<ClassItem> classList) {
        this.classList = classList;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ClassItemBinding binding = ClassItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ClassViewHolder(binding);
    }
    @Override
    public void onBindViewHolder(ClassViewHolder holder, int position) {
        ClassItem classItem = classList.get(position);
        holder.bind(classItem);
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        private final ClassItemBinding binding;

        ClassViewHolder(ClassItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        public void bind(ClassItem classItem){
            binding.className.setText(classItem.getClassName());
            binding.studentCount.setText(String.valueOf(classItem.getStudentCount()));
            binding.classTime.setText(classItem.getClassTime());
            binding.emotionRating.setText(classItem.getEmotionRating());

            binding.getRoot().setOnClickListener(v ->{
                NavController navController = Navigation.findNavController(v);
                Bundle args = new Bundle();
                args.putString("class_id", classItem.getClassId());
                args.putString("class_name", classItem.getClassName());
                args.putInt("student_count", classItem.getStudentCount());
                android.util.Log.d("ClassAdapter", "Navigating with classId: " + classItem.getClassId() + ", className: " + classItem.getClassName() + ", studentCount: " + classItem.getStudentCount());
                navController.navigate(R.id.action_homeFragment_to_classDetailFragment, args);
            });
        }
    }
}