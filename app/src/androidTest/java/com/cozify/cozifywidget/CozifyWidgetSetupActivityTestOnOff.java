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
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CozifyWidgetSetupActivityTestOnOff {
    private static final String COZIFY_PACKAGE = "com.cozify.cozifywidget";
    private static final int LAUNCH_TIMEOUT = 5000;
    private String WIDGET_NAME = "Cozify Scene and Device Control";
    private boolean WIDGET_SELECTION_AT_X = false;
    private static UiDevice device;
    private Instrumentation.ActivityMonitor monitor;
    private int widgetCount = 0;
    private Activity activity;


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

    private void createWidget(boolean doubleSize) {
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
        if (!doubleSize) {
            c = new Point(b.left+580, b.bottom+150);
        }
        Point dest = new Point(displayCenterX/2,displayCenterY/2);
        Point[] pa = new Point[3];
        pa[0] = c;
        pa[1] = c;
        pa[2] = dest;
        device.swipe(pa, 200);
        Log.d("WIDGET POS", String.format("Widget position:%d, %d", dest.x, dest.y));
        activity = monitor.waitForActivityWithTimeout(2000);

    }

    public void checkIcon(String wigdetName, String drawableName) {
        UiObject2 widget = findMyWidget(wigdetName);
        assertThat(widget, is(notNullValue()));
        String p = widget.getContentDescription();
        assertThat(p, is(drawableName));
    }

    public static void removeWidget(UiObject2 widget) {
        Rect r = widget.getVisibleBounds();
        Point c = new Point(r.centerX(), r.centerY());
        Point[] pa = new Point[3];
        pa[0] = c;
        pa[1] = c;
        pa[2] = new Point(device.getDisplayWidth()/2, 120);
        device.swipe(pa, 150);
    }

    public static void removeWidget(String widgetName) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.pressHome();
        UiObject2 widget = device.findObject(By.textContains(widgetName));
        while (widget != null) {
            removeWidget(widget);
            widget = device.findObject(By.textContains(widgetName));
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
        return device.findObject(By.textContains(withName));
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
        UiObject2 widget1 = createAndConfigWidgetAndSetOn(name, "Samppa's Hub", "Test Device", "Small", true, false);
        checkIcon(name, "appwidget_button_clickable_on");
        enableNetwork(false);
        clickWaitArmControl(widget1);
        enableNetwork(false);
        clickWaitArmControl(widget1);
        removeWidget(name);
    }


    @Test
    public void c1_cozifyWidgetSetupActivityTestIconStatesSingle() {
        testIconStates(false);
    }

    @Test
    public void c1_cozifyWidgetSetupActivityTestIconStatesDouble() {
        testIconStates(true);
    }

    private void testIconStates(boolean doubleSize) {

        String name = "ICON";
        removeWidget(name);

        UiObject2 widget = createAndConfigWidgetAndSetOn(name, "Samppa's Hub", "Test Device", "Small", true, doubleSize);
        sleep(3000);
        checkIcon(name, "appwidget_button_clickable_on");
        widget.click();
        sleep(100);
        checkIcon(name, "appwidget_button_arming_on");
        sleep(3000);
        checkIcon(name, "appwidget_button_armed_on");
        widget.click();
        sleep(100);
        checkIcon(name, "appwidget_button_controlling_off");
        sleep(5000);
        checkIcon(name, "appwidget_button_clickable_off");
        sleep(5000);
        widget.click();
        sleep(100);
        checkIcon(name, "appwidget_button_arming_off");
        sleep(3000);
        checkIcon(name, "appwidget_button_armed_off");
        widget.click();
        sleep(100);
        checkIcon(name, "appwidget_button_controlling_on");
        sleep(5000);
        checkIcon(name, "appwidget_button_clickable_on");

        removeWidget(name);
    }

    @Test
    public void c1_cozifyWidgetSetupActivityTestLongName() {
        String name = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        UiObject2 widget1 = createAndConfigWidget(name, "Samppa's Hub", "Test Device", "Small", false);
        UiObject2 widget2 = createAndConfigWidget(name, "Samppa's Hub", "Test Device", "Medium", false);
        UiObject2 widget3 = createAndConfigWidget(name, "Samppa's Hub", "Test Device", "Large", false);
        UiObject2 widget1d = createAndConfigWidget(name, "Samppa's Hub", "Test Device", "Small", true);
        UiObject2 widget2d = createAndConfigWidget(name, "Samppa's Hub", "Test Device", "Medium", true);
        UiObject2 widget3d = createAndConfigWidget(name, "Samppa's Hub", "Test Device", "Large", true);
        clickWaitArmControl(widget1);
        clickWaitArmControl(widget2);
        clickWaitArmControl(widget3);
        clickWaitArmControl(widget1d);
        clickWaitArmControl(widget2d);
        clickWaitArmControl(widget3d);
        removeWidget(name);
    }

    @Test
    public void c1_cozifyWidgetSetupActivityTestScreenshot() {
        removeWidget("Bathroom");
        UiObject2 widget1 = createAndConfigWidget("HOME ALARM", "Samppa's Hub", "Test Device", "Medium", false);
        UiObject2 widget2 = createAndConfigWidget("A/C", "Samppa's Hub", "Test Device", "Large", false);
        UiObject2 widget3 = createAndConfigWidget("NIGHT", "Samppa's Hub", "Test Device", "Medium", false);
        UiObject2 widget4 = createAndConfigWidget("Bathroom", "Samppa's Hub", "Test Sensor", "Small", false);
        UiObject2 widget1d = createAndConfigWidget("HOME ALARM", "Samppa's Hub", "Test Device", "Medium", true);
        UiObject2 widget2d = createAndConfigWidget("A/C", "Samppa's Hub", "Test Device", "Large", true);
        UiObject2 widget3d = createAndConfigWidget("NIGHT", "Samppa's Hub", "Test Device", "Medium", true);
        UiObject2 widget4d = createAndConfigWidget("Bathroom", "Samppa's Hub", "Test Sensor", "Small", true);
        clickAndWait(widget1);
        clickAndWait(widget2);
        clickAndWait(widget3);
        clickAndWait(widget4);
        clickAndWait(widget1d);
        clickAndWait(widget2d);
        clickAndWait(widget3d);
        clickAndWait(widget4d);
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
        UiObject2 widget1 = createAndConfigWidgetAndSetOn("T Scene", "Samppa's Hub", "Scene: Test Scene", "Medium", true, false);
        sleep(1000);
        UiObject2 widget2 = createAndConfigWidget("T Device", "Samppa's Hub", "Test Device", "Medium", false);
        sleep(1000);
        UiObject2 widget3 = createAndConfigWidget("T TEMP", "Samppa's Hub", "Test Sensor", "Medium", false);
        sleep(1000);
        UiObject2 widget4 = createAndConfigWidget("T Group", "Samppa's Hub", "Group: Test Group", "Medium", false);
        sleep(1000);
        UiObject2 widget5 = createAndConfigWidget("T LUX", "Samppa's Hub", "Keittiön liik", "Medium", false);
        sleep(1000);
        UiObject2 widget6 = createAndConfigWidget("T WATT", "Samppa's Hub", "Kiertovesipumppu", "Medium", false);
        sleep(1000);
        UiObject2 widget7 = createAndConfigWidget("T HUM", "Samppa's Hub", "Kylppäri kosteus", "Medium", false);
        sleep(3000);
        widget1.click();
        widget2.click();
        widget3.click();
        widget4.click();
        widget5.click();
        widget6.click();
        widget7.click();
        sleep(7000);
        widget1.click();
        widget2.click();
        widget3.click();
        widget4.click();
        widget5.click();
        widget6.click();
        widget7.click();
        sleep(10000);
        removeWidget("T Group");
        removeWidget("T TEMP");
        removeWidget("T Scene");
        removeWidget("T Device");
        removeWidget("T LUX");
        removeWidget("T WATT");
        removeWidget("T HUM");
    }

    @Test
    public void c4_cozifyWidgetSetupActivityTestCreate3() {
        for (int i = 3; i < 12; i++) {
            createAndConfigWidget("T" + i, "Samppa's Hub", "Test Device", "Medium", false);
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
        device.waitForIdle();
        editText.perform(replaceText(widgetName), closeSoftKeyboard());
        editText.perform(pressImeActionButton());
        device.waitForIdle();
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

    private void createAndPrepareWidget(String widgetName, String hubName, String deviceName, boolean doubleSize) {
        createWidget(doubleSize);
        device.waitForIdle();
        selectHub(hubName);
        selectDevice(deviceName);
        setWidgetName(widgetName);
    }

    private UiObject2 returnCreatedWidget(String widgetName) {
        onView(allOf(withId(R.id.create_button), isDisplayed())).perform(click());
        UiObject2 widget = findMyWidget(widgetName);
        assertThat(widget, is(notNullValue()));
        device.waitForIdle();
        return widget;
    }

    private UiObject2 createAndConfigWidget(String widgetName, String hubName, String deviceName, String fontSize, boolean doubleSize) {
        createAndPrepareWidget(widgetName, hubName, deviceName, doubleSize);
        selectFontSize(fontSize);
        return returnCreatedWidget(widgetName);
    }

    private UiObject2 createAndConfigWidgetAndSetOn(String widgetName, String hubName, String deviceName, String fontSize, boolean on, boolean doubleSize) {
        createAndPrepareWidget(widgetName, hubName, deviceName, doubleSize);
        selectFontSize(fontSize);
        if (on) {
            testOnButton();
        } else {
            testOffButton();
        }
        sleep(4000);
        UiObject2 widget = returnCreatedWidget(widgetName);
        widget.click();
        sleep(10000);
        return widget;
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

    private void selectDevice(String name) {
        onView(allOf(withId(R.id.spinner_devices), isDisplayed())).perform(click());
        device.waitForIdle();
        DataInteraction deviceSpinner = onData(allOf(is(instanceOf(String.class)), is(name)));
        assertThat(deviceSpinner, notNullValue());
        deviceSpinner.perform(click());
        device.waitForIdle();
    }

    private void selectHub(String hubName) {
        onView(allOf(withId(R.id.spinner_hubs), isDisplayed())).perform(click());
        device.waitForIdle();
        DataInteraction deviceSpinner = onData(allOf(is(instanceOf(String.class)), is(hubName)));
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
