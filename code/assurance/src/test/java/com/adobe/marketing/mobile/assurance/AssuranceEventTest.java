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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class AssuranceEventTest {
    @Test
    public void basicConstructorNullPayload() {
        final AssuranceEvent e = new AssuranceEvent("testType", null);
        assertNotNull(e);
        assertNotNull(e.eventID);
        assertEquals(e.vendor, AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE);
        assertEquals(e.type, "testType");
        assertNull(e.payload);
        assertTrue(e.timestamp > 0);
        assertTrue(e.eventNumber > 0);
    }

    @Test
    public void basicConstructorNullType() {
        final Map<String, Object> payload = new HashMap<>();
        final AssuranceEvent e = new AssuranceEvent(null, payload);

        assertNotNull(e);
        assertNotNull(e.eventID);
        assertNull(e.type);
        assertEquals(e.vendor, AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE);
        assertEquals(e.payload, payload);
        assertTrue(e.timestamp > 0);
        assertTrue(e.eventNumber > 0);
    }

    @Test
    public void basicConstructorAllNull() {
        final AssuranceEvent e = new AssuranceEvent(null, null);

        assertNotNull(e);
        assertNull(e.type);
        assertNull(e.payload);
        assertEquals(AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE, e.vendor);
        assertTrue(e.timestamp > 0);
        assertTrue(e.eventNumber > 0);
    }

    @Test
    public void fullFeaturedConstructor() {
        final String typeName = "testType";
        final String pairId = "blah";
        final Map<String, Object> payload = new HashMap<>();

        final AssuranceEvent e = new AssuranceEvent(typeName, payload);

        assertEquals(AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE, e.vendor);
        assertEquals(typeName, e.type);
        assertEquals(payload, e.payload);
        assertTrue(e.timestamp > 0);
    }

    @Test
    public void eventNumberIncrements() {
        final AssuranceEvent e1 = new AssuranceEvent("testType", null);
        final AssuranceEvent e2 = new AssuranceEvent("testType", null);

        assertNotNull(e1);
        assertNotNull(e2);
        assertEquals(e1.eventNumber + 1, e2.eventNumber);
    }

    @Test(expected = JSONException.class)
    @SuppressWarnings("unused")
    public void constructionFromJSONFailsWhenNoEventID() throws JSONException {
        final String jsonEvent = "{\"vendor\":\"testVendor\", \"type\":\"testType\"}";
        final AssuranceEvent e = new AssuranceEvent(jsonEvent);
    }

    @Test(expected = JSONException.class)
    @SuppressWarnings("unused")
    public void constructionFromJSONFailsWhenNoVendor() throws JSONException {
        final String jsonEvent = "{\"eventID\":\"testEventID\", \"type\":\"testType\"}";
        final AssuranceEvent e = new AssuranceEvent(jsonEvent);
    }

    @Test(expected = JSONException.class)
    @SuppressWarnings("unused")
    public void constructionFromJSONFailsWhenNoType() throws JSONException {
        final String jsonEvent = "{\"eventID\":\"testEventID\", \"vendor\":\"testVendor\"}";
        final AssuranceEvent e = new AssuranceEvent(jsonEvent);
    }

    @Test
    public void constructFromMinimalJSON() throws JSONException {
        final String jsonEvent =
                "{\"eventID\":\"testEventID\", \"vendor\":\"testVendor\", \"type\":\"testType\"}";
        final AssuranceEvent e = new AssuranceEvent(jsonEvent);

        assertNotNull(e);

        assertNull(e.payload);

        assertEquals("testEventID", e.eventID);
        assertEquals("testVendor", e.vendor);
        assertEquals("testType", e.type);
    }

    @Test
    public void constructFromFullJSON() throws JSONException {
        final String jsonEvent =
                "{\n"
                        + "\t\"eventID\": \"2ac4f3a7-edb4-4dda-8d6f-6de0d583efb6\",\n"
                        + "\t\"vendor\": \""
                        + AssuranceConstants.VENDOR_ASSURANCE_MOBILE
                        + "\",\n"
                        + "\t\"type\": \"testType\",\n"
                        + "\t\"timestamp\": 1560205144047,\n"
                        + "\t\"pairID\": \"529e7626-71e8-4fa1-be3d-af41dea72ab5\",\n"
                        + "\t\"eventNumber\": \"5\",\n"
                        + "\t\"payload\": {\n"
                        + "\t\t\"testString\": \"this is a string\",\n"
                        + "\t\t\"testInt\": 5,\n"
                        + "\t\t\"testFloat\": 3.5,\n"
                        + "\t\t\"testBool\": false,\n"
                        + "\t\t\"testNull\": null,\n"
                        + "\t\t\"testArray\": [\n"
                        + "\t\t\t\"array item 1\",\n"
                        + "\t\t\t\"array item 2\",\n"
                        + "\t\t\t\"array item 3\"\n"
                        + "\t\t],\n"
                        + "\t\t\"testObject\": {\n"
                        + "\t\t\t\"nestedKey\": \"nestedValue\",\n"
                        + "\t\t\t\"nestedObject\": {\n"
                        + "\t\t\t\t\"doubleNestedKey\": \"doubleNestedValue\"\n"
                        + "\t\t\t},\n"
                        + "\t\t\t\"nestedArray\": [\n"
                        + "\t\t\t\t[\"doubleArray\"],\n"
                        + "\t\t\t\t\"mixed type\"\n"
                        + "\t\t\t]\n"
                        + "\t\t}\n"
                        + "\t}\n"
                        + "}";
        final AssuranceEvent e = new AssuranceEvent(jsonEvent);

        assertNotNull(e);
        assertEquals("2ac4f3a7-edb4-4dda-8d6f-6de0d583efb6", e.eventID);
        assertEquals(AssuranceTestConstants.VENDOR_ASSURANCE_MOBILE, e.vendor);
        assertEquals("testType", e.type);
        assertEquals(1560205144047L, e.timestamp);
        assertEquals(5, e.eventNumber);
        assertNotNull(e.payload);

        assertEquals("this is a string", e.payload.get("testString"));
        assertEquals(5, e.payload.get("testInt"));
        assertEquals(3.5d, e.payload.get("testFloat"));
        assertEquals(false, e.payload.get("testBool"));
        assertEquals(JSONObject.NULL, e.payload.get("testNull"));

        final List list = (List) e.payload.get("testArray");
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("array item 1", list.get(0));
        assertEquals("array item 2", list.get(1));
        assertEquals("array item 3", list.get(2));

        final Map map = (Map) e.payload.get("testObject");
        assertNotNull(map);
        assertEquals("nestedValue", map.get("nestedKey"));

        final Map nestedMap = (Map) map.get("nestedObject");
        assertNotNull(nestedMap);
        assertEquals("doubleNestedValue", nestedMap.get("doubleNestedKey"));

        final List nestedList = (List) map.get("nestedArray");
        assertNotNull(nestedList);
        assertEquals(2, nestedList.size());
        final List nestedNestedList = (List) nestedList.get(0);
        assertNotNull(nestedNestedList);
        assertEquals(1, nestedNestedList.size());
        assertEquals("doubleArray", nestedNestedList.get(0));
        assertEquals("mixed type", nestedList.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ToJSONAndBackAgain() throws JSONException {
        final Map<String, Object> payloadData = new HashMap();
        payloadData.put("testString", "testValue");
        payloadData.put("testNumber", 5);

        final AssuranceEvent originalEvent = new AssuranceEvent("testType", payloadData);
        final String jsonRepresentation = originalEvent.getJSONRepresentation();
        final AssuranceEvent copiedEvent = new AssuranceEvent(jsonRepresentation);

        assertNotNull(originalEvent);
        assertNotNull(copiedEvent);

        assertEquals(originalEvent.vendor, copiedEvent.vendor);
        assertEquals(originalEvent.type, copiedEvent.type);
        assertEquals(originalEvent.eventNumber, copiedEvent.eventNumber);
        assertEquals(originalEvent.timestamp, copiedEvent.timestamp);
        assertEquals(originalEvent.payload, copiedEvent.payload);
        assertEquals(originalEvent.eventID, copiedEvent.eventID);
    }

    @Test
    public void geControlType_when_ControlEvent() {
        // setup
        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        assertEquals(AssuranceConstants.ControlType.FAKE_EVENT, event.getControlType());
    }

    @Test
    public void geControlType_when_NonControlEvent() {
        // setup
        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload);

        // test
        assertNull(event.getControlType());
    }

    @Test
    public void geControlType_when_payloadNull() {
        // setup
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, null);

        // test
        assertNull(event.getControlType());
    }

    @Test
    public void geControlType_when_payloadDoesnotContainType() {
        // setup
        HashMap payload = new HashMap<String, Object>();
        payload.put("SomeKeytype", AssuranceConstants.ControlType.FAKE_EVENT);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        assertNull(event.getControlType());
    }

    @Test
    public void geControlType_when_payloadTypeNonString() {
        // setup
        HashMap payload = new HashMap<String, Object>();
        payload.put("type", 3);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        assertNull(event.getControlType());
    }

    @Test
    public void geControlDetail_Happy() {
        // setup
        HashMap fakeEventDetails = new HashMap<String, Object>();
        fakeEventDetails.put("eventName", "fakeEventName");

        // create payload with details
        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        payload.put("detail", fakeEventDetails);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        assertEquals(fakeEventDetails, event.getControlDetail());
    }

    @Test
    public void geControlDetail_when_NonControlEvent() {
        // setup
        HashMap fakeEventDetails = new HashMap<String, Object>();
        fakeEventDetails.put("eventName", "fakeEventName");

        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        payload.put("detail", fakeEventDetails);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload);

        // test
        assertNull(event.getControlDetail());
    }

    @Test
    public void geControlDetail_when_payloadNull() {
        // setup
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, null);

        // test
        assertNull(event.getControlDetail());
    }

    @Test
    public void geControlDetail_when_payloadDoesnotContainDetails() {
        // setup

        HashMap payload = new HashMap<String, Object>();
        payload.put("type", AssuranceConstants.ControlType.FAKE_EVENT);
        AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.CONTROL, payload);

        // test
        assertNull(event.getControlDetail());
    }

    @Test
    public void test_constructAssuranceEventWithMetadata() {
        final HashMap<String, Object> payload = new HashMap<>();
        payload.put("payloadKey1", "payloadValue1");

        final HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("metadataKey1", "metadataValue1");

        final AssuranceEvent event =
                new AssuranceEvent(
                        AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
                        AssuranceConstants.AssuranceEventType.GENERIC,
                        metadata,
                        payload,
                        System.currentTimeMillis());

        assertNotNull(event.getPayload());
        assertEquals("payloadValue1", event.getPayload().get("payloadKey1"));

        assertNotNull(event.getMetadata());
        assertEquals("metadataValue1", event.getMetadata().get("metadataKey1"));
    }

    @Test
    public void test_constructAssuranceEventWitOutMetadata() {
        final HashMap<String, Object> payload = new HashMap<>();
        payload.put("payloadKey1", "payloadValue1");

        final AssuranceEvent event =
                new AssuranceEvent(AssuranceConstants.AssuranceEventType.GENERIC, payload);

        assertNotNull(event.getPayload());
        assertEquals("payloadValue1", event.getPayload().get("payloadKey1"));

        assertNull(event.getMetadata());
    }
}
