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

import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.RESPONSE_KEY_BLOB_ID;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.RESPONSE_KEY_ERROR;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_ENDPOINT_FORMAT;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_HEADER_KEY_ACCEPT;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_HEADER_KEY_CONTENT_LENGTH;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_HEADER_KEY_CONTENT_TYPE;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_HEADER_KEY_FILE_CONTENT_TYPE;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_HTTP_METHOD;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_PATH_API;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_PATH_FILEUPLOAD;
import static com.adobe.marketing.mobile.assurance.AssuranceConstants.BlobKeys.UPLOAD_QUERY_KEY;

import android.net.Uri;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

class AssuranceBlob {
    private static final String LOG_TAG = "AssuranceBlob";

    interface BlobUploadCallback {
        void onSuccess(final String blobID);

        void onFailure(final String reason);
    }

    /**
     * Sends a binary blob of data to Project Assurance server to be recorded as an 'asset' for the
     * current session.
     *
     * <p>Posts the binary blob to Project Assurance with the given contentType. If no contentType
     * is provided, default of 'application/octet-stream' will be used. Expects server to respond
     * with a JSON object containing one of the following keys:
     *
     * <ol>
     *   blobID - NSString containing the asset ID of the newly stored asset
     * </ol>
     *
     * <ol>
     *   error - NSError representing the error that occurred
     * </ol>
     *
     * @param blobData byte array containing the data to transmit
     * @param contentType {@link String} containing the MIME type of the blob. Null, will default to
     *     application/octet-stream
     * @param contentType the active {@link AssuranceSession} to which the blob is uploaded
     * @param callback callback to be executed when the upload has completed (either successfully or
     *     with an error condition)
     */
    static void upload(
            final byte[] blobData,
            final String contentType,
            final AssuranceSession session,
            final BlobUploadCallback callback) {

        if (blobData == null) {
            uploadFailure(callback, "Sending Blob failed, blobData is null");
            return;
        }

        if (session == null) {
            uploadFailure(
                    callback, "Unable to upload blob, assurance session instance unavailable");
            uploadFailure(
                    callback, "Unable to upload blob, assurance session instance unavailable");
            return;
        }

        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final String environmentFormat =
                                            AssuranceUtil.getURLFormatForEnvironment(
                                                    session.getAssuranceEnvironment());
                                    final String sessionId = session.getSessionId();
                                    final String endpoint =
                                            String.format(
                                                    UPLOAD_ENDPOINT_FORMAT, environmentFormat);
                                    final Uri uri =
                                            new Uri.Builder()
                                                    .encodedPath(endpoint)
                                                    .appendPath(UPLOAD_PATH_API)
                                                    .appendPath(UPLOAD_PATH_FILEUPLOAD)
                                                    .appendQueryParameter(
                                                            UPLOAD_QUERY_KEY,
                                                            (sessionId != null
                                                                            && !sessionId.isEmpty())
                                                                    ? sessionId
                                                                    : "")
                                                    .build();
                                    final URL destinationURL = new URL(uri.toString());
                                    final HttpURLConnection connection =
                                            (HttpURLConnection) destinationURL.openConnection();
                                    connection.setDoInput(true);
                                    connection.setDoOutput(true);
                                    connection.setUseCaches(false);
                                    connection.setRequestMethod(UPLOAD_HTTP_METHOD);
                                    connection.setRequestProperty(
                                            UPLOAD_HEADER_KEY_CONTENT_TYPE,
                                            "application/octet-stream");
                                    connection.setRequestProperty(
                                            UPLOAD_HEADER_KEY_FILE_CONTENT_TYPE, contentType);
                                    connection.setRequestProperty(
                                            UPLOAD_HEADER_KEY_CONTENT_LENGTH, "" + blobData.length);
                                    connection.setRequestProperty(
                                            UPLOAD_HEADER_KEY_ACCEPT, "application/json");
                                    final DataOutputStream outputStream =
                                            new DataOutputStream(connection.getOutputStream());
                                    outputStream.write(blobData);

                                    // Reading the response
                                    final int responseCode = connection.getResponseCode();
                                    final String responseMessage = connection.getResponseMessage();
                                    final BufferedReader bufferedReader =
                                            new BufferedReader(
                                                    new InputStreamReader(
                                                            connection.getInputStream()));
                                    final StringBuilder response = new StringBuilder();
                                    String inputLine;

                                    while ((inputLine = bufferedReader.readLine()) != null) {
                                        response.append(inputLine);
                                    }

                                    outputStream.flush();
                                    outputStream.close();

                                    final JSONObject jsonResponse =
                                            new JSONObject(response.toString());

                                    if (jsonResponse.has(RESPONSE_KEY_ERROR)) {
                                        final String error =
                                                jsonResponse.getString(RESPONSE_KEY_ERROR);

                                        if (!error.isEmpty()) {
                                            callback.onFailure(
                                                    "Error occurred when posting blob, error - "
                                                            + error);
                                            return;
                                        }
                                    }

                                    if (jsonResponse.has(RESPONSE_KEY_BLOB_ID)) {
                                        final String value =
                                                jsonResponse.getString(RESPONSE_KEY_BLOB_ID);

                                        if (value.isEmpty()) {
                                            uploadFailure(
                                                    callback,
                                                    "Uploading Blob failed, Invalid BlobId"
                                                        + " returned from the fileStorage server");
                                            return;
                                        }

                                        uploadSuccess(callback, value);
                                    }

                                } catch (JSONException ex) {
                                    uploadFailure(
                                            callback,
                                            "Uploading Blob failed, Json exception while parsing"
                                                    + " response, Error - "
                                                    + ex);

                                } catch (final MalformedURLException ex) {
                                    uploadFailure(
                                            callback,
                                            String.format(
                                                    "Uploading Blob failed, MalformedURLException"
                                                            + " %s",
                                                    ex));
                                } catch (final IOException ex) {
                                    uploadFailure(
                                            callback,
                                            String.format(
                                                    "Uploading Blob failed, IOException %s", ex));
                                } catch (final Exception ex) {
                                    uploadFailure(
                                            callback,
                                            String.format(
                                                    "Uploading Blob failed with Exception : %s",
                                                    ex));
                                }
                            }
                        })
                .start();
    }

    // ========================================================================================
    // private methods
    // ========================================================================================

    /**
     * Helper method to handle failure during blob upload.
     *
     * <p>
     *
     * @param reason A {@link String} message representing reason for failure
     * @param callback callback to be called during upload failure
     */
    private static void uploadFailure(final BlobUploadCallback callback, final String reason) {
        Log.error(Assurance.LOG_TAG, LOG_TAG, reason);

        if (callback != null) {
            callback.onFailure(reason);
        }
    }

    /**
     * Helper method to handle success of blob upload.
     *
     * <p>
     *
     * @param blobId A {@link String} unique identity of the upladed blob
     * @param callback callback to be called during successful upload
     */
    private static void uploadSuccess(final BlobUploadCallback callback, final String blobId) {
        Log.debug(Assurance.LOG_TAG, LOG_TAG, "Blob upload successfull for id:" + blobId);

        if (callback != null) {
            callback.onSuccess(blobId);
        }
    }
}
