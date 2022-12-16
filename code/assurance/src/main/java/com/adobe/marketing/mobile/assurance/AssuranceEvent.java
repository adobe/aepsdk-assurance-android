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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Event object used to transport data to/from a Assurance server.
 *
 * <p>This object is intentionally opaque. If this needs to be public, refactor this class to
 * reflect a builder pattern enforcing size limits on constituents of the AssuranceEvent like
 * metadata.
 */
@SuppressWarnings("unused")
final class AssuranceEvent {

    final String eventID;
    final String vendor;
    final String type;
    final Map<String, Object> metadata;
    final Map<String, Object> payload;
    final long timestamp;
    final int eventNumber;

    private static final AtomicInteger ASSURANCE_EVENT_SEQUENCE_COUNTER = new AtomicInteger(0);

    /**
     * Creates a new {@link AssuranceEvent}.
     *
     * <p>Vendor value for this created {@code AssuranceEvent} defaults to {@link
     * AssuranceConstants#VENDOR_ASSURANCE_MOBILE}
     *
     * @param type {@code String} containing the event type
     * @param payload {@code Map<String, Object>} containing the event payload
     */
    AssuranceEvent(final String type, final Map<String, Object> payload) {
        this(
                AssuranceConstants.VENDOR_ASSURANCE_MOBILE,
                type,
                null,
                payload,
                System.currentTimeMillis());
    }

    /**
     * Creates a new {@link AssuranceEvent}
     *
     * <p>
     *
     * @param vendor {@code String} containing the vendor-specific identifier
     * @param type {@code String} containing the vent type
     * @param metadata {@code Map<String, Object} containing metadata about payload (if any).
     * @param payload {@code Map<String, Object>} containing the event payload
     * @param timestamp timestamp of the event in milliseconds
     */
    AssuranceEvent(
            final String vendor,
            final String type,
            final Map<String, Object> metadata,
            final Map<String, Object> payload,
            final long timestamp) {
        this(
                UUID.randomUUID().toString(),
                vendor,
                type,
                metadata,
                payload,
                timestamp,
                ASSURANCE_EVENT_SEQUENCE_COUNTER.addAndGet(1));
    }

    /**
     * Creates a new Event object.
     *
     * <p>Intended for internal use only
     *
     * @param eventID {@code String} UUID representing the event.
     * @param vendor {@code String} containing the vendor-specific identifier
     * @param type {@code String} containing the vent type
     * @param metadata {@code Map<String, Object} containing metadata about payload (if any).
     * @param payload {@code Map<String, Object>} containing the event payload
     * @param timestamp timestamp of the event in milliseconds
     * @param eventNumber sequence number of the AssuranceEvent
     */
    private AssuranceEvent(
            final String eventID,
            final String vendor,
            final String type,
            final Map<String, Object> metadata,
            final Map<String, Object> payload,
            final long timestamp,
            final int eventNumber) {
        this.eventID = eventID;
        this.vendor = vendor;
        this.type = type;
        this.metadata = metadata;
        this.payload = payload;
        this.timestamp = timestamp;
        this.eventNumber = eventNumber;
    }

    /**
     * Creates a new Event object from a JSON representation.
     *
     * <p>Intended for internal use only
     *
     * @param json {@code String} containing JSON representation of an Event
     * @throws JSONException When issues arise parsing the JSON input
     */
    AssuranceEvent(final String json) throws JSONException {
        final JSONObject eventMap = new JSONObject(json);

        this.eventID = eventMap.getString(AssuranceConstants.AssuranceEventKeys.EVENT_ID);
        this.vendor = eventMap.getString(AssuranceConstants.AssuranceEventKeys.VENDOR);
        this.type = eventMap.getString(AssuranceConstants.AssuranceEventKeys.TYPE);

        final JSONObject metadataObj =
                eventMap.optJSONObject(AssuranceConstants.AssuranceEventKeys.METADATA);

        if (metadataObj != null) {
            this.metadata = objToMap(metadataObj);
        } else {
            this.metadata = null;
        }

        final JSONObject payloadObj =
                eventMap.optJSONObject(AssuranceConstants.AssuranceEventKeys.PAYLOAD);

        if (payloadObj != null) {
            this.payload = objToMap(payloadObj);
        } else {
            this.payload = null;
        }

        this.timestamp =
                eventMap.optLong(
                        AssuranceConstants.AssuranceEventKeys.TIMESTAMP,
                        System.currentTimeMillis());
        this.eventNumber =
                eventMap.optInt(
                        AssuranceConstants.AssuranceEventKeys.EVENT_NUMBER,
                        ASSURANCE_EVENT_SEQUENCE_COUNTER.addAndGet(1));
    }

