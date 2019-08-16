package com.cozify.cozifywidget;


import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.os.SystemClock.sleep;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CozifyWidgetSetupActivityTestOnOff {
    private static final String COZIFY_PACKAGE
            = "com.cozify.cozifywidget";
    private static final int LAUNCH_TIMEOUT = 5000;
    String WIDGET_NAME = "Cozify Scene and Device Control";
    boolean WIDGET_SELECTION_AT_X = false;
    private UiDevice device;
    private int widgetCount = 1;
    private Point[] widgetLocations = new Point[6];

    @Rule
    public ActivityTestRule<CozifyWidgetSetupActivity> mActivityTestRule = new ActivityTestRule<>(CozifyWidgetSetupActivity.class);

    @Before
    public void startMainActivityFromHomeScreen() {
        widgetLocations[0] = new Point(80,250);
        widgetLocations[1] = new Point(80*2,250);
        widgetLocations[2] = new Point(80*3,250);
        widgetLocations[3] = new Point(80*4,250);
        widgetLocations[4] = new Point(80*5,250);
        widgetLocations[5] = new Point(80,320);
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        createWidget();
    }

    private void createWidget() {

        // Start from the home screen
        device.pressHome();
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)),
                LAUNCH_TIMEOUT);

        device.swipe(device.getDisplayWidth()/2, device.getDisplayHeight()/2,
                device.getDisplayWidth()/2, device.getDisplayHeight()/2, 150);
        device.findObject(By.text("Widgets")).click();
        device.waitForIdle();

        UiScrollable widgets = new UiScrollable(new UiSelector().scrollable(true));
        widgets.setAsHorizontalList();
        try {
            widgets.flingToEnd(2);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        int displayWidth = device.getDisplayWidth();
        int displayHeight = device.getDisplayWidth();
        int centerX = displayWidth / 2;
        int centerY = displayHeight / 2;

        UiObject2 widget = findMyWidget(WIDGET_NAME);
        while (widget == null) {
            swipeScroll(WIDGET_SELECTION_AT_X ? centerX : centerY);
            widget = findMyWidget(WIDGET_NAME);
        }

        // Throw the selected widget on screen
        Rect b = widget.getVisibleBounds();
        Point c = new Point(b.left + 150, b.bottom + 150);
        Point dest = widgetLocations[widgetCount-1];
        Point[] pa = new Point[3];
        pa[0] = c;
        pa[1] = c;
        pa[2] = dest;
        device.swipe(pa, 150);

        widgetCount = widgetCount + 1;
    }

    @After
    public void removeWidgets() {
        for (int i=0; i <6; i++) {
            Point[] pa = new Point[3];
            pa[0] = widgetLocations[i];
            pa[1] = widgetLocations[i];
            pa[2] = new Point(device.getDisplayWidth()/2, 50);
            device.swipe(pa, 150);
        }
    }

    private void swipeScroll(int amount) {
        int displayWidth = device.getDisplayWidth();
        int displayHeight = device.getDisplayWidth();
        int centerX = displayWidth / 2;
        int centerY = displayHeight / 2;

        if (WIDGET_SELECTION_AT_X) {
            // Swipe left to right
            device.swipe(amount, centerY, 0, centerX, 150);
        } else {
            // Swipe top to bottom
            device.swipe(centerY, amount, centerX, 0, 150);
        }
    }

    private UiObject2 findMyWidget(String withName)  {
        return device.findObject(By.text(withName));
    }


    @Test
    public void cozifyWidgetSetupActivityTestOnOff() {
        configWidget();

    }
    @Test
    public void cozifyWidgetSetupActivityTestOnOff2() {
        createWidget();
        configWidget();
    }
    @Test
    public void cozifyWidgetSetupActivityTestOnOff3() {
        createWidget();
        configWidget();
    }
    @Test
    public void cozifyWidgetSetupActivityTestOnOff4() {
        createWidget();
        configWidget();
    }
    private void configWidget() {
        ViewInteraction spinner = onView(
                allOf(withId(R.id.spinner_hubs),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()));
        spinner.perform(click());

        device.waitForIdle();

        DataInteraction checkedTextView = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(1);
        checkedTextView.perform(click());

        device.waitForIdle();

        ViewInteraction spinner2 = onView(
                allOf(withId(R.id.spinner_devices),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()));
        spinner2.perform(click());

        device.waitForIdle();

        DataInteraction checkedTextView2 = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(3);
        checkedTextView2.perform(click());

        device.waitForIdle();

        ViewInteraction button = onView(
                allOf(withId(R.id.test_control_on_button), withText("Test ON now"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                5),
                        isDisplayed()));
        button.perform(click());
        sleep(2000);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.test_control_off_button), withText("Test OFF now"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                6),
                        isDisplayed()));
        button2.perform(click());
        sleep(2000);


        ViewInteraction button3 = onView(
                allOf(withId(R.id.create_button),
                        isDisplayed()));
        button3.perform(click());

        device.waitForIdle();

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
