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

public enum ApiName {

    GET_VERSIONS("getVersions"),
    GET_RESOURCES("getResources"),
    GET_GLOBAL_OBJECTS("getGlobalObjects"),
    GET_SOBJECT_BASIC_INFO("getSObjectBasicInfo"),
    GET_SOBJECT_DESCRIPTION("getSObjectDescription"),
    GET_SOBJECT_BY_ID("getSObjectById"),
    CREATE_SOBJECT("createSObject"),
    UPDATE_SOBJECT_BY_ID("updateSObjectById"),
    DELETE_SOBJECT_BY_ID("deleteSObjectById"),
    GET_SOBJECT_BY_EXTERNAL_ID("getSObjectByExternalId"),
    CREATE_OR_UPDATE_SOBJECT_BY_EXTERNAL_ID("createOrUpdateSObjectByExternalId"),
    DELETE_SOBJECT_BY_EXTERNAL_ID("deleteSObjectByExternalId"),
    EXECUTE_QUERY("executeQuery"),
    GET_QUERY_RECORDS("getQueryRecords"),
    EXECUTE_SEARCH("executeSearch");

    private final String value;

    private ApiName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ApiName fromValue(String value) {
        for (ApiName apiName : ApiName.values()) {
            if (apiName.value.equals(value)) {
                return apiName;
            }
        }
        throw new IllegalArgumentException(value);
    }
}
