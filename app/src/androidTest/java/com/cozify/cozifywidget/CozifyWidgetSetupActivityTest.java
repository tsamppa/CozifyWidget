package com.cozify.cozifywidget;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CozifyWidgetSetupActivityTest {

    @Rule
    public ActivityTestRule<CozifyWidgetSetupActivity> mActivityTestRule = new ActivityTestRule<>(CozifyWidgetSetupActivity.class);

    @Test
    public void cozifyWidgetSetupActivityTest() {
        ViewInteraction editText = onView(
                allOf(withId(R.id.device_name_edit),
                        childAtPosition(
                                childAtPosition(
                                        allOf(withId(R.id.device_name), withContentDescription("Label")),
                                        0),
                                0),
                        isDisplayed()));
        editText.perform(replaceText("T2"), closeSoftKeyboard());

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.device_name_edit), withText("T2"),
                        childAtPosition(
                                childAtPosition(
                                        allOf(withId(R.id.device_name), withContentDescription("Label")),
                                        0),
                                0),
                        isDisplayed()));
        editText2.perform(pressImeActionButton());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
