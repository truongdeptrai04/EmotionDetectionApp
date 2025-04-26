package com.example.smartclassemotion.viewmodel;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.ClassItemBinding;
import com.example.smartclassemotion.models.ClassItem;

import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {
    private String userId;
    private List<ClassItem> classList;
    private FirebaseHelper firebaseHelper;
    private final OnClassActionListener actionListener;

    public interface OnClassActionListener {
        void onEditClass(ClassItem classItem);
        void onDeleteClass(ClassItem classItem);
    }

    public ClassAdapter(List<ClassItem> classList, String userId, OnClassActionListener actionListener) {
        this.classList = classList;
        this.userId = userId;
        this.firebaseHelper = new FirebaseHelper();
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ClassItemBinding binding = ClassItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ClassViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ClassViewHolder holder, int position) {
        ClassItem aClass = classList.get(position);
        holder.bind(aClass);
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    class ClassViewHolder extends RecyclerView.ViewHolder {
        private final ClassItemBinding binding;

        public ClassViewHolder(ClassItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ClassItem classItem) {
            binding.className.setText(classItem.getClassName());

            firebaseHelper.getDb().collection("StudentClasses")
                    .whereEqualTo("classId", classItem.getClassId())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int studentCount = querySnapshot.size();
                        binding.studentCount.setText(String.valueOf(studentCount));
                    })
                    .addOnFailureListener(e -> {
                        binding.studentCount.setText("0");
                        android.util.Log.e("ClassAdapter", "Error getting student count: " + e.getMessage());
                    });

            binding.classTime.setText(classItem.getFormattedTime());
            binding.emotionRating.setText(classItem.getDescription() != null && !classItem.getDescription().isEmpty() ? classItem.getDescription() : "No description");

            binding.getRoot().setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(v);
                Bundle bundle = new Bundle();
                bundle.putString("user_id", userId);
                bundle.putString("class_id", classItem.getClassId());
                bundle.putString("class_name", classItem.getClassName());
                android.util.Log.d("ClassAdapter", "Class ID: " + classItem.getClassId() + ", ClassName: " + classItem.getClassName() + ", User ID: " + userId);
                navController.navigate(R.id.action_homeFragment_to_classDetailFragment, bundle);
            });

            binding.editBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEditClass(classItem);
                }
                android.util.Log.d("ClassAdapter", "Edit button clicked for classId: " + classItem.getClassId());
            });
            binding.deleteBtn.setOnClickListener(view -> {
                if(actionListener != null){
                    actionListener.onDeleteClass(classItem);
                }
                Log.d("ClassAdapter", "Delete button clicked for classId: " + classItem.getClassId());
            });
        }
    }
}