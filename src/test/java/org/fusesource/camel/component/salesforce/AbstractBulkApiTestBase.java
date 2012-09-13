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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.fusesource.camel.component.salesforce.api.dto.bulk.BatchInfo;
import org.fusesource.camel.component.salesforce.api.dto.bulk.BatchStateEnum;
import org.fusesource.camel.component.salesforce.api.dto.bulk.JobInfo;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public abstract class AbstractBulkApiTestBase extends AbstractSalesforceTestBase {

    protected JobInfo createJob(JobInfo jobInfo) throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:createJob");
        mock.expectedMessageCount(1);
        template().sendBody("direct:createJob", jobInfo);

        mock.assertIsSatisfied();
        jobInfo = mock.getExchanges().get(0).getIn().getBody(JobInfo.class);
        // reset mock:createJob
        mock.reset();
        assertNotNull("Missing JobId", jobInfo.getId());
        return jobInfo;
    }

    @Override
    protected RouteBuilder doCreateRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // test createJob
                from("direct:createJob").
                    to("force://createJob").
                    to("mock:createJob");

                // test getJob
                from("direct:getJob").
                    to("force:getJob").
                    to("mock:getJob");

                // test closeJob
                from("direct:closeJob").
                    to("force:closeJob").
                    to("mock:closeJob");

                // test abortJob
                from("direct:abortJob").
                    to("force:abortJob").
                    to("mock:abortJob");

                // test createBatch
                from("direct:createBatch").
                    to("force:createBatch").
                    to("mock:createBatch");

                // test getBatch
                from("direct:getBatch").
                    to("force:getBatch").
                    to("mock:getBatch");

                // test getAllBatches
                from("direct:getAllBatches").
                    to("force:getAllBatches").
                    to("mock:getAllBatches");

                // test getRequest
                from("direct:getRequest").
                    to("force:getRequest").
                    to("mock:getRequest");

                // test getResults
                from("direct:getResults").
                    to("force:getResults").
                    to("mock:getResults");

                // test createBatchQuery
                from("direct:createBatchQuery").
                    to("force:createBatchQuery?sObjectQuery=SELECT+Name%2c+Description__c%2c+Price__c%2c+Total_Inventory__c+FROM+Merchandise__c+WHERE+Name+LIKE+%27%25Bulk+API%25%27").
                    to("mock:createBatchQuery");

                // test getQueryResultIds
                from("direct:getQueryResultIds").
                    to("force:getQueryResultIds").
                    to("mock:getQueryResultIds");

                // test getQueryResult
                from("direct:getQueryResult").
                    to("force:getQueryResult").
                    to("mock:getQueryResult");

            }
        };
    }

    protected boolean batchProcessed(BatchInfo batchInfo) {
        BatchStateEnum state = batchInfo.getState();
        return !(state == BatchStateEnum.QUEUED || state == BatchStateEnum.IN_PROGRESS);
    }

    protected BatchInfo getBatchInfo(BatchInfo batchInfo) throws InterruptedException {
        MockEndpoint mock;
        mock = getMockEndpoint("mock:getBatch");
        mock.expectedMessageCount(1);
        template().sendBody("direct:getBatch", batchInfo);

        mock.assertIsSatisfied();
        batchInfo = mock.getExchanges().get(0).getIn().getBody(BatchInfo.class);
        // reset mock:getBatch
        mock.reset();
        assertNotNull("Null batch", batchInfo);
        assertNotNull("Null batch id", batchInfo.getId());

        return batchInfo;
    }
}
