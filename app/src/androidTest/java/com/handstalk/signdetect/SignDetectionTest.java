package com.handstalk.signdetect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.handstalk.signdetect.translate.SignDetectClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class SignDetectionTest {
    Context instrumentationContext;
    @Before
    public void setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testShapeCorrectness_1(){
        SignDetectClient signDetectClient = SignDetectClient.setTestMode(instrumentationContext, 1);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        signDetectClient.Detect();
        System.out.println(signDetectClient.getPredictedLabel());
        assertTrue("before".equals(signDetectClient.getPredictedLabel()));
    }

    @Test
    public void testShapeCorrectness_2(){
        SignDetectClient signDetectClient = SignDetectClient.setTestMode(instrumentationContext, 2);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        signDetectClient.Detect();
        System.out.println(signDetectClient.getPredictedLabel());
        assertTrue("before".equals(signDetectClient.getPredictedLabel()));
    }
}
