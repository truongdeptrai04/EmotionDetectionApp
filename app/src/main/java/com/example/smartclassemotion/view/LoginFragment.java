package com.example.smartclassemotion.view;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private FirebaseHelper firebaseHelper;
    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseHelper = new FirebaseHelper(getContext());

        binding.signupBtn1.setOnClickListener(view -> {
            navigateToSignupFragment();
        });
        binding.signupBtn2.setOnClickListener(view -> {
            navigateToSignupFragment();
        });
        binding.loginBtn.setOnClickListener(view -> {
            validateAndLogin();
        });
        return root;
    }

    private void navigateToSignupFragment(){
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_loginFragment_to_signupFragment);
    }

    private void validateAndLogin(){
        String email = binding.emailEdt.getText().toString().trim();
        String password = binding.passwordEdt.getText().toString().trim();

        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.emailEdt.setError("Valid email is required");
            return;
        }
        if(password.isEmpty()){
            binding.passwordEdt.setError("Password is required");
            return;
        }

        if(password.length()<6){
            binding.passwordEdt.setError("Password must be at least 6 characters");
            return;
        }

        firebaseHelper.login(email, password, ((success, userId) -> {
            if (success){
                Toast.makeText(getContext(), "Login successful", Toast.LENGTH_SHORT).show();
                Bundle bundle = new Bundle();
                bundle.putString("user_id", userId);
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.action_loginFragment_to_homeFragment, bundle);
            }else {
                Toast.makeText(getContext(), "Login failed. Check email or password", Toast.LENGTH_SHORT).show();
            }
        }));

    }
    @Override
    public void onDestroyView(){
        super.onDestroyView();
        binding = null;
    }
}