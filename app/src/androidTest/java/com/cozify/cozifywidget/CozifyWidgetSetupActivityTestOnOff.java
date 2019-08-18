package com.cozify.cozifywidget;


import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
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
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.os.SystemClock.sleep;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CozifyWidgetSetupActivityTestOnOff {
    private static final String COZIFY_PACKAGE
            = "com.cozify.cozifywidget";
    private static final int LAUNCH_TIMEOUT = 5000;
    String WIDGET_NAME = "Cozify Scene and Device Control";
    boolean WIDGET_SELECTION_AT_X = false;
    private UiDevice device;
    private int widgetCount = 0;

    @Rule
    public ActivityTestRule<CozifyWidgetSetupActivity> mActivityTestRule = new ActivityTestRule<>(CozifyWidgetSetupActivity.class, true, false);

    @Before
    public void startMainActivityFromHomeScreen() {
        int margin = 60;
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    private void createWidget() {
        widgetCount = widgetCount + 1;

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
        Point dest = new Point(300,300);
        Point[] pa = new Point[3];
        pa[0] = c;
        pa[1] = c;
        pa[2] = dest;
        device.swipe(pa, 200);
        Log.d("WIDGET POS", String.format("Widget position:%d, %d", dest.x, dest.y));

    }

    public void removeWidgets() {
        device.pressHome();
        removeWidget("T1");
        removeWidget("T2");
    }

    public void removeWidget(String widgetName) {
        device.pressHome();
        UiObject2 widget = device.findObject(By.text(widgetName));
        while (widget != null) {
            Rect r = widget.getVisibleBounds();
            Point c = new Point(r.centerX(), r.centerY());
            Point[] pa = new Point[3];
            pa[0] = c;
            pa[1] = c;
            pa[2] = new Point(device.getDisplayWidth()/2, 120);
            device.swipe(pa, 150);
            widget = device.findObject(By.text(widgetName));
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
    public void c1_cozifyWidgetSetupActivityTestCreate1() {
        createWidget();
        configWidget("T1", 1, 75);
        UiObject2 widget1 = findMyWidget("T1");
        assertThat(widget1, is(notNullValue()));
        widget1.click();
        sleep(6000);
        widget1.click();
        sleep(2000);
        widget1.click();
    }

    @Test
    public void c2_cozifyWidgetSetupActivityTestCreate2() {
        createWidget();
        configWidget("T2", 1, 29);
        UiObject2 widget2 = findMyWidget("T2");
        assertThat(widget2, is(notNullValue()));
        widget2.click();
        sleep(6000);
        widget2.click();
        sleep(2000);
        widget2.click();

    }

    @Test
    public void c3_cozifyWidgetSetupActivityTestClicks1() {
        UiObject2 widget1 = findMyWidget("T1");
        assertThat(widget1, is(notNullValue()));
        widget1.click();
        sleep(1000);
        widget1.click();
    }

    @Test
    public void c4_cozifyWidgetSetupActivityTestClicks2() {
        UiObject2 widget2 = findMyWidget("T2");
        assertThat(widget2, is(notNullValue()));
        widget2.click();
        sleep(1000);
        widget2.click();
    }


    @Test
    public void c5_cozifyWidgetSetupActivityTestRemove() {
        removeWidget("T1");
        removeWidget("T1");
    }

    private void setWidgetName(String widgetName) {
        ViewInteraction editText = onView(
                allOf(withId(R.id.device_name_edit),
                        childAtPosition(
                                childAtPosition(
                                        allOf(withId(R.id.device_name), withContentDescription("Label")),
                                        0),
                                0),
                        isDisplayed()));
        editText.perform(replaceText(widgetName), closeSoftKeyboard());

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.device_name_edit), withText(widgetName),
                        childAtPosition(
                                childAtPosition(
                                        allOf(withId(R.id.device_name), withContentDescription("Label")),
                                        0),
                                0),
                        isDisplayed()));
        editText2.perform(pressImeActionButton());

    }

    private void testOnButton() {
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
    }

    private void testOffButton() {
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
    }

    private void selectHub(int hubPos) {
        ViewInteraction spinner = onView(
                allOf(withId(R.id.spinner_hubs),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()));
        spinner.perform(click());

        DataInteraction checkedTextView = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(hubPos);
        checkedTextView.perform(click());

    }

    private void configWidget(String widgetName, int hubPos, int devicePos) {
        selectHub(hubPos);

        device.waitForIdle();

        selectDevice(devicePos);

        setWidgetName(widgetName);

        testOnButton();
        testOffButton();

        device.waitForIdle();

        ViewInteraction button3 = onView(
                allOf(withId(R.id.create_button),
                        isDisplayed()));
        button3.perform(click());

        device.waitForIdle();
        sleep(2000);

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

    private void selectDevice(int pos) {
        ViewInteraction spinner3 = onView(
                allOf(withId(R.id.spinner_devices),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()));
        spinner3.perform(click());

        DataInteraction checkedTextView2 = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(pos);
        checkedTextView2.perform(click());
    }
}
