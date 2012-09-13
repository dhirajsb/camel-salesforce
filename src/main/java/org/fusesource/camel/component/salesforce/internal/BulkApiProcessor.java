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

import org.apache.camel.*;
import org.apache.camel.converter.stream.StreamCacheConverter;
import org.fusesource.camel.component.salesforce.SalesforceEndpoint;
import org.fusesource.camel.component.salesforce.SalesforceEndpointConfig;
import org.fusesource.camel.component.salesforce.api.BulkApiClient;
import org.fusesource.camel.component.salesforce.api.DefaultBulkApiClient;
import org.fusesource.camel.component.salesforce.api.RestException;
import org.fusesource.camel.component.salesforce.api.dto.bulk.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.fusesource.camel.component.salesforce.SalesforceEndpointConfig.*;

public class BulkApiProcessor extends AbstractSalesforceProcessor {

    private BulkApiClient bulkClient;

    public BulkApiProcessor(SalesforceEndpoint endpoint) {
        super(endpoint);

        this.bulkClient = new DefaultBulkApiClient(
            endpointConfig.get(SalesforceEndpointConfig.API_VERSION), session, httpClient);
    }

    @Override
    public boolean process(final Exchange exchange, final AsyncCallback callback) {

        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    switch (apiName) {
                        case CREATE_JOB:
                            JobInfo jobBody = exchange.getIn().getMandatoryBody(JobInfo.class);
                            JobInfo jobInfo = bulkClient.createJob(jobBody);

                            createResponse(exchange, jobInfo);

                            break;

                        case GET_JOB:
                            jobBody = exchange.getIn().getBody(JobInfo.class);
                            String jobId;
                            if (jobBody != null) {
                                jobId = jobBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            jobInfo = bulkClient.getJob(jobId);

                            createResponse(exchange, jobInfo);

                            break;

                        case CLOSE_JOB:
                            jobBody = exchange.getIn().getBody(JobInfo.class);
                            if (jobBody != null) {
                                jobId = jobBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            jobInfo = bulkClient.closeJob(jobId);

                            createResponse(exchange, jobInfo);

                            break;

                        case ABORT_JOB:
                            jobBody = exchange.getIn().getBody(JobInfo.class);
                            if (jobBody != null) {
                                jobId = jobBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            jobInfo = bulkClient.abortJob(jobId);

                            createResponse(exchange, jobInfo);

                            break;

                        case CREATE_BATCH:
                            // since request is in the body, use headers or endpoint params
                            ContentType contentType = ContentType.fromValue(
                                getParameter(CONTENT_TYPE, exchange, IGNORE_BODY, NOT_OPTIONAL));
                            jobId = getParameter(JOB_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);

                            InputStream request = null;
                            try {
                                request = exchange.getIn().getMandatoryBody(InputStream.class);
                            } catch (CamelException e) {
                                String msg = "Error preparing batch request: " + e.getMessage();
                                LOG.error(msg, e);
                                throw new RestException(msg, e);
                            }

                            BatchInfo batchInfo = bulkClient.createBatch(request,
                                jobId, contentType);

                            createResponse(exchange, batchInfo);

                            break;

                        case GET_BATCH:
                            BatchInfo batchBody = exchange.getIn().getBody(BatchInfo.class);
                            String batchId;
                            if (batchBody != null) {
                                jobId = batchBody.getJobId();
                                batchId = batchBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                batchId = getParameter(BATCH_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            batchInfo = bulkClient.getBatch(jobId, batchId);

                            createResponse(exchange, batchInfo);

                            break;

                        case GET_ALL_BATCHES:
                            jobBody = exchange.getIn().getBody(JobInfo.class);
                            if (jobBody != null) {
                                jobId = jobBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            List<BatchInfo> batches = bulkClient.getAllBatches(jobId);

                            createResponse(exchange, batches);

                            break;

                        case GET_REQUEST:
                            batchBody = exchange.getIn().getBody(BatchInfo.class);
                            if (batchBody != null) {
                                jobId = batchBody.getJobId();
                                batchId = batchBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                batchId = getParameter(BATCH_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }

                            request = bulkClient.getRequest(jobId, batchId);

                            // read the request stream into a StreamCache temp file
                            // ensures the connection is read
                            try {
                                createResponse(exchange, StreamCacheConverter.convertToStreamCache(request, exchange));
                            } catch (IOException e) {
                                String msg = "Error retrieving batch request: " + e.getMessage();
                                LOG.error(msg, e);
                                throw new RestException(msg, e);
                            } finally {
                                // close the input stream to release the Http connection
                                try {
                                    request.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }

                            break;

                        case GET_RESULTS:
                            batchBody = exchange.getIn().getBody(BatchInfo.class);
                            if (batchBody != null) {
                                jobId = batchBody.getJobId();
                                batchId = batchBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                batchId = getParameter(BATCH_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            InputStream results = bulkClient.getResults(jobId, batchId);

                            // read the result stream into a StreamCache temp file
                            // ensures the connection is read
                            try {
                                createResponse(exchange, StreamCacheConverter.convertToStreamCache(results, exchange));
                            } catch (IOException e) {
                                String msg = "Error retrieving batch results: " + e.getMessage();
                                LOG.error(msg, e);
                                throw new RestException(msg, e);
                            } finally {
                                // close the input stream to release the Http connection
                                try {
                                    results.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }

                            break;

                        case CREATE_BATCH_QUERY:
                            jobBody = exchange.getIn().getBody(JobInfo.class);
                            String soqlQuery;
                            if (jobBody != null) {
                                jobId = jobBody.getId();
                                contentType = jobBody.getContentType();
                                // use SOQL query from header or endpoint config
                                soqlQuery = getParameter(SOBJECT_QUERY, exchange, IGNORE_BODY, NOT_OPTIONAL);
                            } else {
                                jobId = getParameter(JOB_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                contentType = ContentType.fromValue(
                                    getParameter(CONTENT_TYPE, exchange, IGNORE_BODY, NOT_OPTIONAL));
                                // reuse SOBJECT_QUERY property
                                soqlQuery = getParameter(SOBJECT_QUERY, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            batchInfo = bulkClient.createBatchQuery(jobId,
                                soqlQuery, contentType);

                            createResponse(exchange, batchInfo);

                            break;

                        case GET_QUERY_RESULT_IDS:
                            batchBody = exchange.getIn().getBody(BatchInfo.class);
                            if (batchBody != null) {
                                jobId = batchBody.getJobId();
                                batchId = batchBody.getId();
                            } else {
                                jobId = getParameter(JOB_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                batchId = getParameter(BATCH_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            List<String> resultIdList = bulkClient.getQueryResultIds(jobId, batchId);

                            createResponse(exchange, resultIdList);

                            break;

                        case GET_QUERY_RESULT:
                            batchBody = exchange.getIn().getBody(BatchInfo.class);
                            String resultId;
                            if (batchBody != null) {
                                jobId = batchBody.getJobId();
                                batchId = batchBody.getId();
                                resultId = getParameter(RESULT_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                            } else {
                                jobId = getParameter(JOB_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                batchId = getParameter(BATCH_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                resultId = getParameter(RESULT_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }
                            results = bulkClient.getQueryResult(jobId, batchId, resultId);

                            // read the result stream into a StreamCache temp file
                            // ensures the connection is read
                            try {
                                createResponse(exchange, StreamCacheConverter.convertToStreamCache(results, exchange));
                            } catch (IOException e) {
                                String msg = "Error retrieving query result: " + e.getMessage();
                                LOG.error(msg, e);
                                throw new RestException(msg, e);
                            } finally {
                                // close the input stream to release the Http connection
                                try {
                                    results.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }

                            break;
                    }

                } catch (RestException e) {
                    String msg = String.format("Error processing %s: [%s] \"%s\"",
                        apiName, e.getStatusCode(), e.getMessage());
                    LOG.error(msg, e);
                    exchange.setException(e);
                } catch (RuntimeException e) {
                    String msg = String.format("Unexpected Error processing %s: \"%s\"",
                        apiName, e.getMessage());
                    LOG.error(msg, e);
                    exchange.setException(new RestException(msg, e));
                } catch (InvalidPayloadException e) {
                    String msg = String.format("Unexpected Error processing %s: \"%s\"",
                        apiName, e.getMessage());
                    LOG.error(msg, e);
                    exchange.setException(new RestException(msg, e));
                } finally {
                    callback.done(false);
                }

            }
        });

        // continue routing asynchronously
        return false;
    }

    private void createResponse(Exchange exchange, Object body) {
        final Message out = exchange.getOut();
        out.setBody(body);

        // copy headers and attachments
        out.getHeaders().putAll(exchange.getIn().getHeaders());
        out.getAttachments().putAll(exchange.getIn().getAttachments());
    }

}
