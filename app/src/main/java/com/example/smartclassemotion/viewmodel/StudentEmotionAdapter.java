package com.example.smartclassemotion.viewmodel;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartclassemotion.databinding.ItemStudentEmotionBinding;
import com.example.smartclassemotion.models.Student;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentEmotionAdapter extends RecyclerView.Adapter<StudentEmotionAdapter.StudentEmotionViewHolder> {
    private static final String TAG = "StudentEmotionAdapter";
    private final List<Student> studentList;
    private final List<Map<String, Float>> emotionStatsList;

    public StudentEmotionAdapter(List<Student> studentList, List<Map<String, Float>> emotionStatsList) {
        this.studentList = studentList;
        this.emotionStatsList = emotionStatsList;
    }

    @NonNull
    @Override
    public StudentEmotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentEmotionBinding binding = ItemStudentEmotionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentEmotionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentEmotionViewHolder holder, int position) {
        Student student = studentList.get(position);
        Map<String, Float> emotionStats = position < emotionStatsList.size() ? emotionStatsList.get(position) : null;
        if (emotionStats == null) {
            Log.w(TAG, "Không có thống kê cảm xúc cho học sinh tại vị trí: " + position + ", tên: " + student.getStudentName());
            emotionStats = new HashMap<>();
            emotionStats.put("happy", 0f);
            emotionStats.put("sad", 0f);
            emotionStats.put("angry", 0f);
            emotionStats.put("neutral", 0f);
            emotionStats.put("fear", 0f);
            emotionStats.put("surprise", 0f);
        }
        holder.bind(student, emotionStats);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Student> newList, List<Map<String, Float>> newEmotionStatsList) {
        Log.d(TAG, "Cập nhật dữ liệu: studentList size = " + newList.size() + ", emotionStatsList size = " + newEmotionStatsList.size());
        this.studentList.clear();
        this.studentList.addAll(newList);
        this.emotionStatsList.clear();
        this.emotionStatsList.addAll(newEmotionStatsList);
        notifyDataSetChanged();
    }

    public static class StudentEmotionViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentEmotionBinding binding;

        public StudentEmotionViewHolder(ItemStudentEmotionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(Student student, Map<String, Float> emotionStats) {
            binding.studentName.setText(student.getStudentName());

            int happyValue = Math.round(emotionStats.getOrDefault("happy", 0f));
            binding.happyProgress.setProgress(happyValue);
            binding.happyPercent.setText(happyValue + "%");

            int sadValue = Math.round(emotionStats.getOrDefault("sad", 0f));
            binding.sadProgress.setProgress(sadValue);
            binding.sadPercent.setText(sadValue + "%");

            int angryValue = Math.round(emotionStats.getOrDefault("angry", 0f));
            binding.angryProgress.setProgress(angryValue);
            binding.angryPercent.setText(angryValue + "%");

            int neutralValue = Math.round(emotionStats.getOrDefault("neutral", 0f));
            binding.neutralProgress.setProgress(neutralValue);
            binding.neutralPercent.setText(neutralValue + "%");

            int fearValue = Math.round(emotionStats.getOrDefault("fear", 0f));
            binding.fearProgress.setProgress(fearValue);
            binding.fearPercent.setText(fearValue + "%");

            int surpriseValue = Math.round(emotionStats.getOrDefault("surprise", 0f));
            binding.surpriseProgress.setProgress(surpriseValue);
            binding.surprisePercent.setText(surpriseValue + "%");
        }
    }
}
