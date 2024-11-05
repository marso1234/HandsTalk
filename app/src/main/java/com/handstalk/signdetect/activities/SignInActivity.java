package com.handstalk.signdetect.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.databinding.ActivitySignInBinding;
import com.handstalk.signdetect.firebase.Authentication;
import com.handstalk.signdetect.utilities.Constants;
import com.handstalk.signdetect.utilities.MyLogging;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignInActivity extends AppCompatActivity implements MyLogging {

    private ActivitySignInBinding binding;
    private final String TAG = "Sign In";
    private SharedPreferences sharePrefFirebase;
    private String email, password;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDatabase;

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
                                Logging("Login Successfully", "Login Successfully");
                                Intent resultIntent = new Intent();
                                setResult(Activity.RESULT_OK, resultIntent);


                                String authId = mAuth.getCurrentUser().getUid();
                                DocumentReference docRef = mDatabase.collection(Constants.KEY_COLLECTIONS_USERS).document(authId);
                                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task != null && task.isSuccessful()){
                                            DocumentSnapshot document = task.getResult();
                                            if(document.exists()){
                                                HashMap data = (HashMap) document.getData();
                                                sharePrefFirebase.edit().putString(Constants.KEY_USERNAME,(String) data.get(Constants.KEY_USERNAME)).commit();
                                                sharePrefFirebase.edit().putString(Constants.KEY_AUTH_ID,authId).commit();
                                                sharePrefFirebase.edit().putString(Constants.KEY_USER_ID,(String) data.get(Constants.KEY_USER_ID)).commit();
                                            }else{
                                                Log.d(TAG, "No Such Document, creating Default...");
                                                Authentication.createAccount("username",email,SignInActivity.this::Logging);
                                            }
                                        }else{
                                            Log.d(TAG, "Task Failed");
                                        }
                                    }
                                });

                                finish();
                            }else {
                                Toast.makeText(SignInActivity.this, "Sign In Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
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

        sharePrefFirebase = getSharedPreferences(Constants.SHARED_PREF_FIREBASE_INFO,MODE_PRIVATE);
        mDatabase = FirebaseFirestore.getInstance();

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.textSignUp.setOnClickListener(invokeSignUpListener);
        binding.buttonSignIn.setOnClickListener(SignInListener);
        mAuth = FirebaseAuth.getInstance();

        //Google Button
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(SignInActivity.this, options);

        Button googleSignInButton = findViewById(R.id.signInGoogle);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = mGoogleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }
        });
    }

    public void Logging(String debugMsg, String userMsg){
        Log.d(TAG, debugMsg);

        //Skip User Message If Empty
        if(userMsg.isEmpty()) return;
        Toast.makeText(getApplicationContext(), userMsg, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener invokeSignUpListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
        }
    };

    private View.OnClickListener SignInListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Input Null Check
            if (!inputNullCheck()){
                Logging("Data empty", "Please fill in all the information");
                return;
            }
            fillValues();
            signIn();
        }
    };

    private boolean inputNullCheck(){
        if (binding == null) return false;
        if (binding.inputEmail.getText().toString().trim().isEmpty()) return false;
        if (binding.inputPassword.getText().toString().trim().isEmpty()) return false;
        return true;
    }

    private void fillValues(){
        email = binding.inputEmail.getText().toString().trim();
        password = binding.inputPassword.getText().toString().trim();
    }

    private void signIn(){
        binding.buttonSignIn.setVisibility(View.INVISIBLE);
        binding.signInProgressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
            Logging("Login Successfully", "Login Successfully");
            Intent resultIntent = new Intent();
            setResult(Activity.RESULT_OK, resultIntent);


            String authId = mAuth.getCurrentUser().getUid();
            DocumentReference docRef = mDatabase.collection(Constants.KEY_COLLECTIONS_USERS).document(authId);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task != null && task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()){
                            HashMap data = (HashMap) document.getData();
                            sharePrefFirebase.edit().putString(Constants.KEY_USERNAME,(String) data.get(Constants.KEY_USERNAME)).commit();
                            sharePrefFirebase.edit().putString(Constants.KEY_AUTH_ID,authId).commit();
                            sharePrefFirebase.edit().putString(Constants.KEY_USER_ID,(String) data.get(Constants.KEY_USER_ID)).commit();
                        }else{
                            Log.d(TAG, "No Such Document, creating Default...");
                            Authentication.createAccount("username",email,SignInActivity.this::Logging);
                        }
                    }else{
                        Log.d(TAG, "Task Failed");
                    }
                }
            });

            finish();
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Logging("Login Failed", "Invalid Email or Password");
                binding.buttonSignIn.setVisibility(View.VISIBLE);
                binding.signInProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onBackPressed(){
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, resultIntent);
        finish();
    }


    private final OnCompleteListener addDatabaseListener = task -> {
        if(task.isSuccessful()){
            Log.d(TAG, "Update Database Successful");
        }else{
            Logging("Update Database Unsuccessful", "Update Database Unsuccessful");
        }
    };
}