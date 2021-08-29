/*
 * Copyright (C) 2018 Potato Open Sauce Project
 * Copyright (C) 2021 Jyotiraditya Panda <jyotiraditya182@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.os.Handler;
import android.os.HandlerThread;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Helper functions for uploading to HasteBin (https://hastebin.com).
 */
public final class HasteBinUtils {

    private static Handler mHandler;

    /**
     * Uploads {@code content} to HasteBin
     *
     * @param content  the content to upload to HasteBin
     * @param callback the callback to call on success / failure
     */
    public static void upload(String content, UploadResultCallback callback) {
        getHandler().post(() -> {
            try {
                URL url = new URL("https://hastebin.com/documents");
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept-Charset", "UTF-8");
                urlConnection.setDoOutput(true);

                try (OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream())) {
                    writer.write(content);
                    writer.flush();
                }

                String responseBody;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),
                        StandardCharsets.UTF_8))) {
                    responseBody = reader.lines().map(String::trim).collect(Collectors.joining());
                }

                String id = new JSONObject(responseBody).getString("key");
                if (!id.isEmpty()) {
                    callback.onSuccess(String.format("https://hastebin.com/%s", id));
                } else {
                    String msg = "Failed to upload to KatBin: No id retrieved";
                    callback.onFail(msg, new Exception(msg));
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                String msg = "Failed to upload to KatBin";
                callback.onFail(msg, e);
            }
        });
    }

    private static Handler getHandler() {
        if (mHandler == null) {
            HandlerThread hasteBinThread = new HandlerThread("HasteBinThread");
            if (!hasteBinThread.isAlive()) {
                hasteBinThread.start();
            }
            mHandler = new Handler(hasteBinThread.getLooper());
        }
        return mHandler;
    }

    public interface UploadResultCallback {
        void onSuccess(String url);

        void onFail(String message, Exception e);
    }
}
