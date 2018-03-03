/*
 * Copyright 2018 Igor Maznitsa.
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
package com.igormaznitsa.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

public class BackendService {

    public interface PushListener {

        /**
         * Notificaton for push event
         * @param event event data as JSON object
         * @return true if push event should continue processing, false if push event shold be stopped
         */
        boolean onPushEvent(JSONObject event);

        /**
         * Notification about error in push event mechanism
         * @param thr error object
         * @return true if push event mechanism should continue its work, false if it should be stopped
         */
        boolean onPushSystemError(Throwable thr);
    }

    public interface Callback {

        void onResponseReceived(String eventId, JSONObject wholeJsonResponse, String dataFeldValue);

        void onError(String eventId, Throwable exception);
    }

    private final Timer pushCheckTimer;
    private final PushListener pushListener;

    public BackendService(final PushListener pushListener) {
        this.pushListener = pushListener;

        if (this.pushListener != null) {
            this.pushCheckTimer = new Timer() {
                @Override
                public void run() {
                    checkPushEvents();
                }
            };
            this.pushCheckTimer.scheduleRepeating(1000);
        } else {
            this.pushCheckTimer = null;
        }
    }

    private void checkPushEvents() {
        try {
            final RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, makeUrlForResource("__mailbox__"));
            rb.setHeader("Accept", "application/json");

            rb.sendRequest("", new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case 204: {
                            // there are no push events
                        }break;
                        case 200: {
                            final JSONObject jobj = (JSONObject) JSONParser.parseStrict(response.getText());
                            if (!pushListener.onPushEvent(jobj)){
                                pushCheckTimer.cancel();
                            }
                        }
                        break;
                        default: {
                            if (!pushListener.onPushSystemError(new Throwable("Not OK response : " + response.getStatusCode()))){
                                pushCheckTimer.cancel();
                            }
                        }
                        break;
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    if (!pushListener.onPushSystemError(exception)){
                        pushCheckTimer.cancel();
                    }
                }
            });
        } catch (RequestException ex) {
            if (!pushListener.onPushSystemError(ex)){
                pushCheckTimer.cancel();
            }
        }
    }

    public void doDataRequest(final String eventId, final String dataFieldValue, final Callback reqCallback) {
        try {
            final RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, makeUrlForResource(eventId));
            rb.setHeader("Content-Type", "application/json; charset=utf-8");
            rb.setHeader("Accept", "application/json");

            final JSONObject jsonObj = new JSONObject();
            jsonObj.put("Data", new JSONString(dataFieldValue));

            rb.sendRequest(jsonObj.toString(), new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case 0: {
                            reqCallback.onError(eventId, new Throwable("Server is unavaliable"));
                        }
                        break;
                        case 200: {
                            final JSONObject jobj = (JSONObject) JSONParser.parseStrict(response.getText());
                            reqCallback.onResponseReceived(eventId, jobj, ((JSONString) jobj.get("Data")).stringValue());
                        }
                        break;
                        default: {
                            reqCallback.onError(eventId, new Throwable("Unexpected response code : " + response.getStatusCode()));
                        }
                        break;
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    reqCallback.onError(eventId, exception);
                }
            });
        } catch (RequestException e) {
            reqCallback.onError(eventId, e);
        }
    }

    private static String makeUrlForResource(final String path) {
        final UrlBuilder urlBuilder = new UrlBuilder();
        urlBuilder.setHost(Window.Location.getHost());
        urlBuilder.setPath(path);

        final String port = Window.Location.getPort();
        if (!port.isEmpty()) {
            urlBuilder.setPort(Integer.parseInt(port));
        }

        return urlBuilder.buildString();
    }

}
