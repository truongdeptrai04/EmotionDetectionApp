package com.example.smartclassemotion.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.smartclassemotion.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class StudentProfileFragment extends Fragment {

    // View variables
    private ImageView imvProfile;
    private TextView tvNameStudent, tvHappy, tvAttendance, tvParticipation;
    private TextView tvEmotionTrend, tvToday, tvThisWeek;
    private TextView tvTeacherNote, tvHistory;
    private EditText edtNote;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnSave;
    private TextView menuClasses, menuReports, menuStudent, menuSettings;

    public StudentProfileFragment() {
        // Required empty public constructor
    }

    public static StudentProfileFragment newInstance(String param1, String param2) {
        StudentProfileFragment fragment = new StudentProfileFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_profile, container, false);

        // Ánh xạ view từ XML
        imvProfile = view.findViewById(R.id.imv_propile);
        tvNameStudent = view.findViewById(R.id.tv_namestudent);
        tvHappy = view.findViewById(R.id.tv_happy);
        tvAttendance = view.findViewById(R.id.tv_attendance);
        tvParticipation = view.findViewById(R.id.tv_participation);
        tvEmotionTrend = view.findViewById(R.id.tv_emotiontrent);
        tvToday = view.findViewById(R.id.tv_today);
        tvThisWeek = view.findViewById(R.id.tv_thisweek);
        tvTeacherNote = view.findViewById(R.id.tv_teachernote);
        tvHistory = view.findViewById(R.id.tv_history);
        edtNote = view.findViewById(R.id.edt_note);
        btnSave = view.findViewById(R.id.btn_save);

        menuClasses = view.findViewById(R.id.menu_classes);
        menuReports = view.findViewById(R.id.menu_reports);
        menuStudent = view.findViewById(R.id.menu_student);
        menuSettings = view.findViewById(R.id.menu_settings);

        // Xử lý nút Save ghi chú
        btnSave.setOnClickListener(v -> {
            String note = edtNote.getText().toString().trim();
            if (!note.isEmpty()) {
                // TODO: Lưu ghi chú vào database hoặc gửi lên server
                // Ví dụ:
                // saveNoteToDatabase(note);

                edtNote.setText("");
            }
        });

        // TODO: Bạn có thể thêm sự kiện click cho các menu bên dưới:
        menuClasses.setOnClickListener(v -> {
            // Điều hướng đến màn hình Classes
        });

        menuReports.setOnClickListener(v -> {
            // Điều hướng đến màn hình Reports
        });

        menuStudent.setOnClickListener(v -> {
            // Đang ở màn hình Student
        });

        menuSettings.setOnClickListener(v -> {
            // Điều hướng đến Settings
        });

        return view;
    }
}
