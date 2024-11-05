package com.handstalk.signdetect.firebase;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.handstalk.signdetect.activities.MainActivity;
import com.handstalk.signdetect.model.User;
import com.handstalk.signdetect.utilities.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FriendsClient {
    private static final String TAG = "Friend Client";
    private FirebaseFirestore mDatabase;
    private static FriendsClient friendsClient;
    private FirebaseAuth mAuth;
    private ArrayList<String> friendList;
    public static FriendsClient getInstance(){
        if(friendsClient==null){
            friendsClient = new FriendsClient();
            friendsClient.mDatabase = FirebaseFirestore.getInstance();
            friendsClient.mAuth = FirebaseAuth.getInstance();
            friendsClient.friendList = new ArrayList<>();
            friendsClient.fetchFriendList(user -> {});
        }
        return friendsClient;
    }

    public ArrayList<String> fetchFriendList(OnFriendLoad onFriendLoad){
        String id = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        if(id==null) {
            Log.d(TAG, "Auth id is null");
            return null;
        }
        friendList.clear();
        DocumentReference docRef = mDatabase.collection(Constants.KEY_COLLECTION_FRIENDS).document(id);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task != null && task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        HashMap data = (HashMap) document.getData();
                        friendList = (ArrayList<String>) data.get(Constants.KEY_FRIEND_IDS);
                        for (String friendId:
                             friendList) {
                            getFriendDetails(friendId, onFriendLoad);
                        }
                    }else{
                        Log.d(TAG, "No Such Document/ No Friends Added");
                    }
                }else{
                    Log.d(TAG, "Task Failed");
                }
            }
        });
        return friendList;
    }

    // Already Assume the Id is Valid
    private void getFriendDetails(String friendAuthId, OnFriendLoad onFriendLoad){
        if(Objects.equals(friendAuthId, "")) return;
        DocumentReference userProfileRef = mDatabase.collection(Constants.KEY_COLLECTIONS_USERS).document(friendAuthId);
        userProfileRef.get().addOnCompleteListener(task1 -> {
            DocumentSnapshot document = task1.getResult();
            if(document.exists()){
                User user = document.toObject(User.class);
                onFriendLoad.onFriendLoaded(user);
            }
        });
    }

    private boolean checkIdValidity(String friendId, onFriendValid onFriendValid){
        DocumentReference docRef = mDatabase.collection(Constants.KEY_COLLECTIONS_USER_ID).document(friendId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task != null && task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        onFriendValid.onFriendIdValid(document.getData());
                    }else{
                        Log.d(TAG, "No Such Document/ No Friends Added");
                    }
                }else{
                    Log.d(TAG, "Task Failed");
                }
            }
        });
        return true;
    }


    public void addFriendRaw(String friendAuthId, OnFriendAdd onFriendAdd){
        Log.d("Friend", "Add Friend");
        if (friendList.contains(friendAuthId)){
            return; // Should not be able to add same person multiple times
        }
        Log.d("Friend", "Add Friend");

        String selfAuthId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        if(selfAuthId==null) {
            Log.d(TAG, "Auth id is null");
            return;
        }
        if (friendAuthId == selfAuthId){
            Toast.makeText(MainActivity.getContext(), "You cannot add yourself as friend", Toast.LENGTH_SHORT).show();
            return;
        }

        friendList.add(friendAuthId);
        DocumentReference docRef = mDatabase.collection(Constants.KEY_COLLECTION_FRIENDS).document(selfAuthId);
        HashMap<String, Object> document = new HashMap<>();
        document.put(Constants.KEY_FRIEND_IDS, friendList);
        docRef.set(document).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                onFriendAdd.onFriendAdded();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Add Friend Fail",e.getMessage());
            }
        });
    }

    public void addFriend(String friendId, OnFriendAdd onFriendAdd){
        checkIdValidity(friendId, new onFriendValid() {
            @Override
            public void onFriendIdValid(Map snapshot) {
                String friendAuthId = (String) snapshot.get(Constants.KEY_AUTH_ID);
                addFriendRaw(friendAuthId, onFriendAdd);
            }
        });
    }

    public void removeFriend(String friendId){
        if(!friendList.contains(friendId)) return; // Not in Array List
        friendList.remove(friendId);
        String id = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        if(id==null) {
            Log.d(TAG, "Auth id is null");
            return;
        }
        DocumentReference docRef = mDatabase.collection(Constants.KEY_COLLECTION_FRIENDS).document(id);
        HashMap<String, Object> document = new HashMap<>();
        document.put(Constants.KEY_FRIEND_IDS, friendList);
        docRef.set(document).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Successfully Add Friend");
            }
        });
    }

    private interface onFriendValid {
        void onFriendIdValid(Map snapshot);
    }

    public interface OnFriendLoad {
        void onFriendLoaded(User user);
    }

    public interface OnFriendAdd {
        void onFriendAdded();
    }
}
