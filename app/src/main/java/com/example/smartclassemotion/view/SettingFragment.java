package com.example.smartclassemotion.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentSettingBinding;

public class SettingFragment extends Fragment {
    private static final String TAG = "SettingFragment";
    private FragmentSettingBinding binding;
    private SharedPreferences prefs;
    private String userId;
    private FirebaseHelper firebaseHelper;

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

        firebaseHelper = new FirebaseHelper(requireContext());
        if (getArguments() != null) {
            userId = getArguments().getString("user_id");
            Log.d(TAG, "Received userId: " + userId);
        }
        prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        setupSpinners();
        setupSwitches();
        setupMenuBar();
        setupSignOutButton();
        return root;
    }

    private void setupSpinners() {
        // Language Spinner
        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.language_options, android.R.layout.simple_spinner_item);
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
                Toast.makeText(requireContext(), "Language changed to " + selectedLanguage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Không xử lý
            }
        });

        // Report Update Frequency Spinner
        ArrayAdapter<CharSequence> reportUpdateAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.report_update_options, android.R.layout.simple_spinner_item);
        reportUpdateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerReportUpdate.setAdapter(reportUpdateAdapter);

        String savedReportUpdate = prefs.getString("report_update_frequency", "Daily");
        int reportUpdatePosition = reportUpdateAdapter.getPosition(savedReportUpdate);
        binding.spinnerReportUpdate.setSelection(reportUpdatePosition);

        binding.spinnerReportUpdate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedFrequency = adapterView.getItemAtPosition(i).toString();
                prefs.edit().putString("report_update_frequency", selectedFrequency).apply();
                Log.d(TAG, "Selected Report Update Frequency: " + selectedFrequency);
                Toast.makeText(requireContext(), "Report Update Frequency changed to " + selectedFrequency, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Không xử lý
            }
        });
    }

    private void setupSwitches() {
        boolean pushNotificationEnabled = prefs.getBoolean("push_notification", true);
        binding.switchPushNotification.setChecked(pushNotificationEnabled);
        binding.switchPushNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("push_notification", isChecked).apply();
            Log.d(TAG, "Push Notification: " + (isChecked ? "Enabled" : "Disabled"));
            Toast.makeText(requireContext(), "Push Notification " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        });

        boolean backgroundDataEnabled = prefs.getBoolean("background_data_sync", true);
        binding.switchBackgroundData.setChecked(backgroundDataEnabled);
        binding.switchBackgroundData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("background_data_sync", isChecked).apply();
            Log.d(TAG, "Background Data Sync: " + (isChecked ? "Enabled" : "Disabled"));
            Toast.makeText(requireContext(), "Background Data Sync " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        });

        boolean darkModeEnabled = prefs.getBoolean("dark_mode", false);
        binding.switchDarkMode.setChecked(darkModeEnabled);
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            Log.d(TAG, "Dark Mode: " + (isChecked ? "Enabled" : "Disabled"));
            Toast.makeText(requireContext(), "Dark Mode " + (isChecked ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSignOutButton() {
        binding.btnSignOut.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        firebaseHelper.signOut();
                        userId = null; // Xóa userId
                        Log.d(TAG, "User signed out successfully");
                        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();

                        // Điều hướng đến LoginFragment
                        NavController navController = NavHostFragment.findNavController(this);
                        navController.navigate(R.id.action_settingFragment_to_loginFragment);
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void setupMenuBar() {
        NavController navController = NavHostFragment.findNavController(this);

        binding.menuClasses.setOnClickListener(view -> navigateToHomeFragment(navController));
        binding.menuSettings.setOnClickListener(view -> {
            // Không làm gì vì đã ở SettingFragment
            Log.d(TAG, "Already in SettingsFragment");
        });
        binding.menuReports.setOnClickListener(view -> navigateToReportFragment(navController));
        binding.menuStudent.setOnClickListener(view -> navigateToStudentFragment(navController));
    }

    private void navigateToHomeFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_settingFragment_to_homeFragment, bundle);
            Log.d(TAG, "Navigating to HomeFragment with userId: " + userId);
        }
    }

    private void navigateToStudentFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_settingFragment_to_studentFragment, bundle);
            Log.d(TAG, "Navigating to StudentFragment with userId: " + userId);
        }
    }

    private void navigateToReportFragment(NavController navController) {
        if (userId == null) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("user_id", userId);
            navController.navigate(R.id.action_settingFragment_to_alertFragment, bundle);
            Log.d(TAG, "Navigating to ReportFragment with userId: " + userId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}