package com.handstalk.signdetect.firebase;

import com.handstalk.signdetect.utilities.Constants;
import com.handstalk.signdetect.utilities.MyLogging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;


public class Authentication {

    public static void createAccount(String username, String email, MyLogging mLog){
        OnCompleteListener addDatabaseListener = task -> {
            if(task.isSuccessful()){
                mLog.Logging("Update Database Successful", "");
            }else{
                mLog.Logging("Update Database Unsuccessful", "");
            }
        };

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore mDatabase = FirebaseFirestore.getInstance();

        String authId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        String userId = Constants.generateRandomId();
        // For path user (Private Data)
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_USERNAME, username);
        user.put(Constants.KEY_EMAIL, email);
        user.put(Constants.KEY_USER_ID, userId);

        // For path userId (Public Data)
        HashMap<String, Object> userIdCollection = new HashMap<>();
        userIdCollection.put(Constants.KEY_AUTH_ID, authId);
        user.put(Constants.KEY_USERNAME, username);
        user.put(Constants.KEY_EMAIL, email);
        // TODO: Update Here if new column of public data is added

        //Update /userId
        DocumentReference userIdPath = mDatabase.collection(Constants.KEY_COLLECTIONS_USER_ID).document(userId);
        userIdPath.set(userIdCollection).addOnCompleteListener(addDatabaseListener);

        // Update /user/{userId}
        DocumentReference userPath = mDatabase.collection(Constants.KEY_COLLECTIONS_USERS).document(authId);
        userPath.set(user).addOnCompleteListener(addDatabaseListener);
    }


}
