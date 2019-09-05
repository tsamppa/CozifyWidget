package com.cozify.cozifywidget;


import android.app.Activity;
import android.app.Instrumentation;
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
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;

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
import static org.hamcrest.CoreMatchers.instanceOf;
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
    private Instrumentation.ActivityMonitor monitor;
    private Activity activity;
    private int widgetCount = 0;


//    @Rule
//    public ActivityTestRule<CozifyWidgetSetupActivity> mActivityTestRule = new ActivityTestRule<>(CozifyWidgetSetupActivity.class, true, false);

    @BeforeClass
    public static void prepare() {
        removeWidget("Scene or Device");
    }

    @Before
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        monitor = InstrumentationRegistry.getInstrumentation().addMonitor("com.cozify.cozifywidget.CozifyAppWidgetConfigure", null, false);
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

        int displayWidth = device.getDisplayWidth();
        int displayHeight = device.getDisplayHeight();
        int displayCenterX = displayWidth / 2;
        int displayCenterY = displayHeight / 2;

        device.swipe(displayCenterX, displayCenterY, displayCenterX, displayCenterY, 150);

        UiObject2 widgetMenu = device.findObject(By.text("Widgets"));
        if (widgetMenu == null) {
            widgetMenu = device.findObject(By.text("WIDGETS"));
        }
        if (widgetMenu == null) {
            widgetMenu = device.findObject(By.text("Pienoisohjelmat"));
        }
        assertThat(widgetMenu, notNullValue());
        widgetMenu.click();
        device.waitForIdle();

        UiObject2 widget = findMyWidget(WIDGET_NAME);

        if (widget == null) {
            UiScrollable widgets = new UiScrollable(new UiSelector().scrollable(true));
            widgets.setAsHorizontalList();
            try {
                widgets.flingToEnd(2);
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }

            widget = findMyWidget(WIDGET_NAME);
            while (widget == null) {
                swipeScroll(WIDGET_SELECTION_AT_X ? displayWidth*7/10 : displayHeight*7/10);
                widget = findMyWidget(WIDGET_NAME);
            }
            swipeScroll(WIDGET_SELECTION_AT_X ? displayWidth*2/10 : displayHeight*2/10);
        }
        // Throw the selected widget on screen
        Rect b = widget.getVisibleBounds();
        Point c = new Point(b.left+120, b.bottom+150);
        Point dest = new Point(displayCenterX/2,displayCenterY/2);
        Point[] pa = new Point[3];
        pa[0] = c;
        pa[1] = c;
        pa[2] = dest;
        device.swipe(pa, 200);
        Log.d("WIDGET POS", String.format("Widget position:%d, %d", dest.x, dest.y));
        activity = monitor.waitForActivityWithTimeout(2000);

    }

    public static void removeWidget(UiObject2 widget) {
    }

    public static void removeWidget(String widgetName) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
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
        int displayHeight = device.getDisplayHeight();
        int centerX = displayWidth / 2;
        int centerY = displayHeight / 2;

        if (WIDGET_SELECTION_AT_X) {
            // Swipe left to right
            device.swipe(amount, centerY, 0, centerY, 150);
        } else {
            // Swipe top to bottom
            device.swipe(centerX, amount, centerX, 0, 150);
        }
    }

    private UiObject2 findMyWidget(String withName)  {
        return device.findObject(By.text(withName));
    }

    private void armAndControl(UiObject2 widget) {
        widget.click();
        sleep(4000);
        widget.click();
        sleep(8000);
    }

    private void clickAndWait(UiObject2 widget) {
        widget.click();
        sleep(8000);
    }

    private void clickWaitArmControl(UiObject2 widget) {
        device.waitForIdle();
        clickAndWait(widget);
        device.waitForIdle();
        armAndControl(widget);
        device.waitForIdle();
    }

    @Test
    public void c1_cozifyWidgetSetupActivityTestNetworkLost() {
        String name = "Bad internet";
        UiObject2 widget1 = createAndConfigWidget(name, 1, "Test Device", "Small");
        enableNetwork(false);
        clickWaitArmControl(widget1);
        enableNetwork(false);
        clickWaitArmControl(widget1);
        removeWidget(name);
    }

    @Test
    public void c1_cozifyWidgetSetupActivityTestLongName() {
        String name = "12345678901234567890123456789012345678901234567890";
        UiObject2 widget1 = createAndConfigWidget(name, 1, "Test Device", "Small");
        UiObject2 widget2 = createAndConfigWidget(name, 1, "Test Device", "Medium");
        UiObject2 widget3 = createAndConfigWidget(name, 1, "Test Device", "Large");
        clickWaitArmControl(widget1);
        clickWaitArmControl(widget2);
        clickWaitArmControl(widget3);
        removeWidget(name);
    }

    @Test
    public void c1_cozifyWidgetSetupActivityTestScreenshot() {
        UiObject2 widget1 = createAndConfigWidget("HOME ALARM", 1, "Test Device", "Medium");
        UiObject2 widget2 = createAndConfigWidget("A/C", 1, "Test Device", "Large");
        UiObject2 widget3 = createAndConfigWidget("NIGHT", 1, "Test Device", "Medium");
        UiObject2 widget4 = createAndConfigWidget("Bathroom", 1, "Test Sensor", "Small");
        clickAndWait(widget1);
        clickAndWait(widget2);
        clickAndWait(widget3);
        clickAndWait(widget4);
        //takeScreenshot("homeScreenWithDevices");
        sleep(10000);
        removeWidget("HOME ALARM");
        removeWidget("A/C");
        removeWidget("NIGHT");
        removeWidget("Bathroom");
    }

    private void takeScreenshot(String name) {
        File path = new File("/sdcard/"+name+".png");
        device.takeScreenshot(path);
    }


    @Test
    public void c2_cozifyWidgetSetupActivityTestCreateAllTypes() {
        UiObject2 widget1 = createAndConfigWidget("T Device", 1, "Test Device", "Medium");
        UiObject2 widget2 = createAndConfigWidget("T Sensor", 1, "Test Sensor", "Medium");
        UiObject2 widget3 = createAndConfigWidget("T Scene", 1, "Scene: Test Scene", "Medium");
        UiObject2 widget4 = createAndConfigWidget("T Group", 1, "Group: Test Group", "Medium");
        widget1.click();
        widget2.click();
        widget3.click();
        widget4.click();
        sleep(7000);
        widget1.click();
        widget2.click();
        widget3.click();
        widget4.click();
        sleep(1000);
        widget1.click();
        widget2.click();
        widget3.click();
        widget4.click();
        sleep(10000);
        removeWidget("T Group");
        removeWidget("T Sensor");
        removeWidget("T Scene");
        removeWidget("T Device");
    }


    @Test
    public void c2_cozifyWidgetSetupActivityTestCreate1() {
        createWidget();
        configWidget("T1", 1, 75);
        UiObject2 widget1 = findMyWidget("T1");
        assertThat(widget1, is(notNullValue()));
        widget1.click();
        sleep(6000);
        widget1.click();
        sleep(2000);
        widget1.click();
        sleep(1000);
    }

    @Test
    public void c3_cozifyWidgetSetupActivityTestCreate2() {
        createWidget();
        configWidget("T2", 1, 29);
        UiObject2 widget2 = findMyWidget("T2");
        assertThat(widget2, is(notNullValue()));
        widget2.click();
        sleep(6000);
        widget2.click();
        sleep(2000);
        widget2.click();
        sleep(1000);
    }

    @Test
    public void c4_cozifyWidgetSetupActivityTestCreate3() {
        for (int i = 3; i < 12; i++) {
            createWidget();
            configWidget("T" + i, 1, 7);
            UiObject2 widget = findMyWidget("T"+i);
            assertThat(widget, is(notNullValue()));
        }
        clickAll();
        clickAll();
        clickAll();
        clickAll();
    }

    private void clickAll() {
        for (int i = 3; i < 12; i++) {
            UiObject2 widget = findMyWidget("T"+i);
            assertThat(widget, is(notNullValue()));
            widget.click();
            sleep(100);
        }
        sleep(1000);
    }

    @Test
    public void c5_cozifyWidgetSetupActivityTestClicks1() {
        UiObject2 widget1 = findMyWidget("T1");
        assertThat(widget1, is(notNullValue()));
        widget1.click();
        sleep(1000);
        widget1.click();
        sleep(1000);
    }

    @Test
    public void c6_cozifyWidgetSetupActivityTestClicks2() {
        UiObject2 widget2 = findMyWidget("T2");
        assertThat(widget2, is(notNullValue()));
        widget2.click();
        sleep(1000);
        widget2.click();
        sleep(1000);
    }

    @Test
    public void c7_cozifyWidgetSetupActivityTestClicks3() {
        UiObject2 widget1 = findMyWidget("T1");
        assertThat(widget1, is(notNullValue()));
        UiObject2 widget2 = findMyWidget("T2");
        assertThat(widget2, is(notNullValue()));
        widget2.click();
        sleep(1000);
        widget2.click();
        widget1.click();
        sleep(1000);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget2.click();
        sleep(100);
        widget1.click();
        sleep(100);
        widget2.click();
        sleep(100);
    }


    @Test
    public void c9_cozifyWidgetSetupActivityTestRemove() {
        for (int i = 1; i < 12; i++) {
            removeWidget("T"+i);
        }
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
        assertThat(editText, notNullValue());
        editText.perform(replaceText(widgetName), closeSoftKeyboard());

        editText.perform(pressImeActionButton());

    }

    private void testOnButton() {
        ViewInteraction button = onView(
                allOf(withId(R.id.test_control_on_button),
                        withText("Test ON now"),
                        isDisplayed()));
        assertThat(button, notNullValue());
        button.perform(click());
        sleep(2000);
    }

    private void testOffButton() {
        ViewInteraction button2 = onView(
                allOf(withId(R.id.test_control_off_button),
                        withText("Test OFF now"),
                        isDisplayed()));
        assertThat(button2, notNullValue());
        button2.perform(click());
        sleep(2000);
    }

    private void selectHub(int hubPos) {
        ViewInteraction spinner = onView(
                allOf(withId(R.id.spinner_hubs),
                        isDisplayed()));
        spinner.perform(click());

        DataInteraction checkedTextView = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(hubPos);
        assertThat(checkedTextView, notNullValue());
        checkedTextView.perform(click());

    }

    private void configWidget(String widgetName, int hubPos, int devicePos) {
        device.waitForIdle();

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

    private UiObject2 createAndConfigWidget(String widgetName, int hubPos, String deviceName, String fontSize) {
        createWidget();
        device.waitForIdle();
        selectHub(hubPos);
        selectDevice(deviceName);
        setWidgetName(widgetName);
        selectFontSize(fontSize);
        onView(allOf(withId(R.id.create_button), isDisplayed())).perform(click());
        UiObject2 widget1 = findMyWidget(widgetName);
        assertThat(widget1, is(notNullValue()));
        device.waitForIdle();
        return widget1;
    }

    private void selectFontSize(String fontSize) {
        if (fontSize.equals("Small"))
            onView(allOf(withId(R.id.text_size_small), withText(fontSize), isDisplayed())).perform(click());
        if (fontSize.equals("Medium"))
            onView(allOf(withId(R.id.text_size_medium), withText(fontSize), isDisplayed())).perform(click());
        if (fontSize.equals("Large"))
            onView(allOf(withId(R.id.text_size_large), withText(fontSize), isDisplayed())).perform(click());
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
                        isDisplayed()));
        spinner3.perform(click());

        DataInteraction checkedTextView2 = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(pos);
        assertThat(checkedTextView2, notNullValue());
        checkedTextView2.perform(click());
    }

    private void selectDevice(String name) {
        onView(allOf(withId(R.id.spinner_devices), isDisplayed())).perform(click());
        device.waitForIdle();
        DataInteraction deviceSpinner = onData(allOf(is(instanceOf(String.class)), is(name)));
        assertThat(deviceSpinner, notNullValue());
        deviceSpinner.perform(click());
        device.waitForIdle();
    }

    private void enableNetwork(boolean on) {
        try {
            if (on) {
                Runtime.getRuntime().exec("\"svc wifi enable\"");
                Runtime.getRuntime().exec("\"svc data enable\"");
            } else {
                Runtime.getRuntime().exec("\"svc wifi disable\"");
                Runtime.getRuntime().exec("\"svc data disable\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        device.waitForIdle();
    }

}
