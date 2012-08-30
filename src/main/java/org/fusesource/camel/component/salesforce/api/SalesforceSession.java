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
package org.fusesource.camel.component.salesforce.api;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.camel.component.salesforce.api.dto.RestError;
import org.fusesource.camel.component.salesforce.internal.LoginError;
import org.fusesource.camel.component.salesforce.internal.LoginToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SalesforceSession {

    private static final String OAUTH2_REVOKE_URL = "https://login.salesforce.com/services/oauth2/revoke?token=";
    private static final String OAUTH2_TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceSession.class);
    private static final ContentType FORM_CONTENT_TYPE = ContentType.create("application/x-www-form-urlencoded", Consts.UTF_8);

    private HttpClient httpClient;

    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;

    private ObjectMapper objectMapper;

    private String accessToken;
    private String instanceUrl;

    public SalesforceSession(HttpClient httpClient, String clientId, String clientSecret, String userName, String password) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userName = userName;
        this.password = password;

        this.objectMapper = new ObjectMapper();
    }

    public synchronized String login(String oldToken) throws RestException {

        // check if we need a new session
        // this way there's always a single valid session
        if ((accessToken == null) || accessToken.equals(oldToken)) {

            // try revoking the old access token before creating a new one
            accessToken = oldToken;
            if (accessToken != null) {
                try {
                    logout();
                } catch (RestException e) {
                    LOG.warn("Error revoking old access token: " + e.getMessage(), e);
                }
                accessToken = null;
            }

            // login to Salesforce and get session id
            HttpPost post = new HttpPost(OAUTH2_TOKEN_URL);
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
                        throw new RestException(errors, 400);

                    default:
                        String msg2 = String.format("Login error status:[%s] reason:[%s]", statusLine.getStatusCode(),
                            statusLine.getReasonPhrase());
                        LOG.error(msg2);
                        throw new RestException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
                }
            } catch (IOException e) {
                String msg = "Login error: Unknown exception " + e.getMessage();
                LOG.error(msg, e);
                throw new RestException(msg, e);
            } finally {
                // make sure entity is consumed
                EntityUtils.consumeQuietly(httpEntity);
            }
        }

        return accessToken;
    }

    public void logout() throws RestException {
        if (accessToken == null) {
            return;
        }

        HttpGet get = new HttpGet(OAUTH2_REVOKE_URL + accessToken);
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
                    throw new RestException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
                default:
                    String msg2 = "Logout error code: " + statusLine.getStatusCode() + " reason: " + statusLine.getReasonPhrase();
                    LOG.error(msg2);
                    throw new RestException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
            }

        } catch (IOException e) {
            String msg = "Logout error: " + e.getMessage();
            LOG.error(msg, e);
            throw new RestException(msg, e);
        } finally {
            EntityUtils.consumeQuietly(httpEntity);
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

}
