package com.example.smartclassemotion.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.smartclassemotion.R;

public class SettingFragment extends Fragment {

    private TextView tvPreferences, tvLanguage, tvReportUpdate;
    private Spinner spLanguage, spFrequency;
    private SwitchCompat switchPushNotification, switchBackgroundSync, switchDarkMode;
    private TextView menuClasses, menuReports, menuStudent, menuSettings;

    public SettingFragment() {
        // Required empty public constructor
    }

    public static SettingFragment newInstance(String param1, String param2) {
        SettingFragment fragment = new SettingFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // Ánh xạ các view từ XML
        tvPreferences = view.findViewById(R.id.tv_preferences);
        tvLanguage = view.findViewById(R.id.tv_language);
        tvReportUpdate = view.findViewById(R.id.tv_reportupdate);
        spLanguage = view.findViewById(R.id.sp_language);
        spFrequency = view.findViewById(R.id.sp_frequency);

        switchPushNotification = view.findViewById(R.id.switch_custom);
        switchBackgroundSync = view.findViewById(R.id.switch_background_sync);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        menuClasses = view.findViewById(R.id.menu_classes);
        menuReports = view.findViewById(R.id.menu_reports);
        menuStudent = view.findViewById(R.id.menu_student);
        menuSettings = view.findViewById(R.id.menu_settings);

        // Gắn listener đơn giản (nếu cần)
        switchPushNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Xử lý bật/tắt thông báo
        });

        switchBackgroundSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Xử lý bật/tắt đồng bộ
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Xử lý dark mode
        });

        // Navigation click handlers
        menuClasses.setOnClickListener(v -> {
            // Xử lý chuyển đến Classes
        });

        menuReports.setOnClickListener(v -> {
            // Xử lý chuyển đến Reports
        });

        menuStudent.setOnClickListener(v -> {
            // Xử lý chuyển đến Student
        });

        menuSettings.setOnClickListener(v -> {
            // Có thể reload lại hoặc để trống
        });

        return view;
    }
}
