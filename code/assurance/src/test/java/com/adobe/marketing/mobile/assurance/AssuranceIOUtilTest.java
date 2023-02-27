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


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

public class AssuranceIOUtilTest {

    @Test
    public void testConvertXmlToJson() {

        final String xmlString = readResourceFile("AndroidManifest_Test.xml").replace("\n", "");

        final XmlPullParser xmlPullParser = new KXmlParser();
        final Reader reader = new StringReader(xmlString);

        try {
            xmlPullParser.setInput(reader);
            final JSONObject jsonObj = AssuranceIOUtils.convertXMLToJSON(xmlPullParser);
            Assert.assertEquals(
                    new JSONObject(readResourceFile("AndroidManifest_Test.json").replace("\n", ""))
                            .toString(),
                    jsonObj.toString());
        } catch (final Exception e) {
            Assert.fail("Exception while converting XML to JSON.");
        } finally {
            if (reader != null) {
                closeStream(reader);
            }
        }
    }

    /**
     * Helper function to read resource file as text.
     *
     * @param fileName name of resource file.
     * @return {@code String} text of resource file.
     */
    private String readResourceFile(final String fileName) {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;

        try {
            inputStream = this.getClass().getClassLoader().getResource(fileName).openStream();
            inputStreamReader = new InputStreamReader(inputStream);
            char[] chars;
            chars = new char[inputStream.available()];
            inputStreamReader.read(chars, 0, chars.length);
            return new String(chars);
        } catch (IOException e) {
            return "";
        } finally {
            closeStream(inputStream);
            closeStream(inputStreamReader);
        }
    }

    /**
     * Helper function to close streams.
     *
     * @param closeable instance of {@link Closeable}
     */
    private void closeStream(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
        }
    }
}
