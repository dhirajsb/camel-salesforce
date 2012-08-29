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

import java.util.HashMap;
import java.util.Map;

public class RestClientHelper {

    private static Map<String, ApiName> apiMap = new HashMap<String, ApiName>();

    /**
     * Maps api name to enum
     * @param apiStr api name
     * @return matching ApiName enum
     * @throws IllegalArgumentException for unknown names
     */
    public static ApiName valueOf(String apiStr) throws IllegalArgumentException {
        ApiName api = apiMap.get(apiStr);
        if (null == api) {
            throw new IllegalArgumentException("Unknown api name " + apiStr);
        }
        return api;
    }

    static {

        apiMap.put("getVersions", ApiName.GET_VERSIONS);
        apiMap.put("getResources", ApiName.GET_RESOURCES);
        apiMap.put("getGlobalObjects", ApiName.GET_GLOBAL_OBJECTS);
        apiMap.put("getSObjectBasicInfo", ApiName.GET_SOBJECT_BASIC_INFO);
        apiMap.put("getSObjectDescription", ApiName.GET_SOBJECT_DESCRIPTION);
        apiMap.put("getSObjectById", ApiName.GET_SOBJECT_BY_ID);
        apiMap.put("createSObject", ApiName.CREATE_SOBJECT);
        apiMap.put("updateSObjectById", ApiName.UPDATE_SOBJECT_BY_ID);
        apiMap.put("deleteSObjectById", ApiName.DELETE_SOBJECT_BY_ID);
        apiMap.put("createOrUpdateSObjectByExternalId", ApiName.CREATE_OR_UPDATE_SOBJECT_BY_EXTERNAL_ID);
        apiMap.put("deleteSObjectByExternalId", ApiName.DELETE_SOBJECT_BY_EXTERNAL_ID);
        apiMap.put("executeQuery", ApiName.EXECUTE_QUERY);
        apiMap.put("executeSearch", ApiName.EXECUTE_SEARCH);

    }

    public static enum ApiName {

        GET_VERSIONS,
        GET_RESOURCES,
        GET_GLOBAL_OBJECTS,
        GET_SOBJECT_BASIC_INFO,
        GET_SOBJECT_DESCRIPTION,
        GET_SOBJECT_BY_ID,
        CREATE_SOBJECT,
        UPDATE_SOBJECT_BY_ID,
        DELETE_SOBJECT_BY_ID,
        CREATE_OR_UPDATE_SOBJECT_BY_EXTERNAL_ID,
        DELETE_SOBJECT_BY_EXTERNAL_ID,
        EXECUTE_QUERY,
        EXECUTE_SEARCH

    }

}
