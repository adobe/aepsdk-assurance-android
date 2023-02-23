/*
 * Copyright 2023 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;

/** Utility class for tests. */
class AssuranceTestUtils {

    /**
     * Sets a private field using reflection. Used for setting states of classes where dependency
     * injection is not trivial.
     *
     * @param classToSet the class whose member should be set
     * @param name the name of the member that should be set
     * @param value the value of the memberto be set
     */
    static void setInternalState(Object classToSet, String name, Object value) {
        try {
            final Field privateField = classToSet.getClass().getDeclaredField(name);
            privateField.setAccessible(true);
            privateField.set(classToSet, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(String.format("Failed to set %s.%s", classToSet, name));
        }
    }
}
