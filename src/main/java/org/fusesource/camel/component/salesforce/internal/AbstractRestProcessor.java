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

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.fusesource.camel.component.salesforce.SalesforceEndpointConfig;
import org.fusesource.camel.component.salesforce.api.RestClient;
import org.fusesource.camel.component.salesforce.api.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AbstractRestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRestProcessor.class);

    private RestClient restClient;
    private RestClientHelper.ApiName apiName;

    private Executor executor;
    private Map<String, String> endpointConfig;

    public AbstractRestProcessor(RestClient restClient,
                                 RestClientHelper.ApiName apiName, Executor executor,
                                 Map<String, String> endpointConfig) {
        this.restClient = restClient;
        this.apiName = apiName;
        this.endpointConfig = endpointConfig;

        this.executor = executor;
        if (null == this.executor) {
            this.executor = Executors.newCachedThreadPool();
        }
    }

    public final boolean process(final Exchange exchange, final AsyncCallback callback) {

        // process parameters
        final InputStream requestEntity = processRequest(exchange);
        // sets exception on exchange on error
        // or an InputStream in exchange property REQUEST_INPUT_STREAM
        Exception exception = exchange.getException();
        if (exception != null) {
            // there was an error processing request parameters
            callback.done(true);
            return true;
        }

        // call Salesforce asynchronously
        executor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream responseEntity = null;
                try {

                    // call API using REST client
                    final String sObjectName = exchange.getProperty(SalesforceEndpointConfig.SOBJECT_NAME, String.class);
                    final String sObjectId = exchange.getProperty(SalesforceEndpointConfig.SOBJECT_ID, String.class);

                    switch (getApiName()) {
                        case GET_VERSIONS:
                            responseEntity = restClient.getVersions();
                            break;

                        case GET_RESOURCES:
                            responseEntity = restClient.getResources();
                            break;

                        case GET_GLOBAL_OBJECTS:
                            responseEntity = restClient.getGlobalObjects();
                            break;

                        case GET_SOBJECT_BASIC_INFO:
                            responseEntity = restClient.getSObjectBasicInfo(sObjectName);
                            break;

                        case GET_SOBJECT_DESCRIPTION:
                            responseEntity = restClient.getSObjectDescription(sObjectName);
                            break;

                        case GET_SOBJECT_BY_ID:
                            responseEntity = restClient.getSObjectById(sObjectName,
                                sObjectId,
                                exchange.getProperty(SalesforceEndpointConfig.SOBJECT_FIELDS, String[].class));
                            break;

                        case CREATE_SOBJECT:
                            responseEntity = restClient.createSObject(sObjectName,
                                requestEntity);
                            break;

                        case UPDATE_SOBJECT_BY_ID:
                            responseEntity = restClient.updateSObjectById(sObjectName,
                                sObjectId,
                                requestEntity);
                            break;

                        case DELETE_SOBJECT_BY_ID:
                            restClient.deleteSObjectById(sObjectName,
                                sObjectId);
                            break;

                        case CREATE_OR_UPDATE_SOBJECT_BY_EXTERNAL_ID:
                            responseEntity = restClient.createOrUpdateSObjectByExternalId(sObjectName,
                                exchange.getProperty(SalesforceEndpointConfig.SOBJECT_EXT_ID_NAME, String.class),
                                exchange.getProperty(SalesforceEndpointConfig.SOBJECT_EXT_ID_VALUE, String.class),
                                requestEntity);
                            break;

                        case DELETE_SOBJECT_BY_EXTERNAL_ID:
                            restClient.deleteSObjectByExternalId(sObjectName,
                                exchange.getProperty(SalesforceEndpointConfig.SOBJECT_EXT_ID_NAME, String.class),
                                exchange.getProperty(SalesforceEndpointConfig.SOBJECT_EXT_ID_VALUE, String.class));
                            break;

                        case EXECUTE_QUERY:
                            // TODO
                            break;

                        case EXECUTE_SEARCH:
                            // TODO
                            break;
                    }

                    processResponse(exchange, responseEntity);

                } catch (RestException e) {
                    String msg = String.format("Error processing %s: [%s] \"%s\"",
                        apiName, e.getStatusCode(), e.getMessage());
                    LOG.error(msg, e);
                    exchange.setException(e);
                } finally {
                    // consume response entity
                    if (responseEntity != null) {
                        try {
                            responseEntity.close();
                        } catch (IOException e) {}
                    }
                    callback.done(false);
                }

            }

        });

        // continue routing asynchronously
        return false;
    }

    protected abstract InputStream processRequest(Exchange exchange);

    // for Jackson TypeReference
    protected abstract void processResponse(Exchange exchange, InputStream responseEntity);

    protected RestClientHelper.ApiName getApiName() {
        return apiName;
    }

    /**
     * Gets a property with provided name, and sets it on exchange.
     * @param propName
     * @param exchange
     * @param convertInBody
     * @return true if the property was found, false otherwise with an exception in the exchange.
     */
    protected final boolean setParameter(String propName, Exchange exchange,
                                      boolean convertInBody) {
        // get the field name from exchangeProperty
        // look for a message body, header or endpoint property in that order
        String propValue = getParameter(exchange, convertInBody, propName);

        if (propValue != null) {
            // set the property on the exchange
            exchange.setProperty(propName, propValue);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets value for a parameter from exchange body (optional), header, or endpoint config.
     * Also sets an exception on exchange if the property can't be found.
     * @param exchange
     * @param convertInBody
     * @param propName
     * @return value of property, null if not found, with an exception in exchange.
     */
    protected final String getParameter(Exchange exchange, boolean convertInBody, String propName) {
        String propValue = convertInBody ? exchange.getIn().getBody(String.class) : null;
        propValue = propValue != null ? propValue : exchange.getIn().getHeader(propName, String.class);
        propValue = propValue != null ? propValue : endpointConfig.get(propName);

        // error if property was not set
        if (propValue == null) {
            String msg = "Missing property " + propName;
            LOG.error(msg);
            exchange.setException(new RuntimeCamelException(msg));
        }

        return propValue;
    }

}
