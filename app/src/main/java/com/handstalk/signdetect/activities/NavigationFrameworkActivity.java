package com.handstalk.signdetect.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.databinding.ActivityMainBinding;
import com.handstalk.signdetect.firebase.FriendsClient;
import com.handstalk.signdetect.ui.message.MessageFragment;
import com.handstalk.signdetect.utilities.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class NavigationFrameworkActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;
    private SharedPreferences sharePrefFirebase;
    private static final String TAG = "Navigation";
    private static NavigationFrameworkActivity navigationFrameworkActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharePrefFirebase = getSharedPreferences(Constants.SHARED_PREF_FIREBASE_INFO,MODE_PRIVATE);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navigationFrameworkActivity = this;

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();
        //Set FCM
        if(mAuth.getUid() != null){
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if(task.isSuccessful() && task.getResult()!=null){
                    Map<String,Object> update_values = new HashMap<>();
                    update_values.put(Constants.KEY_FCM_TOKEN, task.getResult());
                    mDB.collection(Constants.KEY_COLLECTIONS_USERS).document(mAuth.getUid()).update(update_values).addOnCompleteListener(updateFCMTask -> {
                        if(updateFCMTask.isSuccessful()){
                            Log.d(TAG, "Update FCM successful");
                            sharePrefFirebase.edit().putString(Constants.KEY_FCM_TOKEN, task.getResult()).commit();
                        }else{
                            Log.d(TAG, "Update FCM failed");
                        }
                    });

                }
            });
        }

        binding.addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Quick Exit if not initialize
                if(mAuth.getUid() == null) return;

                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(NavigationFrameworkActivity.this);

                EditText friendIdInput = new EditText(NavigationFrameworkActivity.this);
                friendIdInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                friendIdInput.setPadding(
                        NavigationFrameworkActivity.this.getResources().getDimensionPixelOffset(com.intuit.sdp.R.dimen._19sdp), // if you look at android alert_dialog.xml, you will see the message textview have margin 14dp and padding 5dp. This is the reason why I use 19 here
                        NavigationFrameworkActivity.this.getResources().getDimensionPixelOffset(com.intuit.sdp.R.dimen._12sdp),
                        NavigationFrameworkActivity.this.getResources().getDimensionPixelOffset(com.intuit.sdp.R.dimen._19sdp),
                        NavigationFrameworkActivity.this.getResources().getDimensionPixelOffset(com.intuit.sdp.R.dimen._12sdp)
                );
                friendIdInput.setHint("Friend Id");

                builder.setView(friendIdInput);
                builder.setTitle("Add Friend").setMessage("Enter Friend Id")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                                String friendId = friendIdInput.getText().toString();
                                if (friendId.isEmpty()) {
                                    Toast.makeText(NavigationFrameworkActivity.this, "Input cannot be empty", Toast.LENGTH_SHORT).show();
                                }
                                // Use userInput
                                FriendsClient friendsClient = FriendsClient.getInstance();
                                friendsClient.addFriend(friendId, () -> {
                                    Toast.makeText(NavigationFrameworkActivity.this, "Friend Added", Toast.LENGTH_SHORT).show();
                                    MessageFragment.friendList.clear();
                                    friendsClient.fetchFriendList((user)->{
                                        MessageFragment.friendList.add(user);

                                        MessageFragment.adapter.notifyDataSetChanged();
                                    });
                                });
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                // Show Dialog
                builder.create().show();
            }
        });
    }


    @Override
    public void onBackPressed(){
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, resultIntent);
        finish();
    }
}