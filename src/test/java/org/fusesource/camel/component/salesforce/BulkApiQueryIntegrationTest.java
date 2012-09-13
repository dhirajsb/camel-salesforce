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
import java.util.List;

public class BulkApiQueryIntegrationTest extends AbstractBulkApiTestBase {

    @DataPoints
    public static ContentType[] getContentTypes() {
        return new ContentType[] {
            ContentType.XML,
            ContentType.CSV
        };
    }

    @Theory
    public void testQueryLifecycle(ContentType contentType) throws Exception {
        log.info(String.format("Testing Query lifecycle with %s content", contentType));

        // create a QUERY test Job
        JobInfo jobInfo = new JobInfo();
        jobInfo.setOperation(OperationEnum.QUERY);
        jobInfo.setContentType(contentType);
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo = createJob(jobInfo);

        // test createQuery
        MockEndpoint mock = getMockEndpoint("mock:createBatchQuery");
        mock.expectedMessageCount(1);

        template().sendBody("direct:createBatchQuery", jobInfo);

        mock.assertIsSatisfied();
        BatchInfo batchInfo = mock.getExchanges().get(0).getIn().getBody(BatchInfo.class);
        assertNotNull("Null batch query", batchInfo);
        assertNotNull("Null batch query id", batchInfo.getId());

        // test getRequest
        mock = getMockEndpoint("mock:getRequest");
        mock.expectedMessageCount(1);
        template().sendBody("direct:getRequest", batchInfo);

        mock.assertIsSatisfied();
        InputStream requestStream = mock.getExchanges().get(0).getIn().getBody(InputStream.class);
        assertNotNull("Null batch request", requestStream);

        // wait for batch to finish
        log.info("Waiting for query batch to finish...");
        while (!batchProcessed(batchInfo)) {
            // sleep 5 seconds
            Thread.sleep(5000);
            // check again
            batchInfo = getBatchInfo(batchInfo);
        }
        log.info("Query finished with state " + batchInfo.getState());
        assertEquals("Query did not succeed", BatchStateEnum.COMPLETED, batchInfo.getState());

        // test getQueryResultList
        mock = getMockEndpoint("mock:getQueryResultIds");
        mock.expectedMessageCount(1);
        template().sendBody("direct:getQueryResultIds", batchInfo);

        mock.assertIsSatisfied();
        @SuppressWarnings("unchecked")
        List<String> resultIds = mock.getExchanges().get(0).getIn().getBody(List.class);
        assertNotNull("Null query result ids", resultIds);
        assertFalse("Empty result ids", resultIds.isEmpty());

        // test getQueryResult
        for (String resultId : resultIds) {
            mock = getMockEndpoint("mock:getQueryResult");
            mock.expectedMessageCount(1);
            template().sendBodyAndHeader("direct:getQueryResult", batchInfo,
                SalesforceEndpointConfig.RESULT_ID, resultId);

            mock.assertIsSatisfied();
            InputStream results = mock.getExchanges().get(0).getIn().getBody(InputStream.class);
            assertNotNull("Null query result", results);
        }

        // close the test job
        mock = getMockEndpoint("mock:closeJob");
        mock.expectedMessageCount(1);
        template().sendBody("direct:closeJob", jobInfo);
        mock.assertIsSatisfied();
    }

}