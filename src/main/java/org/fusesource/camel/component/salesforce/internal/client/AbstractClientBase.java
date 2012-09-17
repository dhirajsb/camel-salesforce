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
package org.fusesource.camel.component.salesforce.internal.client;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.fusesource.camel.component.salesforce.api.SalesforceException;
import org.fusesource.camel.component.salesforce.internal.SalesforceSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractClientBase {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private static final int SESSION_EXPIRED = 401;
    protected static final ContentType APPLICATION_JSON_UTF8 = ContentType.create("application/json", Consts.UTF_8);
    protected static final ContentType APPLICATION_XML_UTF8 = ContentType.create("application/xml", Consts.UTF_8);
    protected HttpClient httpClient;
    protected String version;
    protected SalesforceSession session;
    protected String accessToken;
    protected String instanceUrl;

    public AbstractClientBase(String version, SalesforceSession session, HttpClient httpClient) {
        this.version = version;
        this.session = session;
        this.httpClient = httpClient;

        // local cache
        this.accessToken = session.getAccessToken();
        this.instanceUrl = session.getInstanceUrl();
    }

    protected InputStream doHttpRequest(HttpUriRequest request) throws SalesforceException {
        HttpResponse httpResponse = null;
        try {
            // execute the request
            httpResponse = httpClient.execute(request);

            // check response for session timeout
            final StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() == SESSION_EXPIRED) {
                // use the session to get a new accessToken and try the request again
                LOG.warn("Retrying {} on session expiry: {}", request.getMethod(), statusLine.getReasonPhrase());
                accessToken = session.login(accessToken);
                instanceUrl = session.getInstanceUrl();

                setAccessToken(request);

                // reset input entity for retry
                if (request instanceof HttpEntityEnclosingRequestBase) {
                    // TODO this may not always work, need a better way to handle this
                    HttpEntityEnclosingRequestBase requestBase = (HttpEntityEnclosingRequestBase) request;
                    HttpEntity entity = requestBase.getEntity();
                    entity.getContent().reset();
                }
                httpResponse = httpClient.execute(request);
            }

            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                LOG.error(String.format("Error {%s:%s} executing {%s:%s}",
                    statusCode, statusLine.getReasonPhrase(),
                    request.getMethod(),request.getURI()));
                throw createRestException(request, httpResponse);
            } else {
                return (httpResponse.getEntity() == null) ?
                    null : httpResponse.getEntity().getContent();
            }
        } catch (IOException e) {
            request.abort();
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
            String msg = "Unexpected Error: " + e.getMessage();
            LOG.error(msg, e);
            throw new SalesforceException(msg, e);
        } catch (RuntimeException e) {
            request.abort();
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
            String msg = "Unexpected Error: " + e.getMessage();
            LOG.error(msg, e);
            throw new SalesforceException(msg, e);
        }
    }

    protected abstract void setAccessToken(HttpRequest httpRequest);

    protected abstract SalesforceException createRestException(HttpUriRequest request, HttpResponse response);

    public void setVersion(String version) {
        this.version = version;
    }
}
