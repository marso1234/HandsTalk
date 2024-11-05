package com.handstalk.signdetect.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

//Nothing is bind to MainActivity, MainActivity is just the root route to other activities
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private static Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        swapPage();
    }

    //If user not signIn -> Direct to signIn Page
    //Else -> Home Page
    void swapPage() {
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getUid() != null){
            //Home Page
            Intent nav = new Intent(getApplicationContext(), NavigationFrameworkActivity.class);
            navLauncher.launch(nav);
        }else{
            //Sign In Page
            Intent login = new Intent(getApplicationContext(), SignInActivity.class);
            loginLauncher.launch(login);
        }
    }

    // Callback when other intent finished
    ActivityResultLauncher<Intent> navLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    finish();
                }
                if (result.getResultCode() == Activity.RESULT_FIRST_USER) {
                    swapPage();
                }
            }
    );

    // Callback when other intent finished
    ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    swapPage();
                }
                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    finish();
                }
            }
    );

    public static Context getContext(){
        return context;
    }


}