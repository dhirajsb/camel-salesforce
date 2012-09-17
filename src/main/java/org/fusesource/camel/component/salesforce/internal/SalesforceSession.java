/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.camel.component.salesforce.internal;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.camel.component.salesforce.api.SalesforceException;
import org.fusesource.camel.component.salesforce.api.dto.RestError;
import org.fusesource.camel.component.salesforce.internal.dto.LoginError;
import org.fusesource.camel.component.salesforce.internal.dto.LoginToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SalesforceSession {

    private static final String OAUTH2_REVOKE_PATH = "/services/oauth2/revoke?token=";
    private static final String OAUTH2_TOKEN_PATH = "/services/oauth2/token";

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceSession.class);
    private static final ContentType FORM_CONTENT_TYPE = ContentType.create("application/x-www-form-urlencoded", Consts.UTF_8);

    private final HttpClient httpClient;

    private final String loginUrl;
    private final String clientId;
    private final String clientSecret;
    private final String userName;
    private final String password;

    private final ObjectMapper objectMapper;

    private String accessToken;
    private String instanceUrl;

    public SalesforceSession(HttpClient httpClient,
                             String loginUrl,
                             String clientId, String clientSecret, String userName, String password) {
        // validate parameters
        assertNotNull("Null httpClient", httpClient);
        assertNotNull("Null loginUrl", loginUrl);
        assertNotNull("Null clientId", clientId);
        assertNotNull("Null clientSecret", clientSecret);
        assertNotNull("Null userName", userName);
        assertNotNull("Null password", password);

        this.httpClient = httpClient;
        // strip trailing '/'
        this.loginUrl = loginUrl.endsWith("/") ? loginUrl.substring(0, loginUrl.length() - 1) : loginUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userName = userName;
        this.password = password;

        this.objectMapper = new ObjectMapper();
    }

    private void assertNotNull(String s, Object o) {
        if (o == null) {
            throw new IllegalArgumentException(s);
        }
    }

    public synchronized String login(String oldToken) throws SalesforceException {

        // check if we need a new session
        // this way there's always a single valid session
        if ((accessToken == null) || accessToken.equals(oldToken)) {

            // try revoking the old access token before creating a new one
            accessToken = oldToken;
            if (accessToken != null) {
                try {
                    logout();
                } catch (SalesforceException e) {
                    LOG.warn("Error revoking old access token: " + e.getMessage(), e);
                }
                accessToken = null;
            }

            // login to Salesforce and get session id
            HttpPost post = new HttpPost(loginUrl + OAUTH2_TOKEN_PATH);
            post.setHeader("Content-Type", FORM_CONTENT_TYPE.toString());

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();

            nvps.add(new BasicNameValuePair("grant_type", "password"));
            nvps.add(new BasicNameValuePair("client_id", clientId));
            nvps.add(new BasicNameValuePair("client_secret", clientSecret));
            nvps.add(new BasicNameValuePair("username", userName));
            nvps.add(new BasicNameValuePair("password", password));
            nvps.add(new BasicNameValuePair("format", "json"));
            post.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

            HttpEntity httpEntity = null;
            try {
                final HttpResponse response = httpClient.execute(post);
                httpEntity = response.getEntity();

                final StatusLine statusLine = response.getStatusLine();
                switch (statusLine.getStatusCode()) {

                    case 200:
                        // parse the response to get token
                        LoginToken token = objectMapper.readValue(httpEntity.getContent(), LoginToken.class);

                        accessToken = token.getAccessToken();
                        instanceUrl = token.getInstanceUrl();

                        break;

                    case 400:
                        // parse the response to get error
                        LoginError error = objectMapper.readValue(httpEntity.getContent(), LoginError.class);
                        String msg = String.format("Login error code:[%s] description:[%s]", error.getError(),
                            error.getErrorDescription());
                        LOG.error(msg);
                        List<RestError> errors = new ArrayList<RestError>();
                        errors.add(new RestError(error.getError(), error.getErrorDescription()));
                        throw new SalesforceException(errors, 400);

                    default:
                        String msg2 = String.format("Login error status:[%s] reason:[%s]", statusLine.getStatusCode(),
                            statusLine.getReasonPhrase());
                        LOG.error(msg2);
                        throw new SalesforceException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
                }
            } catch (IOException e) {
                String msg = "Login error: Unknown exception " + e.getMessage();
                LOG.error(msg, e);
                throw new SalesforceException(msg, e);
            } finally {
                // make sure entity is consumed
                EntityUtils.consumeQuietly(httpEntity);
            }
        }

        return accessToken;
    }

    public void logout() throws SalesforceException {
        if (accessToken == null) {
            return;
        }

        HttpGet get = new HttpGet(loginUrl + OAUTH2_REVOKE_PATH + accessToken);
        HttpEntity httpEntity = null;
        try {
            final HttpResponse response = httpClient.execute(get);
            httpEntity = response.getEntity();

            final StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case 200:
                    LOG.info("Logout successful");
                    break;
                case 400:
                    String msg = "Logout error: " + statusLine.getReasonPhrase();
                    LOG.error(msg);
                    throw new SalesforceException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
                default:
                    String msg2 = "Logout error code: " + statusLine.getStatusCode() + " reason: " + statusLine.getReasonPhrase();
                    LOG.error(msg2);
                    throw new SalesforceException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
            }

        } catch (IOException e) {
            String msg = "Logout error: " + e.getMessage();
            LOG.error(msg, e);
            throw new SalesforceException(msg, e);
        } finally {
            EntityUtils.consumeQuietly(httpEntity);
            // reset session
            accessToken = null;
            instanceUrl = null;
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public String getUserName() {
        return userName;
    }
}
