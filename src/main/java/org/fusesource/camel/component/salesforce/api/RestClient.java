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

import java.io.InputStream;

public interface RestClient {

    /**
     * Lists summary information about each API version currently available,
     * including the version, label, and a link to each version's root.
     *
     * @return response entity
     * @throws RestException
     */
    InputStream getVersions() throws RestException;

    /**
     * Set the API version to use for the rest of the APIs. Since this is possible not thread safe,
     * it should be usually only called once during setup.
     *
     * @param version Salesforce api version
     */
    void setVersion(String version);

    /**
     * Lists available resources for the specified API version, including resource name and URI.
     *
     * @return response entity
     * @throws RestException
     */
    InputStream getResources() throws RestException;

    /**
     * Lists the available objects and their metadata for your organization's data.
     *
     * @return response entity
     * @throws RestException
     */
    InputStream getGlobalObjects() throws RestException;

    /**
     * Describes the individual metadata for the specified object.
     *
     * @param sObjectName specified object name
     * @return response entity
     * @throws RestException
     */
    InputStream getBasicInfo(String sObjectName) throws RestException;

    /**
     * Completely describes the individual metadata at all levels for the specified object.
     *
     * @param sObjectName specified object name
     * @return response entity
     * @throws RestException
     */
    InputStream getDescription(String sObjectName) throws RestException;

    /**
     * Retrieves a record for the specified object ID.
     *
     * @param sObjectName specified object name
     * @param id          object id
     * @return response entity
     * @throws RestException
     */
    InputStream getSObject(String sObjectName, String id, String[] fields) throws RestException;

    /**
     * Creates a record for the specified object.
     *
     * @param sObjectName specified object name
     * @param sObject     request entity
     * @return response entity
     * @throws RestException
     */
    InputStream createSObject(String sObjectName, InputStream sObject) throws RestException;

    /**
     * Updates a record for the specified object ID.
     *
     * @param sObjectName specified object name
     * @param id          object id
     * @param sObject     request entity
     * @return response entity
     * @throws RestException
     */
    void updateSObject(String sObjectName, String id, InputStream sObject) throws RestException;

    /**
     * Deletes a record for the specified object ID.
     *
     * @param sObjectName specified object name
     * @param id          object id
     * @return response entity
     * @throws RestException
     */
    void deleteSObject(String sObjectName, String id) throws RestException;

    /**
     * Retrieves a record for the specified external ID.
     *
     * @param sObjectName
     * @param fieldName
     * @param fieldValue
     * @return
     * @throws RestException
     */
    InputStream getSObjectWithId(String sObjectName, String fieldName, String fieldValue) throws RestException;

    /**
     * Creates or updates a record based on the value of a specified external ID field.
     *
     * @param sObjectName
     * @param fieldName
     * @param fieldValue
     * @param sObject
     * @return
     * @throws RestException
     */
    InputStream upsertSObject(String sObjectName,
                              String fieldName, String fieldValue, InputStream sObject) throws RestException;

    /**
     * Deletes a record based on the value of a specified external ID field.
     *
     * @param sObjectName
     * @param fieldName
     * @param fieldValue
     * @throws RestException
     */
    void deleteSObjectWithId(String sObjectName,
                             String fieldName, String fieldValue) throws RestException;

/*
    TODO
    SObject Blob Retrieve	/vXX.X/sobjects/SObject/id/blobField	Retrieves the specified blob field from an individual record.

    SObject User Password
    /vXX.X/sobjects/User/user id/password
    /vXX.X/sobjects/SelfServiceUser/self service user id/password

    These methods set, reset, or get information about a user password.
*/

    /**
     * Executes the specified SOQL query.
     *
     * @param soqlQuery
     * @return
     * @throws RestException
     */
    InputStream query(String soqlQuery) throws RestException;

    /**
     * Get SOQL query results using nextRecordsUrl.
     *
     * @param nextRecordsUrl
     * @return
     * @throws RestException
     */
    InputStream queryMore(String nextRecordsUrl) throws RestException;

    /**
     * Executes the specified SOSL search.
     *
     * @param soslQuery
     * @return
     * @throws RestException
     */
    InputStream search(String soslQuery) throws RestException;

}
