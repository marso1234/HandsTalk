package com.handstalk.signdetect.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.databinding.ActivitySignUpBinding;
import com.handstalk.signdetect.firebase.Authentication;
import com.handstalk.signdetect.utilities.Constants;
import com.handstalk.signdetect.utilities.MyLogging;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity implements MyLogging {

    private ActivitySignUpBinding binding;
    private final String TAG = "Sign Up";
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDatabase = FirebaseFirestore.getInstance();
    private String username, email, password, confirmPassword;

    private GoogleSignInClient mGoogleSignInClient;
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result.getResultCode() == RESULT_OK){
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                    mAuth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){

                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    email = user.getEmail();
                                    username = user.getEmail();
                                    Authentication.createAccount(username,email,SignUpActivity.this);
                                    finish();
                                }
                                Logging("Sign Up successful", "Account created, please sign in");

                            }else {
                                Toast.makeText(SignUpActivity.this, "Sign Up Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch(ApiException e){
                    e.printStackTrace();
                }
            }
        }
    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.imageBack.setOnClickListener(v-> finish());
        binding.buttonSignUp.setOnClickListener(signUpListener);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();

        //Google Button
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(SignUpActivity.this, options);

        Button googleSignInButton = findViewById(R.id.signInGoogle);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = mGoogleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }
        });
    }

    @Override
    public void Logging(String debugMsg, String userMsg){
        Log.d(TAG, debugMsg);

        //Skip User Message If Empty
        if(userMsg.isEmpty()) return;
        Toast.makeText(getApplicationContext(), userMsg, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener signUpListener = view -> {
        // Input Null Check
        if (!inputNullCheck()){
            Logging("Data empty", "Please fill in all the information");
            return;
        }else{
            fillValues();
        }
        if (!emailCheck()){
            Logging("Email Validation failed", "Please Enter Email in correct format");
            return;
        }
        if (!usernameCheck()){
            Logging("Username Invalid", "User must be within "+Constants.usernameMinLength+" to "+Constants.usernameMaxLength+" characters");
            return;
        }
        if (!passwordCheck()){
            Logging("Password Length Invalid", "Password must be within "+Constants.passwordMinLength+" to "+Constants.passwordMaxLength+" characters");
            return;
        }
        if (!passwordConfirmCheck()){
            Logging("Password Validation failed", "Confirm Password Failed");
            return;
        }
        signUp();
    };

    private boolean inputNullCheck(){
        if (binding == null) return false;
        if (binding.inputUsername.getText().toString().trim().isEmpty()) return false;
        if (binding.inputEmail.getText().toString().trim().isEmpty()) return false;
        if (binding.inputPassword.getText().toString().trim().isEmpty()) return false;
        if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) return false;
        return true;
    }

    private void fillValues(){
        username = binding.inputUsername.getText().toString().trim();
        email = binding.inputEmail.getText().toString().trim();
        password = binding.inputPassword.getText().toString().trim();

        // Not Trimming Prevent Unexpected Sign Up
        confirmPassword = binding.inputConfirmPassword.getText().toString();
    }

    private boolean usernameCheck(){
        return username.length() >= Constants.usernameMinLength && username.length() <= Constants.usernameMaxLength;
    }

    private boolean passwordCheck(){
        return password.length()>= Constants.passwordMinLength && password.length()<= Constants.passwordMaxLength;
    }

    private boolean passwordConfirmCheck(){
        return password.equals(confirmPassword);
    }


    private boolean emailCheck() {
        String regexPattern = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b";
        return Pattern.compile(regexPattern).matcher(email).matches();
    }

    private void signUp(){

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(task -> {

                    Authentication.createAccount(username,email,this);
                    mAuth.signOut();
                    Logging("Sign Up successful", "Account created, please sign in");
                    finish();
            })
                .addOnFailureListener(task -> {
                    Logging(task.getMessage(), task.getMessage());
            });
    }


}