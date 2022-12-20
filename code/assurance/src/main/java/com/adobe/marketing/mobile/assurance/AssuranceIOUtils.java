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


import android.content.Context;
import android.content.res.XmlResourceParser;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.IOException;
import java.util.Stack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class AssuranceIOUtils {
    private static final String LOG_TAG = "AssuranceIOUtils";

    private AssuranceIOUtils() {}

    /**
     * Returns the {@link JSONObject} representation of XML file @xmlFileName
     *
     * <p>It opens a {@link XmlResourceParser} for @xmlFileName. Use the opened {@code
     * XmlResourceParser} to reads XML tags and convert them to {@code JSONObject}. for ex:
     * <manifest><application name="abc"/><manifest/> will convert to {"manifest:
     * {application:{"name":"abc"}}"}
     *
     * <p>Returns an empty {@code JSONObject} if there is an {@code Exception} thrown by {@code
     * XmlResourceParser}.
     *
     * @param xmlFileName name of XML to parse.
     * @return a {@code JSONObject} mapping of AndroidManifest.xml.
     */
    static JSONObject parseXMLResourceFileToJson(final String xmlFileName) {

        XmlResourceParser xmlResParser = null;

        try {
            final Context appContext =
                    ServiceProvider.getInstance().getAppContextService().getApplicationContext();

            if (appContext == null) {
                return new JSONObject();
            }

            xmlResParser =
                    appContext
                            .createPackageContext(appContext.getPackageName(), 0)
                            .getAssets()
                            .openXmlResourceParser(xmlFileName);
            xmlResParser.next(); // Move the parser to XmlPullParser.START_DOCUMENT
            return convertXMLToJSON(xmlResParser);
        } catch (final Exception e) {
            Log.debug(
                    Assurance.LOG_TAG,
                    "Failed to create parse %s file. Error: %s",
                    xmlFileName,
                    e.getMessage());
        } finally {
            if (xmlResParser != null) {
                xmlResParser.close();
            }
        }

        return new JSONObject();
    }

    /**
     * Takes {@link XmlPullParser} as an argument and returns {@code JSONObject} mapping for XML.
     *
     * @param xmlPullParser {@code XmlPullParser}
     * @return {@code JSONObject} mapping of XML.
     * @throws IOException
     * @throws XmlPullParserException
     * @throws JSONException
     */
    static JSONObject convertXMLToJSON(final XmlPullParser xmlPullParser)
            throws IOException, XmlPullParserException, JSONException {
        final Stack<JSONObject> xmlJsonObjectStack = new Stack<>();

        while (xmlPullParser.getEventType() != XmlPullParser.END_DOCUMENT) {
            switch (xmlPullParser.getEventType()) {
                case XmlPullParser.START_DOCUMENT:
                    xmlJsonObjectStack.add(new JSONObject());
                    xmlPullParser.next();
                    break;

                case XmlPullParser.START_TAG:
                    final JSONObject jsonObject = new JSONObject();
                    int count = xmlPullParser.getAttributeCount();

                    for (int i = 0; i < count; i++) {
                        jsonObject.put(
                                xmlPullParser.getAttributeName(i),
                                xmlPullParser.getAttributeValue(i));
                    }

                    xmlJsonObjectStack.push(jsonObject);
                    xmlPullParser.next();
                    break;

                case XmlPullParser.TEXT:
                    final JSONObject jsonObj = xmlJsonObjectStack.peek();
                    final String content = xmlPullParser.getText().trim();

                    if (!StringUtils.isNullOrEmpty(content)) {
                        jsonObj.put("content", content);
                    }

                    xmlPullParser.next();
                    break;

                case XmlPullParser.END_TAG:
                    final JSONObject jsonObjChild = xmlJsonObjectStack.pop();
                    final JSONObject jsonObjectParent = xmlJsonObjectStack.peek();

                    if (jsonObjectParent.has(xmlPullParser.getName())) {
                        if (jsonObjectParent.get(xmlPullParser.getName()) instanceof JSONArray) {
                            final JSONArray arr =
                                    (JSONArray) jsonObjectParent.get(xmlPullParser.getName());
                            arr.put(jsonObjChild);
                        } else {
                            final JSONObject obj =
                                    (JSONObject) jsonObjectParent.get(xmlPullParser.getName());
                            final JSONArray jsonArray = new JSONArray();
                            jsonArray.put(obj).put(jsonObjChild);
                            jsonObjectParent.put(xmlPullParser.getName(), jsonArray);
                        }
                    } else {
                        jsonObjectParent.put(xmlPullParser.getName(), jsonObjChild);
                    }

                    xmlPullParser.next();
                    break;
            }
        }

        return xmlJsonObjectStack.pop();
    }
}
