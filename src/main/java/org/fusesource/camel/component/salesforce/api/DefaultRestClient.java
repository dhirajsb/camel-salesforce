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

import com.thoughtworks.xstream.XStream;
import org.apache.http.Consts;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.fusesource.camel.component.salesforce.api.dto.RestError;
import org.fusesource.camel.component.salesforce.internal.dto.RestErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class DefaultRestClient extends AbstractClientBase implements RestClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRestClient.class);
    private static final String SERVICES_DATA = "/services/data/";
    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private ObjectMapper objectMapper;
    private XStream xStream;
    protected String format;

    public DefaultRestClient(HttpClient httpClient, String version, String format, SalesforceSession session) {
        super(version, session, httpClient);

        this.format = format;

        // initialize error parsers for JSON and XML
        this.objectMapper = new ObjectMapper();
        this.xStream = new XStream();
        xStream.processAnnotations(RestErrors.class);

        // local cache
    }

    @Override
    protected InputStream doHttpRequest(HttpUriRequest request) throws RestException {
        // set standard headers for all requests
        final String contentType = ("json".equals(format) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8).toString();
        request.setHeader("Accept", contentType);
        request.setHeader("Accept-Charset", Consts.UTF_8.toString());
        // request content type and charset is set by the request entity

        return super.doHttpRequest(request);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected RestException createRestException(HttpUriRequest request, HttpResponse response) {
        StatusLine statusLine = response.getStatusLine();

        // try parsing response according to format
        try {
            if ("json".equals(format)) {
                List<RestError> restErrors = objectMapper.readValue(response.getEntity().getContent(), new TypeReference<List<RestError>>(){});
                return new RestException(restErrors, statusLine.getStatusCode());
            } else {
                RestErrors errors = new RestErrors();
                xStream.fromXML(response.getEntity().getContent(), errors);
                return new RestException(errors.getErrors(), statusLine.getStatusCode());
            }
        } catch (IOException e) {
            // log and ignore
            String msg = "Unexpected Error parsing " + format + " error response: " + e.getMessage();
            LOG.warn(msg, e);
        } catch (RuntimeException e) {
            // log and ignore
            String msg = "Unexpected Error parsing " + format + " error response: " + e.getMessage();
            LOG.warn(msg, e);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // just report HTTP status info
        return new RestException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
    }

    @Override
    public InputStream versions() throws RestException {
        HttpGet get = new HttpGet(servicesDataUrl());
        // does not require authorization token

        return doHttpRequest(get);
    }

    @Override
    public InputStream resources() throws RestException {
        HttpGet get = new HttpGet(versionUrl());
        // requires authorization token
        setAccessToken(get);

        return doHttpRequest(get);
    }

    @Override
    public InputStream globalObjects() throws RestException {
        HttpGet get = new HttpGet(sobjectsUrl(""));
        // requires authorization token
        setAccessToken(get);

        return doHttpRequest(get);
    }

    @Override
    public InputStream basicInfo(String sObjectName) throws RestException {
        HttpGet get = new HttpGet(sobjectsUrl(sObjectName + "/"));
        // requires authorization token
        setAccessToken(get);

        return doHttpRequest(get);
    }

    @Override
    public InputStream description(String sObjectName) throws RestException {
        HttpGet get = new HttpGet(sobjectsUrl(sObjectName + "/describe/"));
        // requires authorization token
        setAccessToken(get);

        return doHttpRequest(get);
    }

    @Override
    public InputStream retrieve(String sObjectName, String id, String[] fields) throws RestException {

        // parse fields if set
        String params = "";
        if (fields != null && fields.length > 0) {
            StringBuilder fieldsValue = new StringBuilder("?fields=");
            for (int i = 0; i < fields.length; i++) {
                fieldsValue.append(fields[i]);
                if (i < (fields.length - 1)) {
                    fieldsValue.append(',');
                }
            }
            params = fieldsValue.toString();
        }
        HttpGet get = new HttpGet(sobjectsUrl(sObjectName + "/" + id + params));
        // requires authorization token
        setAccessToken(get);

        return doHttpRequest(get);
    }

    @Override
    public InputStream create(String sObjectName, InputStream sObject) throws RestException {
        // post the sObject
        final HttpPost post = new HttpPost(sobjectsUrl(sObjectName));

        // authorization
        setAccessToken(post);

        // input stream as entity content
        post.setEntity(new InputStreamEntity(sObject, -1,
            "json".equals(format) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8));

        return doHttpRequest(post);
    }

    @Override
    public void update(String sObjectName, String id, InputStream sObject) throws RestException {
        final HttpPatch patch = new HttpPatch(sobjectsUrl(sObjectName + "/" + id));
        // requires authorization token
        setAccessToken(patch);

        // input stream as entity content
        patch.setEntity(new InputStreamEntity(sObject, -1,
            "json".equals(format) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8));

        doHttpRequest(patch);
    }

    @Override
    public void delete(String sObjectName, String id) throws RestException {
        final HttpDelete delete = new HttpDelete(sobjectsUrl(sObjectName + "/" + id));

        // requires authorization token
        setAccessToken(delete);

        doHttpRequest(delete);
    }

    @Override
    public InputStream retrieveWithId(String sObjectName, String fieldName, String fieldValue) throws RestException {
        final HttpGet get = new HttpGet(sobjectsExternalIdUrl(sObjectName, fieldName, fieldValue));

        // requires authorization token
        setAccessToken(get);

        return doHttpRequest(get);
    }

    @Override
    public InputStream upsert(String sObjectName, String fieldName, String fieldValue, InputStream sObject) throws RestException {
        final HttpPatch patch = new HttpPatch(sobjectsExternalIdUrl(sObjectName, fieldName, fieldValue));

        // requires authorization token
        setAccessToken(patch);

        // input stream as entity content
        patch.setEntity(new InputStreamEntity(sObject, -1,
            "json".equals(format) ? ContentType.APPLICATION_JSON : ContentType.APPLICATION_XML));

        return doHttpRequest(patch);
    }

    @Override
    public void deleteWithId(String sObjectName, String fieldName, String fieldValue) throws RestException {
        final HttpDelete delete = new HttpDelete(sobjectsExternalIdUrl(sObjectName, fieldName, fieldValue));

        // requires authorization token
        setAccessToken(delete);

        doHttpRequest(delete);
    }

    @Override
    public InputStream query(String soqlQuery) throws RestException {
        try {

            String encodedQuery = URLEncoder.encode(soqlQuery, Consts.UTF_8.toString());
            // URLEncoder likes to use '+' for spaces
            encodedQuery = encodedQuery.replace("+", "%20");
            final HttpGet get = new HttpGet(versionUrl() + "query/?q=" + encodedQuery);

            // requires authorization token
            setAccessToken(get);

            return doHttpRequest(get);

        } catch (UnsupportedEncodingException e) {
            String msg = "Unexpected error: " + e.getMessage();
            LOG.error(msg, e);
            throw new RestException(msg, e);
        }
    }

    @Override
    public InputStream queryMore(String nextRecordsUrl) throws RestException {
        final HttpGet get = new HttpGet(instanceUrl + nextRecordsUrl);

        // requires authorization token
        setAccessToken(get);

        return doHttpRequest(get);
    }

    @Override
    public InputStream search(String soslQuery) throws RestException {
        try {

            String encodedQuery = URLEncoder.encode(soslQuery, Consts.UTF_8.toString());
            // URLEncoder likes to use '+' for spaces
            encodedQuery = encodedQuery.replace("+", "%20");
            final HttpGet get = new HttpGet(versionUrl() + "search/?q=" + encodedQuery);

            // requires authorization token
            setAccessToken(get);

            return doHttpRequest(get);

        } catch (UnsupportedEncodingException e) {
            String msg = "Unexpected error: " + e.getMessage();
            LOG.error(msg, e);
            throw new RestException(msg, e);
        }
    }

    private String servicesDataUrl() {
        return instanceUrl + SERVICES_DATA;
    }

    private String versionUrl() throws RestException {
        if (version == null) {
            throw new RestException("NULL API version", new NullPointerException("version"));
        }
        return servicesDataUrl() + "v" + version + "/";
    }

    private String sobjectsUrl(String sObjectName) throws RestException {
        if (sObjectName == null) {
            throw new RestException("Null SObject name", new NullPointerException("sObjectName"));
        }
        return versionUrl() + "sobjects/" + sObjectName;
    }

    private String sobjectsExternalIdUrl(String sObjectName, String fieldName, String fieldValue) throws RestException {
        if (fieldName == null || fieldValue == null) {
            throw new RestException("External field name and value cannot be NULL",
                new NullPointerException("fieldName,fieldValue"));
        }
        try {
            String encodedValue = URLEncoder.encode(fieldValue, Consts.UTF_8.toString());
            // URLEncoder likes to use '+' for spaces
            encodedValue = encodedValue.replace("+", "%20");
            return sobjectsUrl(sObjectName + "/" + fieldName + "/" + encodedValue);
        } catch (UnsupportedEncodingException e) {
            String msg = "Unexpected error: " + e.getMessage();
            LOG.error(msg, e);
            throw new RestException(msg, e);
        }
    }

    protected void setAccessToken(HttpRequest httpRequest) {
        httpRequest.setHeader(TOKEN_HEADER, TOKEN_PREFIX + accessToken);
    }
}
