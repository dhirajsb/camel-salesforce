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
package org.fusesource.camel.component.salesforce;

import org.apache.camel.CamelException;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultComponent;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.fusesource.camel.component.salesforce.api.RestException;
import org.fusesource.camel.component.salesforce.api.SalesforceSession;
import org.fusesource.camel.component.salesforce.api.dto.AbstractSObjectBase;
import org.fusesource.camel.component.salesforce.internal.ApiName;
import org.fusesource.camel.component.salesforce.internal.PayloadFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Represents the component that manages {@link SalesforceEndpoint}.
 */
public class SalesforceComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceComponent.class);

    public static final String DEFAULT_LOGIN_URL = "https://login.salesforce.com";
    private static final String DEFAULT_VERSION = "25.0";

    private static final int DEFAULT_MAX_PER_ROUTE = 20;
    private static final int MAX_TOTAL = 100;
    private static final int CONNECTION_TIMEOUT = 15000;
    private static final int SO_TIMEOUT = 15000;

    private String loginUrl = DEFAULT_LOGIN_URL;
    private String clientId;
    private String clientSecret;
    private String userName;
    private String password;

    private PayloadFormat payloadFormat = PayloadFormat.JSON;
    private String apiVersion = DEFAULT_VERSION;

    private HttpClient httpClient;
    private SalesforceSession session;
    private boolean loggedIn;

    private String[] packages;

    // executor for Salesforce calls
    private Executor executor;
    private Map<String, Class<?>> classMap;

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        // get API name from remaining URI
        ApiName apiName = null;
        try {
            LOG.debug("Creating endpoint for ", remaining);
            apiName = ApiName.fromValue(remaining);
        } catch (IllegalArgumentException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new RuntimeCamelException(ex.getMessage(), ex);
        }

        final SalesforceEndpoint endpoint = new SalesforceEndpoint(uri, this, apiName);

        // parse and set API name from uri
        final SalesforceEndpointConfig endpointConfig = new SalesforceEndpointConfig(getCamelContext(), uri);

        // inherit default values from component
        endpointConfig.setFormat(payloadFormat.toString());
        endpointConfig.setApiVersion(apiVersion);

        // map endpoint parameters to endpointConfig
        setProperties(endpointConfig, parameters);

        // map remaining parameters to endpoint (specifically, synchronous)
        setProperties(endpoint, parameters);

        // set endpointConfig on endpoint
        endpoint.setEndpointConfiguration(endpointConfig);
        return endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // create an Apache 4.0 HttpClient if not already set
        if (null == httpClient) {
            PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
            connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
            connectionManager.setMaxTotal(MAX_TOTAL);
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT);
            httpClient = new DefaultHttpClient(connectionManager, params);
        }

        // support restarts
        if (null == this.session) {
            this.session = new SalesforceSession(httpClient, loginUrl,
                clientId, clientSecret, userName, password);
        }
        try {
            // get a new token
            session.login(session.getAccessToken());
            loggedIn = true;
        } catch (RestException e) {
            LOG.error(e.getMessage(), e);
            throw new CamelException(e.getMessage(), e);
        }

        if (packages != null && packages.length > 0) {
            // parse the packages to create SObject name to class map
            classMap = parsePackages();
        } else {
            // use an empty map to avoid NPEs later
            LOG.warn("Missing property packages, getSObject* operations will NOT work");
            classMap = Collections.unmodifiableMap(new HashMap<String, Class<?>>());
        }
    }

    private Map<String, Class<?>> parsePackages() {
        Map<String, Class<?>> result = new HashMap<String, Class<?>>();
        Set<Class<?>> classes = getCamelContext().getPackageScanClassResolver().findImplementations(AbstractSObjectBase.class, packages);
        for (Class<?> aClass : classes) {
            // findImplementations also returns AbstractSObjectBase for some reason!!!
            if (AbstractSObjectBase.class != aClass) {
                result.put(aClass.getSimpleName(), aClass);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        try {
            if (loggedIn) {
                // logout of Salesforce
                session.logout();
            }
        } finally {
            // get a new session next time
            session = null;

            // shutdown http client connections
            httpClient.getConnectionManager().shutdown();
        }
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFormat(String format) {
        this.payloadFormat = PayloadFormat.valueOf(format.toUpperCase());
    }

    public PayloadFormat getPayloadFormat() {
        return payloadFormat;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        if (null == httpClient) {
            String msg = "Null httpClient";
            LOG.error(msg);
            throw new NullPointerException(msg);
        }
        this.httpClient = httpClient;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public SalesforceSession getSession() {
        return session;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public String[] getPackages() {
        return packages;
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

    public Map<String, Class<?>> getClassMap() {
        return classMap;
    }
}
