/*
 * Copyright 2022 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.assurance.AssuranceConnectionStatusUI;
import com.adobe.marketing.mobile.assurance.AssuranceConstants;
import com.adobe.marketing.mobile.assurance.AssuranceFullScreenTakeover;
import com.adobe.marketing.mobile.assurance.AssuranceSessionOrchestrator;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@PowerMockIgnore({"javax.xml.*", "org.robolectric.*", "android.*"})
public class AssuranceConnectionStatusUITest {

	private AssuranceConnectionStatusUI connectionStatusUI;

	private Activity mockActivity;
	private AssuranceFullScreenTakeover mockStatusTakeOver;
	@Mock
	private AssuranceSessionOrchestrator.ApplicationHandle mockApplicationHandle;
	@Mock
	private AssuranceSessionOrchestrator.SessionUIOperationHandler mockSessionUIOperationHandler;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		mockActivity = Mockito.mock(Activity.class);
		mockStatusTakeOver = Mockito.mock(AssuranceFullScreenTakeover.class);

		when(mockApplicationHandle.getCurrentActivity()).thenReturn(mockActivity);
		connectionStatusUI = new AssuranceConnectionStatusUI(mockSessionUIOperationHandler, mockApplicationHandle);
	}

	@Test
	public void test_AddUILog() {
		// prepare
		mockInternalVariables();
		final ArgumentCaptor<String> javascriptCaptor = ArgumentCaptor.forClass(String.class);


		// test
		connectionStatusUI.addUILog(AssuranceConstants.UILogColorVisibility.LOW, "Message1");
		connectionStatusUI.addUILog(AssuranceConstants.UILogColorVisibility.NORMAL, "Message2");
		connectionStatusUI.addUILog(AssuranceConstants.UILogColorVisibility.HIGH, "Message3");

		// verify
		verify(mockStatusTakeOver, times(3)).runJavascript(javascriptCaptor.capture());
		assertEquals("addLog(0, \"Message1\");", javascriptCaptor.getAllValues().get(0));
		assertEquals("addLog(1, \"Message2\");", javascriptCaptor.getAllValues().get(1));
		assertEquals("addLog(2, \"Message3\");", javascriptCaptor.getAllValues().get(2));
	}


	@Test
	public void test_onURLTriggered_Disconnect() {
		// prepare
		mockInternalVariables();

		// test
		connectionStatusUI.onURLTriggered("adbinapp://disconnect");

		// verify
		verify(mockStatusTakeOver, times(1)).remove();
		verify(mockSessionUIOperationHandler, times(1)).onDisconnect();
	}

	@Test
	public void test_onURLTriggered_Cancel() {
		// prepare
		mockInternalVariables();

		// test
		connectionStatusUI.onURLTriggered("adbinapp://cancel");

		// verify
		verify(mockStatusTakeOver, times(1)).remove();
		verify(mockSessionUIOperationHandler, times(0)).onDisconnect();
	}

	@Test
	public void test_onURLTriggered_Invalid() {
		// prepare
		mockInternalVariables();

		// test
		connectionStatusUI.onURLTriggered("invalid:URL");

		// verify
		verify(mockStatusTakeOver, times(0)).remove();
		verify(mockSessionUIOperationHandler, times(0)).onDisconnect();
	}

	@Test
	public void test_show() {
		// prepare
		mockInternalVariables();

		// test
		connectionStatusUI.show();

		// verify
		verify(mockStatusTakeOver, times(1)).show(mockActivity);
	}


	@Test
	public void test_clearLog() {
		// prepare
		mockInternalVariables();

		// test
		connectionStatusUI.clearLogs();

		// verify
		verify(mockStatusTakeOver, times(1)).runJavascript("clearLog()");
	}

	@Test
	public void test_NoOp_OnShowAndDismissDoesNotCrash() {
		// prepare
		mockInternalVariables();

		// test
		connectionStatusUI.onDismiss(null);
		connectionStatusUI.onShow(null);

		// verify
		verify(mockStatusTakeOver, times(0)).remove();
	}


	private void mockInternalVariables() {
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Whitebox.setInternalState(connectionStatusUI, "statusTakeover", mockStatusTakeOver);
	}
}
