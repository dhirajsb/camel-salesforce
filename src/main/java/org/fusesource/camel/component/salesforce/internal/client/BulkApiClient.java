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

import org.fusesource.camel.component.salesforce.api.SalesforceException;
import org.fusesource.camel.component.salesforce.api.dto.bulk.*;

import java.io.InputStream;
import java.util.List;

/**
 * Client interface for Salesforce Bulk API
 */
public interface BulkApiClient {

    /**
     * Creates a Bulk Job
     * @param jobInfo A {@link JobInfo} with required fields
     * @return a complete job description {@link JobInfo}
     * @throws org.fusesource.camel.component.salesforce.api.SalesforceException on error
     */
    JobInfo createJob(JobInfo jobInfo) throws SalesforceException;

    JobInfo getJob(String jobId) throws SalesforceException;

    JobInfo closeJob(String jobId) throws SalesforceException;

    JobInfo abortJob(String jobId) throws SalesforceException;

    BatchInfo createBatch(InputStream batchStream, String jobId, ContentType contentTypeEnum) throws SalesforceException;

    BatchInfo getBatch(String jobId, String batchId) throws SalesforceException;

    List<BatchInfo> getAllBatches(String jobId) throws SalesforceException;

    InputStream getRequest(String jobId, String batchId) throws SalesforceException;

    InputStream getResults(String jobId, String batchId) throws SalesforceException;

    BatchInfo createBatchQuery(String jobId, String soqlQuery, ContentType jobContentType) throws SalesforceException;

    List<String> getQueryResultIds(String jobId, String batchId) throws SalesforceException;

    InputStream getQueryResult(String jobId, String batchId, String resultId) throws SalesforceException;

}
