package com.handstalk.signdetect.activities;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.handstalk.signdetect.Adapter.ChatAdaper;
import com.handstalk.signdetect.databinding.ActivityChatDetailBinding;
import com.handstalk.signdetect.R;
import com.handstalk.signdetect.model.MessageModel;
import com.handstalk.signdetect.model.User;
import com.handstalk.signdetect.utilities.AndroidUtil;
import com.handstalk.signdetect.utilities.FirebaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.handstalk.signdetect.utilities.Constants;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ChatDetailActivity extends AppCompatActivity {

    ActivityChatDetailBinding binding;
    FirebaseFirestore db;
    FirebaseAuth auth;
    String chatroomId;
    String receiveId;
    ImageView imageView;
    User receiverUser;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        final ArrayList<MessageModel> messageModels = new ArrayList<>();
        final ChatAdaper chatAdapter = new ChatAdaper(messageModels, this);

        String senderId = auth.getUid();
        receiverUser = (User) getIntent().getSerializableExtra("User");
        receiveId = receiverUser.getUserId();
        String userName = receiverUser.getUsername();
        binding.userName.setText(userName);

        binding.icBackarrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.videoCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatDetailActivity.this, OutgoingInvitationActivity.class);
                intent.putExtra("User",receiverUser);
                intent.putExtra(Constants.REMOTE_MSG_MEETING_TYPE, "Video");
                startActivity(intent);
            }
        });

        binding.chatRecyclerView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        chatroomId = createChatroomId();
        imageView = findViewById(R.id.profile_image);

        // get receiver's auth_id -> check chatroom existence -> refresh UI
        getUserAuthId(receiveId, new UserAuthIdCallback() {
            @Override
            public void onUserAuthIdFound(String authId) {
                if (authId != null) {
                    //Found authId
                    Log.d("ChatDetailActivity", "AuthId found for receiveId " + receiveId + ": " + authId);
                    receiveId = authId;

                    //set receiver icon image
                    FirebaseUtil.getOtherProfilePicStorageRef(receiveId).getDownloadUrl()
                            .addOnCompleteListener(t -> {
                                if(t.isSuccessful()){
                                    Uri uri  = t.getResult();
                                    AndroidUtil.setProfilePic(ChatDetailActivity.this,uri,imageView);
                                }
                            });

                    checkChatroomExistence(senderId, receiveId, new ChatroomExistenceCallback() {
                        @Override
                        public void onChatroomExistenceChecked(String chatroomId) {
                            db.collection("chats")
                                    .document(chatroomId)
                                    .collection("messages")
                                    .addSnapshotListener((value, error) -> {
                                        if (value != null) {
                                            messageModels.clear();
                                            List<MessageModel> sortedMessages = new ArrayList<>();
                                            for (DocumentSnapshot document : value.getDocuments()) {
                                                MessageModel model = document.toObject(MessageModel.class);
                                                if (model != null) {
                                                    sortedMessages.add(model);
                                                }
                                            }
                                            // Sort messages by timestamp
                                            Collections.sort(sortedMessages, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                                            // Add sorted messages to messageModels
                                            messageModels.addAll(sortedMessages);
                                            chatAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    });
                } else {
                    // AuthId is null
                    Log.e("ChatDetailActivity", "AuthId not found for receiveId: " + receiveId);
                }
            }
        });

        //Set up menu
        ImageView menuButton = findViewById(R.id.chatroomMenu);
        storageReference = FirebaseStorage.getInstance().getReference();
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create PopupMenu
                PopupMenu popup = new PopupMenu(ChatDetailActivity.this, view);
                getMenuInflater().inflate(R.menu.chatroom_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.delete_all_records) {
                            // Delete all messages
                            deleteAllMessages(chatroomId);
                            return true;
                        } else if (itemId == R.id.modify_background) {
                            Log.d("modify_background: ", "modify_background: ");
                            choosePicture();
                        }
                        return false;
                    }
                });
                // Show the PopupMenu
                popup.show();
            }
        });

        binding.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = binding.enterMessage.getText().toString();
                if (!messageText.isEmpty()) {
                    sendMessage(chatroomId, senderId, receiveId, messageText);
                    binding.enterMessage.setText("");
                }
            }
        });
        loadBackground();
    }

    private String createChatroomId() {
        // Generate a random document ID provided by Firestore
        return db.collection("chats").document().getId();
    }

    private void sendMessage(String chatroomId, String senderId, String receiverId, String messageText) {
        MessageModel message = new MessageModel(senderId, messageText, new Date().getTime());
        db.collection("chats")
                .document(chatroomId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // Handle success
                    Log.d("ChatDetailedActivity", "Message sent successfully");
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Log.e("ChatDetailedActivity", "Error sending message", e);
                });
    }

    private void saveChatroomId(String senderId, String receiverId, String chatroomId) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("chatroomId", chatroomId);

        // Save chatroom ID to Firestore
        db.collection("chatrooms")
                .document(senderId)
                .collection("friendIds")
                .document(receiverId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatDetailActivity", "Chat room ID saved successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailActivity", "Error saving chat room ID", e);
                });

        db.collection("chatrooms")
                .document(receiverId)
                .collection("friendIds")
                .document(senderId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatDetailActivity", "Chat room ID saved successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailActivity", "Error saving chat room ID", e);
                });
    }

    //check chat room exists between the sender and receiver
    private void checkChatroomExistence(String senderId, String receiverId, ChatroomExistenceCallback callback) {
        db.collection("chatrooms")
                .document(senderId)
                .collection("friendIds")
                .document(receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            chatroomId = document.getString("chatroomId");
                            callback.onChatroomExistenceChecked(chatroomId);
                        } else {
                            // not Exist
                            chatroomId = createChatroomId();
                            saveChatroomId(senderId, receiverId, chatroomId);
                            saveChatroomId(receiverId, senderId, chatroomId);
                            //saveChatroomId(receiverId, senderId, chatroomId);
                            callback.onChatroomExistenceChecked(chatroomId);
                        }
                    } else {
                        // Error occurred while checking chat room existence
                        Log.e("ChatDetailActivity", "Error checking chat room existence", task.getException());
                    }
                });
    }

    public interface ChatroomExistenceCallback {
        void onChatroomExistenceChecked(String chatroomId);
    }

    private void getUserAuthId(String uId, final UserAuthIdCallback callback) {
        db.collection("userIds")
                .document(uId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            receiveId = document.getString("auth_id");
                            callback.onUserAuthIdFound(receiveId);
                            Log.d("ChatDetailActivity", "User document exist for receiveId: " + receiveId);
                        } else {
                            // Handle if the document doesn't exist
                            Log.d("ChatDetailActivity", "User document doesn't exist for receiveId: " + receiveId);
                            callback.onUserAuthIdFound(null);
                        }
                    } else {
                        // Error occurred while fetching user document
                        Log.e("ChatDetailActivity", "Error fetching user document for receiveId: " + receiveId, task.getException());
                        callback.onUserAuthIdFound(null);
                    }
                });
    }
    public interface UserAuthIdCallback {
        void onUserAuthIdFound(String authId);
    }

    private void deleteAllMessages(String chatroomId) {
        db.collection("chats")
                .document(chatroomId)
                .collection("messages")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot snapshot : task.getResult()) {
                            snapshot.getReference().delete();
                        }
                    } else {
                        Log.e("ChatDetailActivity", "Error deleting messages", task.getException());
                    }
                });
    }

    private void choosePicture() {
        // Pick image from gallery
        ImagePicker.Companion.with(this).galleryOnly().crop(10f, 16f).start();
    }

    // Handle the result of the imagepicker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ImagePicker.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Get the Uri of the cropped image
            Uri imageUri = data.getData();
            // Upload the cropped image to Firebase Storage
            uploadBackgroundImageToFirebase(imageUri);
        }
    }

    // Upload image to Firebase Storage & set to background immediately
    private void uploadBackgroundImageToFirebase(Uri imageUri) {
        String filename = auth.getUid().toString();
        StorageReference backgroundRef = storageReference.child("chat_background").child(filename);

        // Upload cropped image file to Firebase Storage
        backgroundRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("ChatDetailActivity", "Image uploaded successfully");
                    backgroundRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Load downloaded image into the chatBackground ImageView
                        AndroidUtil.setBackgroundPic(ChatDetailActivity.this, uri, binding.chatRecyclerView);
                    }).addOnFailureListener(e -> {
                        Log.e("ChatDetailActivity", "Error getting download URL", e);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailActivity", "Error uploading image", e);
                });
    }

    // Load background image from Firebase Storage
    private void loadBackground() {
        Log.d("loadBackground:", "loaded Background");
        String filename = auth.getUid().toString();
        StorageReference backgroundRef = storageReference.child("chat_background").child(filename);

        // Download image from Firebase and set it as the chat background
        backgroundRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Load downloaded image into the chatBackground ImageView
            AndroidUtil.setBackgroundPic(ChatDetailActivity.this, uri, binding.chatRecyclerView);
        }).addOnFailureListener(e -> {
            Log.d("ChatDetailActivity", "No background image / Error getting download URL for background image", e);
        });
    }
}
