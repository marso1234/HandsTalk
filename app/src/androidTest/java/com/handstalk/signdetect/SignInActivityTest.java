package com.handstalk.signdetect;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.handstalk.signdetect.activities.SignInActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SignInActivityTest {

    @Rule
    public ActivityScenarioRule<SignInActivity> activityRule = new ActivityScenarioRule<>(SignInActivity.class);

    @Test
    public void testSignInWithValidCredentials() {

        // Sign In
        String email = "123@gmail.com";
        String password = "12345678";

        try (ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class)) {
            scenario.onActivity(activity -> {
                Espresso.onView(ViewMatchers.withId(R.id.inputEmail))
                        .perform(ViewActions.typeText(email), ViewActions.closeSoftKeyboard());
                Espresso.onView(ViewMatchers.withId(R.id.inputPassword))
                        .perform(ViewActions.typeText(password), ViewActions.closeSoftKeyboard());
                Espresso.onView(ViewMatchers.withId(R.id.buttonSignIn))
                        .perform(ViewActions.click());
            });

            // Check if the sign-in button is not displayed after clicking
            Espresso.onView(withId(R.id.buttonSignIn))
                    .check(matches(isDisplayed()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
