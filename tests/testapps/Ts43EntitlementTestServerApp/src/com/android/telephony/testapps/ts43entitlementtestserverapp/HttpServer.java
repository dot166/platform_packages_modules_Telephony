/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.telephony.testapps.ts43entitlementtestserverapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple HTTP server that provides adaptive responses for TS.43 entitlement testing.
 */
public class HttpServer {
    private static final String TAG = "Ts43HttpServer";
    public static final int SERVER_PORT = 5555;

    private final Ts43TestServerActivity mActivity;
    private ServerSocket mServerSocket;
    private ExecutorService mExecutorService;
    private volatile boolean mIsRunning = false;

    public HttpServer(Ts43TestServerActivity activity) {
        mActivity = activity;
    }

    /**
     * start server
     * @throws IOException
     */
    public void start() throws IOException {
        if (mIsRunning) {
            Log.d(TAG, "Server is already running.");
            return;
        }
        mExecutorService = Executors.newCachedThreadPool();
        mServerSocket = new ServerSocket(SERVER_PORT);
        mIsRunning = true;
        new Thread(this::listenForClients, "HttpServerAcceptThread").start();
        Log.d(TAG, "Server started successfully.");
    }

    /**
     * stop server
     */
    public void stop() {
        try {
            mIsRunning = false;
            if (mServerSocket != null && !mServerSocket.isClosed()) {
                mServerSocket.close();
            }
            if (mExecutorService != null) {
                mExecutorService.shutdown();
            }
            Log.d(TAG, "Server stopped.");
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket", e);
        }
    }
    public boolean isRunning() {
        return mIsRunning;
    }

    private void listenForClients() {
        while (mIsRunning) {
            try {
                Socket clientSocket = mServerSocket.accept();
                mExecutorService.submit(new ClientRequestHandler(clientSocket));
                mActivity.logToServerStatus("Client connected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                if (mIsRunning) {
                    Log.e(TAG, "Error accepting client connection", e);
                }
            }
        }
    }

    private String getEapAkaChallengeResponse() {
        // This is an actual Challenge value from b/445571916 logs.
        return "{\"eap-relay-packet\":\"a2FKeXdic29FUXlQeEMyeXp2RmM1dz09\"}";
    }

    private String getTS43ResponseForAuthToken() {
        // This is an actual token from b/445571916 logs.
        String token = "a2FKeXdic29FUXlQeEMyeXp2RmM1dz09a2FKeXdic29FUXlQeEMyeXp2RmM1dz09a2FKeXd"
                + "ic29FUXlQeEMyeXp2RmM1dz09";
        return "<?xml version=\"1.0\"?><wap-provisioningdoc version=\"1.1\">"
                + "<characteristic type=\"TOKEN\"><parm name=\"token\" value=\"" + token
                + "\"/><parm name=\"validity\" value=\"7200\"/></characteristic>"
                + "</wap-provisioningdoc>";
    }

    private String getTS43ResponseForGetPhoneNumber() {
        // This is a test MSISDN from b/445571916 logs.
        String msisdn = "+41234567890";
        return "<?xml version=\"1.0\"?><wap-provisioningdoc version=\"1.1\">"
                + "<characteristic type=\"APPLICATION\"><parm name=\"AppID\" value=\"ap2014\"/>"
                + "<parm name=\"MSISDN\" value=\"" + msisdn + "\"/></characteristic>"
                + "</wap-provisioningdoc>";
    }

    private class ClientRequestHandler implements Runnable {
        private final Socket mClientSocket;

        ClientRequestHandler(Socket socket) {
            mClientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(mClientSocket.getInputStream()));
                    PrintWriter writer = new PrintWriter(mClientSocket.getOutputStream(),
                            true)) {

                String requestLine = reader.readLine();
                Log.d(TAG, "Request Line: " + requestLine);
                mActivity.logToRequestView("Request: " + requestLine);

                boolean isPost = requestLine != null && requestLine.startsWith("POST");
                String responseBody = "";
                String contentType = "application/json; charset=utf-8";
                int responseCode = mActivity.getSelectedResponseCode();
                String statusText;
                switch (responseCode) {
                    case 200:
                        statusText = "OK";
                        break;
                    case 500:
                        statusText = "Internal Server Error";
                        break;
                    case 503:
                        statusText = "Service Unavailable";
                        break;
                    default:
                        statusText = "Unknown Status";
                        break;
                }

                if (requestLine != null && responseCode == 200) {
                    if (requestLine.contains("operation=GetPhoneNumber")) {
                        Log.d(TAG, "Detected getPhoneNumber operation.");
                        responseBody = getTS43ResponseForGetPhoneNumber();
                        contentType = "text/vnd.wap.connectivity-xml; charset=utf-8";
                    } else if (isPost) {
                        Log.d(TAG, "Detected EAP-AKA response (POST), returning final token.");
                        responseBody = getTS43ResponseForAuthToken();
                        contentType = "text/vnd.wap.connectivity-xml; charset=utf-8";
                    } else if (requestLine.contains("EAP_ID=")) {
                        Log.d(TAG, "Detected initial EAP-AKA challenge request.");
                        responseBody = getEapAkaChallengeResponse();
                        contentType = "application/vnd.gsma.eap-relay.v1.0+json; charset=utf-8";
                    }
                } else if (responseCode != 200) {
                    responseBody = "{\"error\":\"" + statusText + "\"}";
                    contentType = "application/json; charset=utf-8";
                }
                String retryAfter = null;
                if (responseCode == 503) {
                    retryAfter = mActivity.getRetryAfterValue();
                }
                String httpResponse = buildHttpResponse(responseCode, statusText, responseBody,
                        contentType, retryAfter);
                writer.print(httpResponse);
                writer.flush();
                Log.d(TAG, "Response sent to client: " + httpResponse);
            } catch (IOException e) {
                Log.e(TAG, "Error handling client", e);
            } finally {
                try {
                    mClientSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing client socket", e);
                }
            }
        }

        private String buildHttpResponse(int code, String status, String body, String contentType,
                String retryAfter) {
            StringBuilder response = new StringBuilder();
            response.append("HTTP/1.1 ").append(code).append(" ").append(status).append("\r\n");
            response.append("Content-Type: ").append(contentType).append("\r\n");
            response.append("Content-Length: ").append(body.getBytes().length).append("\r\n");
            response.append("Connection: close\r\n");

            if (code == 503 && retryAfter != null && !retryAfter.isEmpty()) {
                response.append("Retry-After: ").append(retryAfter).append("\r\n");
            }

            response.append("\r\n");
            response.append(body);
            return response.toString();
        }
    }
}

