package com.example.smartclassemotion.viewmodel;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartclassemotion.databinding.AlertItemBinding;
import com.example.smartclassemotion.models.Alert;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {
    private final List<Alert> alertList = new ArrayList<>();
    private final Context context;
    private final Vibrator vibrator;

    public AlertAdapter(Context context) {
        this.context = context;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AlertItemBinding binding = AlertItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AlertViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        holder.bind(alertList.get(position));
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public void updateAlerts(List<Alert> newAlerts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AlertDiffCallback(alertList, newAlerts));
        alertList.clear();
        alertList.addAll(newAlerts);
        diffResult.dispatchUpdatesTo(this);
    }

    private void deleteAlert(Alert alert) {
        FirebaseFirestore.getInstance().collection("Alerts")
                .document(alert.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Không cần cập nhật thủ công, snapshot listener sẽ xử lý
                    Toast.makeText(context, "Thông báo đã được xóa", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        context,
                        "Lỗi khi xóa thông báo: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }

    class AlertViewHolder extends RecyclerView.ViewHolder {
        private final AlertItemBinding binding;

        AlertViewHolder(AlertItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Alert alert) {
            binding.alertContent.setText(String.format("%s: %s", alert.getTitle(), alert.getContent()));
            binding.alertTime.setText(getRelativeTime(alert.getTimestampInMillis()));

            int backgroundColor;
            switch (alert.getTitle()) {
                case "Critical":
                    backgroundColor = 0xFFFFF5F5; // #FFF5F5
                    break;
                case "Warning":
                case "Alert":
                    backgroundColor = 0xFFFFF9E6; // #FFF9E6
                    break;
                default:
                    backgroundColor = 0xFFFFFFFF; // Trắng
            }
            binding.cardView.setCardBackgroundColor(backgroundColor);

            binding.deleteBtn.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }

                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa thông báo này?")
                        .setPositiveButton("Có", (dialog, which) -> deleteAlert(alert))
                        .setNegativeButton("Không", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            });
        }

        private String getRelativeTime(long timestamp) {
            return DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString();
        }
    }

    private static class AlertDiffCallback extends DiffUtil.Callback {
        private final List<Alert> oldList;
        private final List<Alert> newList;

        AlertDiffCallback(List<Alert> oldList, List<Alert> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Alert oldAlert = oldList.get(oldItemPosition);
            Alert newAlert = newList.get(newItemPosition);
            return oldAlert.getTitle().equals(newAlert.getTitle()) &&
                    oldAlert.getContent().equals(newAlert.getContent()) &&
                    oldAlert.getTimestamp().equals(newAlert.getTimestamp()) &&
                    oldAlert.getClassId().equals(newAlert.getClassId());
        }
    }
}