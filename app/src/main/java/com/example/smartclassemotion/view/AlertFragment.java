package com.example.smartclassemotion.view;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.databinding.FragmentAlertBinding;
import com.example.smartclassemotion.viewmodel.AlertAdapter;
import com.example.smartclassemotion.viewmodel.AlertViewModel;

public class AlertFragment extends Fragment {
    private FragmentAlertBinding binding;
    private String userId;
    private AlertAdapter alertAdapter;
    private AlertViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAlertBinding.inflate(inflater, container, false);

        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("user_id");
            Log.d(TAG, "Received args - userId: " + userId);
        } else {
            Log.w(TAG, "No arguments received");
            showToast("User ID not found");
        }

        alertAdapter = new AlertAdapter(requireContext());
        binding.recyclerViewAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewAlerts.setHasFixedSize(true);
        binding.recyclerViewAlerts.setAdapter(alertAdapter);

        viewModel = new ViewModelProvider(this).get(AlertViewModel.class);
        viewModel.getAlerts().observe(getViewLifecycleOwner(), alerts -> {
            if (alerts != null) {
                alertAdapter.updateAlerts(alerts);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadAlerts(userId); // Truyền userId vào loadAlerts
        setupMenuBar();
        return binding.getRoot();
    }

    private void setupMenuBar() {
        binding.menuClasses.setOnClickListener(v -> navigateToHomeFragment());
        binding.menuStudent.setOnClickListener(v -> navigateToStudentFragment());
        binding.menuSettings.setOnClickListener(v -> navigateToSettingFragment());
    }

    private void navigateToHomeFragment() {
        navigateTo(R.id.action_alertFragment_to_homeFragment);
    }

    private void navigateToStudentFragment() {
        navigateTo(R.id.action_alertFragment_to_studentFragment);
    }

    private void navigateToSettingFragment() {
        navigateTo(R.id.action_alertFragment_to_settingFragment);
    }

    private void navigateTo(int actionId) {
        if (userId == null) {
            showToast("User ID not found");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("user_id", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(actionId, bundle);
        Log.d(TAG, "Navigating to actionId: " + actionId + " with userId: " + userId);
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}