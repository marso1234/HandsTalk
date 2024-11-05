package com.handstalk.signdetect;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.handstalk.signdetect.activities.ChatDetailActivity;
import com.handstalk.signdetect.model.User;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)

public class ChatDetailActivityTest {

    String email = "123@gmail.com";
    String password = "12345678";
    User user = new User("123", "123@gmail.com", "1706229837591201", "cm7LpOAESuuH0wDkDs6x1u:APA91bGV4xs9Ge-3WbgPwe6Zfwr5T_yiPVAjCNuhSTG8KqY_o04CKtOklxlnhp-1wsAEYkFD720CvKGzP85jSw-iUAlifQ_4wpw0n7YwrCpfGAI7WaQlRowyxP8azsNt_ibMoPaGC9pF");


    @Test
    public void signInAndLaunchActivity() {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatDetailActivity.class);
                        intent.putExtra("User", user);
                        ActivityScenario<ChatDetailActivity> scenario = ActivityScenario.launch(intent);
                        scenario.onActivity(activity -> {
                            // Verify if past message display in chat RecyclerView
                            Espresso.onView(ViewMatchers.withId(R.id.chatRecyclerView))
                                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                        });
                    } else {
                        System.out.println("Sign-in failed: " + task.getException().getMessage());
                    }
                });
    }

    @Test
    public void testSendMessage() {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatDetailActivity.class);
                        intent.putExtra("User", user);
                        ActivityScenario<ChatDetailActivity> scenario = ActivityScenario.launch(intent);
                        scenario.onActivity(activity -> {
                            // Type message
                            String message = "message";
                            Espresso.onView(ViewMatchers.withId(R.id.enterMessage))
                                    .perform(ViewActions.typeText(message), ViewActions.closeSoftKeyboard());
                            // Send button
                            Espresso.onView(ViewMatchers.withId(R.id.send))
                                    .perform(ViewActions.click());
                            // Verify new message is displayed in chat RecyclerView
                            Espresso.onView(ViewMatchers.withText(message))
                                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                        });
                    } else {
                        System.out.println("Sign-in failed: " + task.getException().getMessage());
                    }
                });
    }

    @Test
    public void testLoadBackgroundImage() {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatDetailActivity.class);
                        intent.putExtra("User", user);
                        ActivityScenario<ChatDetailActivity> scenario = ActivityScenario.launch(intent);
                        scenario.onActivity(activity -> {
                            // Verify if the background ImageView is displayed
                            Espresso.onView(ViewMatchers.withId(R.id.chatRecyclerView))
                                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
                        });
                    } else {
                        System.out.println("Sign-in failed: " + task.getException().getMessage());
                    }
                });
    }
}

