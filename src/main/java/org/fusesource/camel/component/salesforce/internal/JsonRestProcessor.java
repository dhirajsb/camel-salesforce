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

import org.apache.camel.Exchange;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.fusesource.camel.component.salesforce.SalesforceEndpointConfig;
import org.fusesource.camel.component.salesforce.api.RestClient;
import org.fusesource.camel.component.salesforce.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class JsonRestProcessor extends AbstractRestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRestProcessor.class);

    private final ObjectMapper objectMapper;
    private static final String RESPONSE_TYPE = JsonRestProcessor.class.getName() + ".responseType";
    private static final String RESPONSE_CLASS = JsonRestProcessor.class.getName() + ".responseClass";

    public JsonRestProcessor(RestClient restClient,
                             RestClientHelper.ApiName apiName, Executor executor,
                             Map<String, String> endpointConfig) {
        super(restClient, apiName, executor, endpointConfig);

        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected InputStream processRequest(Exchange exchange) {
        // TODO process JSON parameters
        InputStream request = null;

        switch (getApiName()) {
            case GET_VERSIONS:
                // handle in built response types
                exchange.setProperty(RESPONSE_TYPE, new TypeReference<List<Version>>() {});
                break;

            case GET_RESOURCES:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, RestResources.class);
                break;

            case GET_GLOBAL_OBJECTS:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, GlobalObjects.class);
                break;

            case GET_SOBJECT_BASIC_INFO:
                // get parameters and set them in exchange
                if (!setParameter(SalesforceEndpointConfig.SOBJECT_NAME, exchange, true)) {
                    return null;
                }

                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectBasicInfo.class);
                break;

            case GET_SOBJECT_DESCRIPTION:
                // get parameters and set them in exchange
                if (!setParameter(SalesforceEndpointConfig.SOBJECT_NAME, exchange, true)) {
                    return null;
                }

                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectDescription.class);
                break;

            case GET_SOBJECT_BY_ID:
                // get parameters and set them in exchange
                if (!setParameter(SalesforceEndpointConfig.SOBJECT_NAME, exchange, false) ||
                    !setParameter(SalesforceEndpointConfig.SOBJECT_ID, exchange, true)) {
                    return null;
                }

                // use custom response class from endpoint config
                final String className = getParameter(exchange, false, SalesforceEndpointConfig.SOBJECT_CLASS);
                if (className == null) {
                    return null;
                }

                try {
                    Class sObjectClass = Thread.currentThread().getContextClassLoader().loadClass(className);
                    exchange.setProperty(RESPONSE_CLASS, sObjectClass);
                } catch (ClassNotFoundException e) {
                    LOG.error("Error loading class " + className);
                    exchange.setException(e);
                    return null;
                }
                break;

            case CREATE_SOBJECT:
                break;
            case UPDATE_SOBJECT_BY_ID:
                break;
            case DELETE_SOBJECT_BY_ID:
                break;
            case CREATE_OR_UPDATE_SOBJECT_BY_EXTERNAL_ID:
                break;
            case DELETE_SOBJECT_BY_EXTERNAL_ID:
                break;
            case EXECUTE_QUERY:
                break;
            case EXECUTE_SEARCH:
                break;
        }

        return request;
    }

    @Override
    protected void processResponse(Exchange exchange, InputStream responseEntity) {
        // process JSON response for TypeReference
        try {
            Object response = null;
            Class<?> responseClass = exchange.getProperty(RESPONSE_CLASS, Class.class);
            if (responseClass != null) {
                response = objectMapper.readValue(responseEntity, responseClass);
            } else {
                TypeReference<?> responseType = exchange.getProperty(RESPONSE_TYPE, TypeReference.class);
                response = objectMapper.readValue(responseEntity, responseType);
            }
            exchange.getIn().setBody(response);
        } catch (IOException e) {
            String msg = "Error parsing JSON response: " + e.getMessage();
            LOG.error(msg, e);
            exchange.setException(e);
        }
    }

}
