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

    VERSIONS("versions"),
    RESOURCES("resources"),
    GLOBAL_OBJECTS("globalObjects"),
    BASIC_INFO("basicInfo"),
    DESCRIPTION("description"),
    RETRIEVE("retrieve"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    RETRIEVE_WITH_ID("retrieveWithId"),
    UPSERT("upsert"),
    DELETE_WITH_ID("deleteWithId"),
    QUERY("query"),
    QUERY_MORE("queryMore"),
    SEARCH("search");

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
