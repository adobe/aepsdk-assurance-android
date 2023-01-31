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


import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages registration of plugins and notifications to plugins about the {@link AssuranceSession}.
 */
class AssurancePluginManager {
    private static final String LOG_TAG = "AssurancePluginManager";

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<AssurancePlugin>> plugins;
    private final AssuranceSession session;

    AssurancePluginManager(final AssuranceSession session) {
        this(session, new ConcurrentHashMap<String, ConcurrentLinkedQueue<AssurancePlugin>>());
    }

    AssurancePluginManager(
            final AssuranceSession session,
            final ConcurrentHashMap<String, ConcurrentLinkedQueue<AssurancePlugin>> plugins) {
        this.session = session;
        this.plugins = plugins;
    }

    /**
     * Registers a plugin to be able to notify about session activity.
     *
     * @param plugin the plugin to be registered.
     */
    void addPlugin(final AssurancePlugin plugin) {
        if (plugin == null) {
            return;
        }

        final String vendorID = plugin.getVendor();
        final ConcurrentLinkedQueue<AssurancePlugin> newVendorQueue = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<AssurancePlugin> existingQueue =
                plugins.putIfAbsent(vendorID, newVendorQueue);

        if (existingQueue == null) {
            newVendorQueue.add(plugin);
        } else {
            existingQueue.add(plugin);
        }

        plugin.onRegistered(session);
    }

    /**
     * Notifies registered plugins about {@link AssuranceEvent}'s received by the {@link
     * AssuranceSession}
     *
     * @param event the {@link AssuranceEvent}'s received by {@link AssuranceSession}
     */
    void onAssuranceEvent(final AssuranceEvent event) {
        final ConcurrentLinkedQueue<AssurancePlugin> pluginsForVendor =
                plugins.get(event.getVendor());

        if (pluginsForVendor == null) {
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    String.format(
                            "There are no plugins registered to handle incoming"
                                    + " Assurance event with vendor : %s",
                            event.getVendor()));
            return;
        }

        for (final AssurancePlugin plugin : pluginsForVendor) {

            final String pluginControlType = plugin.getControlType();

            // refrain from calling `onEventReceived` on a plugin if plugin's controlType is empty
            // or #AssuranceConstants.ControlType.NONE
            if (pluginControlType == null
                    || pluginControlType.isEmpty()
                    || pluginControlType.equals(AssuranceConstants.ControlType.NONE)) {
                continue;
            }

            // call `onEventReceived` on a plugin if
            // 1. the event's controlType matches the plugin's controlType
            // 2. the event's controlType is a wildCard
            if (pluginControlType.equals(AssuranceConstants.ControlType.WILDCARD)
                    || pluginControlType.equals(event.getControlType())) {
                plugin.onEventReceived(event);
            }
        }
    }

    /** Notifies registered plugins about successful {@link AssuranceSession} connection. */
    void onSessionConnected() {
        for (final ConcurrentLinkedQueue<AssurancePlugin> pluginQueue : plugins.values()) {
            for (AssurancePlugin plugin : pluginQueue) {
                plugin.onSessionConnected();
            }
        }
    }

    /** Notifies registered plugins about {@link AssuranceSession} termination. */
    void onSessionTerminated() {
        for (final ConcurrentLinkedQueue<AssurancePlugin> pluginQueue : plugins.values()) {
            for (final AssurancePlugin plugin : pluginQueue) {
                plugin.onSessionTerminated();
            }
        }
    }

    /**
     * Notifies registered plugins about {@link AssuranceSession} disconnection.
     *
     * @param closeCode reason for the session disconnection.
     */
    void onSessionDisconnected(final int closeCode) {
        for (final ConcurrentLinkedQueue<AssurancePlugin> pluginQueue : plugins.values()) {
            for (AssurancePlugin plugin : pluginQueue) {
                plugin.onSessionDisconnected(closeCode);
            }
        }
    }
}
