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

import java.util.ArrayList;
import java.util.List;

public class BulkApiJobIntegrationTest extends AbstractBulkApiTestBase {

    // test jobs for testJobLifecycle
    @DataPoints
    public static JobInfo[] getJobs() {
        JobInfo jobInfo = new JobInfo();

        // insert XML
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setContentType(ContentType.XML);
        jobInfo.setOperation(OperationEnum.INSERT);

        List<JobInfo> result = new ArrayList<JobInfo>();
        result.add(jobInfo);

        // insert CSV
        jobInfo = new JobInfo();
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setContentType(ContentType.CSV);
        jobInfo.setOperation(OperationEnum.INSERT);
        result.add(jobInfo);

        // update CSV
        jobInfo = new JobInfo();
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setContentType(ContentType.CSV);
        jobInfo.setOperation(OperationEnum.UPDATE);
        result.add(jobInfo);

        // upsert CSV
        jobInfo = new JobInfo();
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setContentType(ContentType.CSV);
        jobInfo.setOperation(OperationEnum.UPSERT);
        jobInfo.setExternalIdFieldName("Name");
        result.add(jobInfo);

        // delete CSV
        jobInfo = new JobInfo();
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setContentType(ContentType.CSV);
        jobInfo.setOperation(OperationEnum.DELETE);
        result.add(jobInfo);

        // hard delete CSV
        jobInfo = new JobInfo();
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setContentType(ContentType.CSV);
        jobInfo.setOperation(OperationEnum.HARD_DELETE);
        result.add(jobInfo);

        // query CSV
        jobInfo = new JobInfo();
        jobInfo.setObject(Merchandise__c.class.getSimpleName());
        jobInfo.setContentType(ContentType.CSV);
        jobInfo.setOperation(OperationEnum.QUERY);
        result.add(jobInfo);

        return result.toArray(new JobInfo[result.size()]);
    }

    @Theory
    public void testJobLifecycle(JobInfo jobInfo) throws Exception {
        log.info(String.format("Testing Job lifecycle for %s of type %s", jobInfo.getOperation(), jobInfo.getContentType()));

        // test create
        jobInfo = createJob(jobInfo);

        // test get
        MockEndpoint mock = getMockEndpoint("mock:getJob");
        mock.expectedMessageCount(1);
        template().sendBody("direct:getJob", jobInfo);

        mock.assertIsSatisfied();
        jobInfo = mock.getExchanges().get(0).getIn().getBody(JobInfo.class);
        assertSame("Job should be OPEN", JobStateEnum.OPEN, jobInfo.getState());

        // test close
        mock = getMockEndpoint("mock:closeJob");
        mock.expectedMessageCount(1);
        template().sendBody("direct:closeJob", jobInfo);

        mock.assertIsSatisfied();
        jobInfo = mock.getExchanges().get(0).getIn().getBody(JobInfo.class);
        assertSame("Job should be CLOSED", JobStateEnum.CLOSED, jobInfo.getState());

        // test abort
        mock = getMockEndpoint("mock:abortJob");
        mock.expectedMessageCount(1);
        template().sendBody("direct:abortJob", jobInfo);

        mock.assertIsSatisfied();
        jobInfo = mock.getExchanges().get(0).getIn().getBody(JobInfo.class);
        assertSame("Job should be ABORTED", JobStateEnum.ABORTED, jobInfo.getState());
    }

}
