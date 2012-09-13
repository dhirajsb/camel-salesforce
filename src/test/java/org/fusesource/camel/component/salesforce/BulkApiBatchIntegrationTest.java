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

import org.apache.camel.component.mock.MockEndpoint;
import org.fusesource.camel.component.salesforce.api.dto.bulk.*;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkApiBatchIntegrationTest extends AbstractBulkApiTestBase {
    private static final String TEST_REQUEST_XML = "/test-request.xml";
    private static final String TEST_REQUEST_CSV = "/test-request.csv";

    @DataPoints
    public static BatchTest[] getBatches() {
        List<BatchTest> result = new ArrayList<BatchTest>();
        BatchTest test = new BatchTest();
        test.contentType = ContentType.XML;
        test.stream = AbstractBulkApiTestBase.class.getResourceAsStream(TEST_REQUEST_XML);
        result.add(test);

        test = new BatchTest();
        test.contentType = ContentType.CSV;
        test.stream = AbstractBulkApiTestBase.class.getResourceAsStream(TEST_REQUEST_CSV);
        result.add(test);

        // TODO test ZIP_XML and ZIP_CSV
        return result.toArray(new BatchTest[result.size()]);
    }

    @Theory
    public void testBatchLifecycle(BatchTest request) throws Exception {
        log.info(String.format("Testing Batch lifecycle with %s content", request.contentType));

        // create an UPSERT test Job for this batch request
        JobInfo jobInfo = new JobInfo();
        jobInfo.setOperation(OperationEnum.UPSERT);
        jobInfo.setContentType(request.contentType);
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setExternalIdFieldName("Name");
        jobInfo = createJob(jobInfo);

        // test createBatch
        MockEndpoint mock = getMockEndpoint("mock:createBatch");
        mock.expectedMessageCount(1);

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(SalesforceEndpointConfig.JOB_ID, jobInfo.getId());
        headers.put(SalesforceEndpointConfig.CONTENT_TYPE, jobInfo.getContentType());
        template().sendBodyAndHeaders("direct:createBatch", request.stream, headers);

        mock.assertIsSatisfied();
        BatchInfo batchInfo = mock.getExchanges().get(0).getIn().getBody(BatchInfo.class);
        assertNotNull("Null batch", batchInfo);
        assertNotNull("Null batch id", batchInfo.getId());

        // test getAllBatches
        mock = getMockEndpoint("mock:getAllBatches");
        mock.expectedMessageCount(1);
        template().sendBody("direct:getAllBatches", jobInfo);

        mock.assertIsSatisfied();
        @SuppressWarnings("unchecked")
        List<BatchInfo> batches = mock.getExchanges().get(0).getIn().getBody(List.class);
        assertNotNull("Null batches", batches);
        assertFalse("Empty batch list", batches.isEmpty());

        // test getBatch
        batchInfo = batches.get(0);
        batchInfo = getBatchInfo(batchInfo);

        // test getRequest
        mock = getMockEndpoint("mock:getRequest");
        mock.expectedMessageCount(1);
        template().sendBody("direct:getRequest", batchInfo);

        mock.assertIsSatisfied();
        InputStream requestStream = mock.getExchanges().get(0).getIn().getBody(InputStream.class);
        assertNotNull("Null batch request", requestStream);

        // wait for batch to finish
        log.info("Waiting for batch to finish...");
        while (!batchProcessed(batchInfo)) {
            // sleep 5 seconds
            Thread.sleep(5000);
            // check again
            batchInfo = getBatchInfo(batchInfo);
        }
        log.info("Batch finished with state " + batchInfo.getState());
        assertEquals("Batch did not succeed", BatchStateEnum.COMPLETED, batchInfo.getState());

        // test getResults
        mock = getMockEndpoint("mock:getResults");
        mock.expectedMessageCount(1);
        template().sendBody("direct:getResults", batchInfo);

        mock.assertIsSatisfied();
        InputStream results = mock.getExchanges().get(0).getIn().getBody(InputStream.class);
        assertNotNull("Null batch results", results);

        // close the test job
        mock = getMockEndpoint("mock:closeJob");
        mock.expectedMessageCount(1);
        template().sendBody("direct:closeJob", jobInfo);
        mock.assertIsSatisfied();
    }

    private static class BatchTest {
        public InputStream stream;
        public ContentType contentType;
    }
}