    /**
     * Gets the JSON representation of an Event object.
     *
     * @return String containing the JSON representation of the Event object.
     */
    String getJSONRepresentation() {
        final Map<String, Object> eventMap = new HashMap<>();
        eventMap.put(AssuranceConstants.AssuranceEventKeys.EVENT_ID, eventID);
        eventMap.put(AssuranceConstants.AssuranceEventKeys.VENDOR, vendor);
        eventMap.put(AssuranceConstants.AssuranceEventKeys.TYPE, type);
        eventMap.put(AssuranceConstants.AssuranceEventKeys.TIMESTAMP, timestamp);
        eventMap.put(AssuranceConstants.AssuranceEventKeys.EVENT_NUMBER, eventNumber);

        if (metadata != null) {
            eventMap.put(AssuranceConstants.AssuranceEventKeys.METADATA, metadata);
        }

        if (payload != null) {
            eventMap.put(AssuranceConstants.AssuranceEventKeys.PAYLOAD, payload);
        }

        final JSONObject jsonObj = new JSONObject(eventMap);
        return jsonObj.toString();
    }

    /**
     * Returns the type of the Control Event. Applicable only for Control Events. This method
     * returns null for all other {@link AssuranceEvent} types.
     *
     * <p>Returns null if the event is not a control event. Returns null if the payload doesnot
     * contain "type" key. Returns null if the payload "type" key contains non string data.
     * Following are the available control events to the SDK.
     *
     * <ul>
     *   <li>startEventForwarding
     *   <li>screenshot
     *   <li>logForwarding
     *   <li>fakeEvent
     *   <li>configUpdate
     * </ul>
     *
     * @return a {@link String} value representing the control type
     */
    String getControlType() {
        // return null if the event is not of type "control"
        if (!AssuranceConstants.AssuranceEventType.CONTROL.equals(type)) {
            return null;
        }

        // return null if the payload is null or does not contain "type" key
        if (payload == null
                || payload.isEmpty()
                || !payload.containsKey(AssuranceConstants.PayloadDataKeys.TYPE)) {
            return null;
        }

        // if the type is not String, return null
        if (!(payload.get(AssuranceConstants.PayloadDataKeys.TYPE) instanceof String)) {
            return null;
        }

        return (String) payload.get(AssuranceConstants.PayloadDataKeys.TYPE);
    }

    /**
     * Returns the details of the Control Event. Applicable only for Control Events. This method
     * returns null for all other {@link AssuranceEvent} types.
     *
     * <p>Returns null if the event is not a control event. Returns null if the payload does not
     * contain "type" key. Returns null if the payload "type" key contains non map data.
     *
     * @return a {@link HashMap} value representing the control details
     */
    HashMap<String, Object> getControlDetail() {
        // return null, if the event is not of type "control"
        if (!AssuranceConstants.AssuranceEventType.CONTROL.equals(type)) {
            return null;
        }

        // return null, if the payload is null or doesnot contain "detail" key
        if (payload == null
                || payload.isEmpty()
                || !payload.containsKey(AssuranceConstants.PayloadDataKeys.DETAIL)) {
            return null;
        }

        // if the detail is not HashMap, return null
        if (!(payload.get(AssuranceConstants.PayloadDataKeys.DETAIL) instanceof HashMap)) {
            return null;
        }

        return (HashMap<String, Object>) payload.get(AssuranceConstants.PayloadDataKeys.DETAIL);
    }

    /**
     * Returns the vendor of this event.
     *
     * @return the vendor of this event.
     */
    String getVendor() {
        return this.vendor;
    }

    /**
     * Returns the metadata associated with this event.
     *
     * @return metadata map associated with this event.
     */
    Map<String, Object> getMetadata() {
        return this.metadata;
    }

    /**
     * Returns the payload associated with this event.
     *
     * @return payload map associated with this event.
     */
    Map<String, Object> getPayload() {
        return this.payload;
    }

    /**
     * Converts a JSONObject into a {@code Map<String, Object>}
     *
     * <p>If JSONArray values are encountered in the input object, {@code arrayToList} will be
     * invoked to convert those objects prior to adding them to the output list.
     *
     * @param jsonObj JSONObject to be converted
     * @return A Map containing the contents of the JSONObject converted into Java types
     * @throws JSONException If errors parsing the JSON values are encountered
     */
    private Map<String, Object> objToMap(final JSONObject jsonObj) throws JSONException {
        final Map<String, Object> map = new HashMap<>();
        final Iterator<String> keys = jsonObj.keys();

        while (keys.hasNext()) {
            final String key = keys.next();
            final Object value = jsonObj.get(key);

            if (value instanceof JSONArray) {
                map.put(key, arrayToList((JSONArray) value));
            } else if (value instanceof JSONObject) {
                map.put(key, objToMap((JSONObject) value));
            } else {
                map.put(key, value);
            }
        }

        return map;
    }

    /**
     * Converts a JSONArray into an ordered List.
     *
     * <p>If JSONObjects are encountered in the input array, {@code objToMap} will be invoked to
     * convert those objects prior to adding them to the output list.
     *
     * @param jsonArr JSONArray to be converted
     * @return A List object containing the contents of the JSONArray
     * @throws JSONException If errors parsing JSON values are encountered
     */
    private List<Object> arrayToList(final JSONArray jsonArr) throws JSONException {
        final List<Object> list = new ArrayList<>();

        for (int i = 0; i < jsonArr.length(); i++) {
            final Object value = jsonArr.get(i);

            if (value instanceof JSONObject) {
                list.add(objToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(arrayToList((JSONArray) value));
            } else {
                list.add(value);
            }
        }

        return list;
    }
}
