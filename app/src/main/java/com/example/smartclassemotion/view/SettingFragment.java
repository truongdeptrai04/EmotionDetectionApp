package com.example.smartclassemotion.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavHostController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.databinding.FragmentSettingBinding;

public class SettingFragment extends Fragment {
    private static final String TAG = "SettingFragment";
    private FragmentSettingBinding binding;
    private SharedPreferences prefs;
    private String userId;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        if (getArguments() != null) {
            userId = getArguments().getString("user_id");
            Log.d(TAG, "Received userId: " + userId);
        }
        prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        setupSpinner();
        setupSwitch();
        setupMenuBar();
        return root;
    }
    private void setupSwitch(){
        boolean pushNotificationEnabled = prefs.getBoolean("push_notification", true);
        binding.switchPushNotification.setChecked(pushNotificationEnabled);
        binding.switchPushNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("push_notification", isChecked).apply();
            Log.d(TAG, "Push Notification: " + (isChecked ? "Enabled" : "Disabled"));
            Toast.makeText(getContext(), "Push Notification " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        });

        boolean backgroundDataEnabled = prefs.getBoolean("background_data_sync", true);
        binding.switchBackgroundData.setChecked(backgroundDataEnabled);
        binding.switchBackgroundData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("background_data_sync", isChecked).apply();
            Log.d(TAG, "Background Data Sync: " + (isChecked ? "Enabled" : "Disabled"));
            Toast.makeText(getContext(), "Background Data Sync " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        });

        boolean darkModeEnabled = prefs.getBoolean("dark_mode", false);
        binding.switchDarkMode.setChecked(darkModeEnabled);
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            Log.d(TAG, "Dark Mode: " + (isChecked ? "Enabled" : "Disabled"));
            Toast.makeText(getContext(), "Dark Mode " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        });
    }
    private void setupSpinner(){
        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.language_options, android.R.layout.simple_spinner_item);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLanguage.setAdapter(languageAdapter);

        String savedLanguage = prefs.getString("language", "English");
        int languagePosition = languageAdapter.getPosition(savedLanguage);
        binding.spinnerLanguage.setSelection(languagePosition);

        binding.spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedLanguage = adapterView.getItemAtPosition(i).toString();
                prefs.edit().putString("language", selectedLanguage).apply();
                Log.d(TAG, "Selected Language: " + selectedLanguage);
                Toast.makeText(getContext(), "Language changed to " + selectedLanguage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    private void setupMenuBar(){
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(view -> {
            NavigateToHomeFragment(navController);
        });
        binding.menuSettings.setOnClickListener(view -> {

        });
        binding.menuReports.setOnClickListener(view -> {
            NavigateToReportFragment(navController);
        });
        binding.menuStudent.setOnClickListener(view -> {
            NavigateToStudentFragment(navController);
        });
    }
    private void NavigateToHomeFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_settingFragment_to_homeFragment, bundle);
            Log.d(TAG, "Navigating to HomeFragment with userId: " + userId);
        }
    }
    private void NavigateToStudentFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_settingFragment_to_studentFragment, bundle);
            Log.d(TAG, "Navigating to StudentFragment with userId: " + userId);
        }
    }
    private void NavigateToReportFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_settingFragment_to_alertFragment, bundle);
            Log.d(TAG, "Navigating to ReportFragment with userId: " + userId);
        }
    }
}