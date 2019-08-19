package com.cozify.cozifywidget;

import android.content.Context;
import androidx.test.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PersistentStorageTest {

    @Test
    public void saveEmail() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        PersistentStorage.getInstance().saveEmail(appContext, "1");
        assertEquals(PersistentStorage.getInstance().loadEmail(appContext), "1");
    }

    @Test
    public void saveCloudToken() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        PersistentStorage.getInstance().saveCloudToken(appContext, "1");
        assertEquals(PersistentStorage.getInstance().loadCloudToken(appContext), "1");
    }

    @Test
    public void saveHubKey() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        PersistentStorage.getInstance().saveHubKey(appContext, 2, "2");
        assertEquals(PersistentStorage.getInstance().loadHubKey(appContext, 2), "2");
    }

    @Test
    public void saveDeviceId() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        PersistentStorage.getInstance().saveDeviceId(appContext,1, "2");
        PersistentStorage.getInstance().saveDeviceId(appContext,2, "3");
        assertEquals(PersistentStorage.getInstance().loadDeviceId(appContext, 1), "2");
        assertEquals(PersistentStorage.getInstance().loadDeviceId(appContext, 2), "3");
    }

    @Test
    public void saveDeviceName() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        PersistentStorage.getInstance().saveDeviceName(appContext,1, "2");
        PersistentStorage.getInstance().saveDeviceName(appContext,2, "3");
        assertEquals(PersistentStorage.getInstance().loadDeviceName(appContext, 1), "2");
        assertEquals(PersistentStorage.getInstance().loadDeviceName(appContext, 2), "3");
    }

    @Test
    public void saveSettingsJson() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        PersistentStorage.getInstance().saveSettings(appContext,1, true, false, false, false, false, true);
        PersistentStorage.getInstance().saveSettings(appContext,2, false, true, false, false, false, true);
        JSONObject j1 = PersistentStorage.getInstance().loadSettingsJson(appContext, 1);
        JSONObject j2 = PersistentStorage.getInstance().loadSettingsJson(appContext, 2);
        try {
            assertTrue(j1.getBoolean("isOn"));
            assertFalse(j1.getBoolean("isArmed"));
            assertFalse(j2.getBoolean("isOn"));
            assertTrue(j2.getBoolean("isArmed"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail();
        }
    }
}