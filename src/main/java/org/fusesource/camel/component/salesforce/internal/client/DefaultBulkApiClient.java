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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.fusesource.camel.component.salesforce.api.RestException;
import org.fusesource.camel.component.salesforce.api.dto.RestError;
import org.fusesource.camel.component.salesforce.api.dto.bulk.*;
import org.fusesource.camel.component.salesforce.api.dto.bulk.Error;
import org.fusesource.camel.component.salesforce.internal.SalesforceSession;

import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultBulkApiClient extends AbstractClientBase implements BulkApiClient {

    private static final String TOKEN_HEADER = "X-SFDC-Session";

    private JAXBContext context;
    private static final ContentType DEFAULT_ACCEPT_TYPE = ContentType.XML;
    private ObjectFactory objectFactory;
    private static final int BUFFER_SIZE = 2048;

    public DefaultBulkApiClient(String version, SalesforceSession session, HttpClient httpClient) {
        super(version, session, httpClient);

        try {
            context = JAXBContext.newInstance(JobInfo.class.getPackage().getName(), getClass().getClassLoader());
        } catch (JAXBException e) {
            String msg = "Error loading Bulk API DTOs: " + e.getMessage();
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        this.objectFactory = new ObjectFactory();
    }

    @Override
    public JobInfo createJob(JobInfo request) throws RestException {

        // clear system fields if set
        sanitizeJobRequest(request);

        final HttpPost post = new HttpPost(jobUrl(null));
        marshalRequest(objectFactory.createJobInfo(request), post, APPLICATION_XML_UTF8);

        // make the call and parse the result
        InputStream response = doHttpRequest(post);

        JobInfo value = unmarshalResponse(response, post, JobInfo.class);
        return value;
    }

    // reset read only fields
    private void sanitizeJobRequest(JobInfo request) {
        request.setApexProcessingTime(null);
        request.setApiActiveProcessingTime(null);
        request.setApiVersion(null);
        request.setCreatedById(null);
        request.setCreatedDate(null);
        request.setId(null);
        request.setNumberBatchesCompleted(null);
        request.setNumberBatchesFailed(null);
        request.setNumberBatchesInProgress(null);
        request.setNumberBatchesQueued(null);
        request.setNumberBatchesTotal(null);
        request.setNumberRecordsFailed(null);
        request.setNumberRecordsProcessed(null);
        request.setNumberRetries(null);
        request.setState(null);
        request.setSystemModstamp(null);
        request.setSystemModstamp(null);
    }

    @Override
    public JobInfo getJob(String jobId) throws RestException {

        final HttpGet get = new HttpGet(jobUrl(jobId));

        // make the call and parse the result
        InputStream response = doHttpRequest(get);

        JobInfo value = unmarshalResponse(response, get, JobInfo.class);
        return value;
    }

    @Override
    public JobInfo closeJob(String jobId) throws RestException {
        final JobInfo request = new JobInfo();
        request.setState(JobStateEnum.CLOSED);

        final HttpPost post = new HttpPost(jobUrl(jobId));
        marshalRequest(objectFactory.createJobInfo(request), post, APPLICATION_XML_UTF8);

        // make the call and parse the result
        InputStream response = doHttpRequest(post);

        JobInfo value = unmarshalResponse(response, post, JobInfo.class);
        return value;
    }

    @Override
    public JobInfo abortJob(String jobId) throws RestException {
        final JobInfo request = new JobInfo();
        request.setState(JobStateEnum.ABORTED);

        final HttpPost post = new HttpPost(jobUrl(jobId));
        marshalRequest(objectFactory.createJobInfo(request), post, APPLICATION_XML_UTF8);

        // make the call and parse the result
        InputStream response = doHttpRequest(post);

        JobInfo value = unmarshalResponse(response, post, JobInfo.class);
        return value;
    }

    @Override
    public BatchInfo createBatch(InputStream batchStream, String jobId, ContentType contentTypeEnum) throws RestException {

        final HttpPost post = new HttpPost(batchUrl(jobId, null));
        post.setEntity(new InputStreamEntity(
            batchStream, -1,
            org.apache.http.entity.ContentType.create(getContentType(contentTypeEnum), Consts.UTF_8)));

        // make the call and parse the result
        InputStream response = doHttpRequest(post);

        BatchInfo value = unmarshalResponse(response, post, BatchInfo.class);
        return value;
    }

    @Override
    public BatchInfo getBatch(String jobId, String batchId) throws RestException {

        final HttpGet get = new HttpGet(batchUrl(jobId, batchId));

        // make the call and parse the result
        InputStream response = doHttpRequest(get);

        BatchInfo value = unmarshalResponse(response, get, BatchInfo.class);
        return value;
    }

    @Override
    public List<BatchInfo> getAllBatches(String jobId) throws RestException {

        final HttpGet get = new HttpGet(batchUrl(jobId, null));

        // make the call and parse the result
        InputStream response = doHttpRequest(get);

        BatchInfoList value = unmarshalResponse(response, get, BatchInfoList.class);
        return value.getBatchInfo();
    }

    @Override
    public InputStream getRequest(String jobId, String batchId) throws RestException {

        final HttpGet get = new HttpGet(batchUrl(jobId, batchId));

        // make the call and parse the result
        InputStream response = doHttpRequest(get);

        return response;
    }

    @Override
    public InputStream getResults(String jobId, String batchId) throws RestException {
        final HttpGet get = new HttpGet(batchResultUrl(jobId, batchId, null));

        // make the call and return the result
        return doHttpRequest(get);
    }

    @Override
    public BatchInfo createBatchQuery(String jobId, String soqlQuery, ContentType jobContentType) throws RestException {

        final HttpPost post = new HttpPost(batchUrl(jobId, null));
        byte[] queryBytes = soqlQuery.getBytes(Consts.UTF_8);
        post.setEntity(new InputStreamEntity(
            new ByteArrayInputStream(queryBytes), queryBytes.length,
            org.apache.http.entity.ContentType.create(getContentType(jobContentType), Consts.UTF_8)));

        // make the call and parse the result
        InputStream response = doHttpRequest(post);

        BatchInfo value = unmarshalResponse(response, post, BatchInfo.class);
        return value;
    }

    @Override
    public List<String> getQueryResultIds(String jobId, String batchId) throws RestException {
        final HttpGet get = new HttpGet(batchResultUrl(jobId, batchId, null));

        // make the call and parse the result
        InputStream response = doHttpRequest(get);

        QueryResultList value = unmarshalResponse(response, get, QueryResultList.class);
        return Collections.unmodifiableList(value.getResult());
    }

    @Override
    public InputStream getQueryResult(String jobId, String batchId, String resultId) throws RestException {
        final HttpGet get = new HttpGet(batchResultUrl(jobId, batchId, resultId));

        // make the call and parse the result
        InputStream response = doHttpRequest(get);

        return response;
    }

    @Override
    protected void setAccessToken(HttpRequest httpRequest) {
        httpRequest.setHeader(TOKEN_HEADER, session.getAccessToken());
    }

    @Override
    protected InputStream doHttpRequest(HttpUriRequest request) throws RestException {
        // set access token for all requests
        setAccessToken(request);

        // set default charset
        request.setHeader("Accept-Charset", Consts.UTF_8.toString());

        // TODO check if this is really needed or not, since SF response content type seems fixed
        // check if the default accept content type must be used
        if (!request.containsHeader("Accept")) {
            final String contentType = getContentType(DEFAULT_ACCEPT_TYPE);
            request.setHeader("Accept", contentType);
            // request content type and charset is set by the request entity
        }

        return super.doHttpRequest(request);
    }

    private static String getContentType(ContentType type) {
        String result = null;

        switch (type) {
            case CSV:
                result = "text/csv";
                break;

            case XML:
                result = "application/xml";
                break;

            case ZIP_CSV:
            case ZIP_XML:
                result = type.toString().toLowerCase().replace('_', '/');
                break;
        }

        return result;
    }

    @Override
    protected RestException createRestException(HttpUriRequest request, HttpResponse response) {
        // this must be of type Error
        try {
            final Error error = unmarshalResponse(response.getEntity().getContent(), request, Error.class);

            final RestError restError = new RestError();
            restError.setErrorCode(error.getExceptionCode());
            restError.setMessage(error.getExceptionMessage());

            return new RestException(Arrays.asList(restError), response.getStatusLine().getStatusCode());
        } catch (RestException e) {
            String msg = "Error un-marshaling Salesforce Error: " + e.getMessage();
            LOG.error(msg, e);
            return new RestException(msg, e);
        } catch (IOException e) {
            String msg = "Error reading Salesforce Error: " + e.getMessage();
            LOG.error(msg, e);
            return new RestException(msg, e);
        }
    }

    private <T> T unmarshalResponse(InputStream response, HttpUriRequest request, Class<T> resultClass) throws RestException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<T> result = unmarshaller.unmarshal(new StreamSource(response), resultClass);
            return result.getValue();
        } catch (JAXBException e) {
            String msg = String.format("Error unmarshaling response {%s:%s} : %s",
                request.getMethod(), request.getURI(), e.getMessage());
            LOG.error(msg, e);
            throw new RestException(msg, e);
        } catch (IllegalArgumentException e) {
            String msg = String.format("Error unmarshaling response for {%s:%s} : %s",
                request.getMethod(), request.getURI(), e.getMessage());
            LOG.error(msg, e);
            throw new RestException(msg, e);
        }
    }

    private void marshalRequest(Object input, HttpEntityEnclosingRequest request, org.apache.http.entity.ContentType contentType) throws RestException {
        final RequestLine requestLine = request.getRequestLine();
        try {
            Marshaller marshaller = context.createMarshaller();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            marshaller.marshal(input, byteStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteStream.toByteArray());
            request.setEntity(new InputStreamEntity(
                inputStream, -1, contentType));
        } catch (JAXBException e) {
            String msg = String.format("Error marshaling request for {%s:%s} : %s",
                requestLine.getMethod(), requestLine.getUri(), e.getMessage());
            LOG.error(msg, e);
            throw new RestException(msg, e);
        } catch (IllegalArgumentException e) {
            String msg = String.format("Error marshaling request for {%s:%s} : %s",
                requestLine.getMethod(), requestLine.getUri(), e.getMessage());
            LOG.error(msg, e);
            throw new RestException(msg, e);
        }
    }

    private String jobUrl(String jobId) {
        if (jobId != null) {
            return super.instanceUrl + "/services/async/" + version + "/job/" + jobId;
        } else {
            return super.instanceUrl + "/services/async/" + version + "/job";
        }
    }

    private String batchUrl(String jobId, String batchId) {
        if (batchId != null) {
            return jobUrl(jobId) + "/batch/" + batchId;
        } else {
            return jobUrl(jobId) + "/batch";
        }
    }

    private String batchResultUrl(String jobId, String batchId, String resultId) {
        if (resultId != null) {
            return batchUrl(jobId, batchId) + "/result/" + resultId;
        } else {
            return batchUrl(jobId, batchId) + "/result";
        }
    }

}
