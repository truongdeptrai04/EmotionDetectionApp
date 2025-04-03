package com.example.smartclassemotion.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.smartclassemotion.R;
import com.example.smartclassemotion.database.FirebaseHelper;
import com.example.smartclassemotion.databinding.FragmentSignupBinding;

public class SignupFragment extends Fragment {
    private FragmentSignupBinding   binding;
    private FirebaseHelper firebaseHelper;

    public SignupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSignupBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        firebaseHelper = new FirebaseHelper();


        binding.loginBtn1.setOnClickListener(view -> navigateToLoginFragment());
        binding.loginBtn2.setOnClickListener(view -> navigateToLoginFragment());
        binding.signupBtn.setOnClickListener(view -> validateAndSignup());
        return root;
    }
    private void navigateToLoginFragment(){
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_signupFragment_to_loginFragment);
    }

    private void validateAndSignup(){
        String username = binding.usernameEdt.getText().toString().trim();
        String email = binding.emailEdt.getText().toString().trim();
        String password = binding.passwordEdt.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEdt.getText().toString().trim();

        if(username.isEmpty()){
            binding.usernameEdt.setError("Username is required");
            return;
        }
        if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.emailEdt.setError("Valid email is required");
        }
        if(password.isEmpty()){
            binding.passwordEdt.setError("Password is required");
            return;
        }
        if(password.length() < 6){
            binding.passwordEdt.setError("Password must be as least 6 characters");
            return;
        }
        if(confirmPassword.isEmpty()){
            binding.confirmPasswordEdt.setError("Confirm Password is required");
            return;
        }
        if (!password.equals(confirmPassword)){
            binding.confirmPasswordEdt.setError("Password do not match");
            return;
        }
        firebaseHelper.signUp(username, email, password, (success, userId) -> {
            if(success){
                Toast.makeText(getContext(), "Sign up successful", Toast.LENGTH_SHORT).show();
                navigateToLoginFragment();
            }else{
                Toast.makeText(getContext(), "Sign up failed. Email may already exist.",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        binding = null;
    }
}