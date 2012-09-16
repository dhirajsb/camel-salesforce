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
import org.apache.camel.util.ObjectHelper;
import org.fusesource.camel.component.salesforce.SalesforceEndpoint;
import org.fusesource.camel.component.salesforce.api.DefaultRestClient;
import org.fusesource.camel.component.salesforce.api.RestClient;
import org.fusesource.camel.component.salesforce.api.RestException;
import org.fusesource.camel.component.salesforce.api.dto.AbstractSObjectBase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.fusesource.camel.component.salesforce.SalesforceEndpointConfig.*;

public abstract class AbstractRestProcessor extends AbstractSalesforceProcessor {

    protected static final String RESPONSE_CLASS = AbstractRestProcessor.class.getName() + ".responseClass";

    private RestClient restClient;
    private Map<String, Class<?>> classMap;

    public AbstractRestProcessor(SalesforceEndpoint endpoint) {
        super(endpoint);

        final PayloadFormat payloadFormat = endpoint.getEndpointConfiguration().getPayloadFormat();

        this.restClient = new DefaultRestClient(httpClient, endpointConfig.get(API_VERSION),
            payloadFormat.toString().toLowerCase() , session);

        this.classMap = endpoint.getComponent().getClassMap();
    }

    @Override
    public final boolean process(final Exchange exchange, final AsyncCallback callback) {

        // pre-process request message
        try {
            processRequest(exchange);
        } catch (RestException e) {
            LOG.error(e.getMessage(), e);
            exchange.setException(e);
            callback.done(true);
            return true;
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            exchange.setException(new RestException(e.getMessage(), e));
            callback.done(true);
            return true;
        }

        // call Salesforce asynchronously
        executor.execute(new Runnable() {
            @Override
            public void run() {

                InputStream responseEntity = null;
                // common parameters
                String sObjectName;
                String sObjectId = null;
                String sObjectExtIdName = null;
                String sObjectExtIdValue = null;
                // input SObject
                AbstractSObjectBase sObjectBase = null;
                Object oldValue = null;

                try {

                    // call Operation using REST client
                    switch (getOperationName()) {
                        case GET_VERSIONS:
                            responseEntity = restClient.getVersions();
                            break;

                        case GET_RESOURCES:
                            responseEntity = restClient.getResources();
                            break;

                        case GET_GLOBAL_OBJECTS:
                            responseEntity = restClient.getGlobalObjects();
                            break;

                        case GET_BASIC_INFO:
                            sObjectName = getParameter(SOBJECT_NAME, exchange, USE_BODY, NOT_OPTIONAL);
                            responseEntity = restClient.getBasicInfo(sObjectName);

                            break;

                        case GET_DESCRIPTION:
                            sObjectName = getParameter(SOBJECT_NAME, exchange, USE_BODY, NOT_OPTIONAL);
                            responseEntity = restClient.getDescription(sObjectName);
                            break;

                        case GET_SOBJECT:
                            // determine parameters from input AbstractSObject
                            sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
                            if (sObjectBase != null) {
                                sObjectName = sObjectBase.getClass().getSimpleName();
                                sObjectId = sObjectBase.getId();
                            } else {
                                sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                sObjectId = getParameter(SOBJECT_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }

                            // use sObject name to load class
                            setResponseClass(exchange, sObjectName);

                            // get optional field list
                            String fieldsValue = getParameter(SOBJECT_FIELDS, exchange, IGNORE_BODY, IS_OPTIONAL);
                            String[] fields = null;
                            if (fieldsValue != null) {
                                fields = fieldsValue.split(",");
                            }

                            responseEntity = restClient.getSObject(sObjectName,
                                sObjectId,
                                fields);

                            break;

                        case CREATE_SOBJECT:
                            // determine parameters from input AbstractSObject
                            sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
                            if (sObjectBase != null) {
                                sObjectName = sObjectBase.getClass().getSimpleName();
                            } else {
                                sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
                            }

                            responseEntity = restClient.createSObject(sObjectName,
                                getRequestStream(exchange));

                            break;

                        case UPDATE_SOBJECT:
                            // determine parameters from input AbstractSObject
                            sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
                            if (sObjectBase != null) {
                                sObjectName = sObjectBase.getClass().getSimpleName();
                                // remember the sObject Id
                                sObjectId = sObjectBase.getId();
                                // clear base object fields, which cannot be updated
                                sObjectBase.clearBaseFields();
                            } else {
                                sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                sObjectId = getParameter(SOBJECT_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
                            }

                            restClient.updateSObject(sObjectName,
                                sObjectId,
                                getRequestStream(exchange));

                            break;

                        case DELETE_SOBJECT:
                            // determine parameters from input AbstractSObject
                            sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
                            if (sObjectBase != null) {
                                sObjectName = sObjectBase.getClass().getSimpleName();
                                sObjectId = sObjectBase.getId();
                            } else {
                                sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                sObjectId = getParameter(SOBJECT_ID, exchange, USE_BODY, NOT_OPTIONAL);
                            }

                            restClient.deleteSObject(sObjectName,
                                sObjectId);
                            break;

                        case GET_SOBJECT_WITH_ID:
                            sObjectExtIdName = getParameter(SOBJECT_EXT_ID_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);

                            // determine parameters from input AbstractSObject
                            sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
                            if (sObjectBase != null) {
                                sObjectName = sObjectBase.getClass().getSimpleName();
                                oldValue = getAndClearPropertyValue(sObjectBase, sObjectExtIdName);
                                sObjectExtIdValue = oldValue.toString();
                            } else {
                                sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                sObjectExtIdValue = getParameter(SOBJECT_EXT_ID_VALUE, exchange, USE_BODY, NOT_OPTIONAL);
                            }

                            // use sObject name to load class
                            setResponseClass(exchange, sObjectName);

                            responseEntity = restClient.getSObjectWithId(sObjectName,
                                sObjectExtIdName,
                                sObjectExtIdValue);

                            break;

                        case UPSERT_SOBJECT:
                            sObjectExtIdName = getParameter(SOBJECT_EXT_ID_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);

                            // determine parameters from input AbstractSObject
                            oldValue = null;
                            sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
                            if (sObjectBase != null) {
                                sObjectName = sObjectBase.getClass().getSimpleName();
                                oldValue = getAndClearPropertyValue(sObjectBase, sObjectExtIdName);
                                sObjectExtIdValue = oldValue.toString();
                                // clear base object fields, which cannot be updated
                                sObjectBase.clearBaseFields();
                            } else {
                                sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                sObjectExtIdValue = getParameter(SOBJECT_EXT_ID_VALUE, exchange, IGNORE_BODY, NOT_OPTIONAL);
                            }

                            responseEntity = restClient.upsertSObject(sObjectName,
                                sObjectExtIdName,
                                sObjectExtIdValue,
                                getRequestStream(exchange));

                            break;

                        case DELETE_SOBJECT_WITH_ID:
                            sObjectExtIdName = getParameter(SOBJECT_EXT_ID_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);

                            // determine parameters from input AbstractSObject
                            oldValue = null;
                            sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
                            if (sObjectBase != null) {
                                sObjectName = sObjectBase.getClass().getSimpleName();
                                oldValue = getAndClearPropertyValue(sObjectBase, sObjectExtIdName);
                                sObjectExtIdValue = oldValue.toString();
                            } else {
                                sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
                                sObjectExtIdValue = getParameter(SOBJECT_EXT_ID_VALUE, exchange, USE_BODY, NOT_OPTIONAL);
                            }

                            restClient.deleteSObjectWithId(sObjectName,
                                sObjectExtIdName,
                                sObjectExtIdValue);

                            break;

                        case QUERY:
                            final String sObjectQuery = getParameter(SOBJECT_QUERY, exchange, USE_BODY, NOT_OPTIONAL);

                            // use sObject name to load class
                            setResponseClass(exchange, null);

                            responseEntity = restClient.query(sObjectQuery);
                            break;

                        case QUERY_MORE:
                            // reuse SOBJECT_QUERY parameter name for nextRecordsUrl
                            final String nextRecordsUrl = getParameter(SOBJECT_QUERY, exchange, USE_BODY, NOT_OPTIONAL);

                            // use custom response class property
                            setResponseClass(exchange, null);

                            responseEntity = restClient.queryMore(nextRecordsUrl);
                            break;

                        case SEARCH:
                            final String sObjectSearch = getParameter(SOBJECT_SEARCH, exchange, USE_BODY, NOT_OPTIONAL);

                            responseEntity = restClient.search(sObjectSearch);
                            break;

                    }

                    // process response entity and create out message
                    processResponse(exchange, responseEntity);

                } catch (RestException e) {
                    String msg = String.format("Error processing %s: [%s] \"%s\"",
                        operationName, e.getStatusCode(), e.getMessage());
                    LOG.error(msg, e);
                    exchange.setException(e);
                } catch (RuntimeException e) {
                    String msg = String.format("Unexpected Error processing %s: \"%s\"",
                        operationName, e.getMessage());
                    LOG.error(msg, e);
                    exchange.setException(new RestException(msg, e));
                } finally {
                    // restore fields
                    if (sObjectBase != null) {
                        // restore the Id if it was cleared
                        if (sObjectId != null) {
                            sObjectBase.setId(sObjectId);
                        }
                        // restore the external id if it was cleared
                        if (sObjectExtIdName != null && oldValue != null) {
                            try {
                                setPropertyValue(sObjectBase, sObjectExtIdName, oldValue);
                            } catch (RestException e) {
                                // YES, the exchange may fail if the property cannot be reset!!!
                                LOG.error(e.getMessage(), e);
                                exchange.setException(e);
                            }
                        }
                    }
                    // consume response entity
                    if (responseEntity != null) {
                        try {
                            responseEntity.close();
                        } catch (IOException e) {
                        }
                    }
                    callback.done(false);
                }

            }

        });

        // continue routing asynchronously
        return false;
    }

    private void setPropertyValue(AbstractSObjectBase sObjectBase, String name, Object value) throws RestException {
        try {
            // set the value with the set method
            Method setMethod = sObjectBase.getClass().getMethod("set" + name, new Class<?>[] {value.getClass()});
            setMethod.invoke(sObjectBase, new Object[] { value });
        } catch (NoSuchMethodException e) {
            String msg = String.format("SObject %s does not have a field %s",
                sObjectBase.getClass().getName(), name);
            LOG.error(msg, e);
            throw new RestException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = String.format("Error setting value %s.%s",
                sObjectBase.getClass().getSimpleName(), name);
            LOG.error(msg, e);
            throw new RestException(msg, e);
        } catch (IllegalAccessException e) {
            String msg = String.format("Error accessing value %s.%s",
                sObjectBase.getClass().getSimpleName(), name);
            LOG.error(msg, e);
            throw new RestException(msg, e);
        }
    }

    private Object getAndClearPropertyValue(AbstractSObjectBase sObjectBase, String propertyName) throws RestException {
        try {
            // obtain the value using the get method
            Method getMethod = sObjectBase.getClass().getMethod("get" + propertyName, new Class<?>[] {});
            Object value = getMethod.invoke(sObjectBase, new Object[] {});

            // clear the value with the set method
            Method setMethod = sObjectBase.getClass().getMethod("set" + propertyName, new Class<?>[] {getMethod.getReturnType()});
            setMethod.invoke(sObjectBase, new Object[] { null });

            return value;
        } catch (NoSuchMethodException e) {
            String msg = String.format("SObject %s does not have a field %s",
                sObjectBase.getClass().getSimpleName(), propertyName);
            LOG.error(msg, e);
            throw new RestException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = String.format("Error getting/setting value %s.%s",
                sObjectBase.getClass().getSimpleName(), propertyName);
            LOG.error(msg, e);
            throw new RestException(msg, e);
        } catch (IllegalAccessException e) {
            String msg = String.format("Error accessing value %s.%s",
                sObjectBase.getClass().getSimpleName(), propertyName);
            LOG.error(msg, e);
            throw new RestException(msg, e);
        }
    }

    // pre-process request message
    protected abstract void processRequest(Exchange exchange) throws RestException;

    // get request stream from In message
    protected abstract InputStream getRequestStream(Exchange exchange) throws RestException;

    protected void setResponseClass(Exchange exchange, String sObjectName) throws RestException {
        Class<?> sObjectClass = null;

        if (sObjectName != null) {
            // lookup class from class map
            sObjectClass = classMap.get(sObjectName);
            if (null == sObjectClass) {
                String msg = String.format("No class found for SObject %s", sObjectName);
                LOG.error(msg);
                throw new RestException(msg, null);
            }

        } else {

            // use custom response class property
            final String className = getParameter(SOBJECT_CLASS, exchange, IGNORE_BODY, NOT_OPTIONAL);

            sObjectClass = ObjectHelper.loadClass(className, getClass().getClassLoader());
            if (null == sObjectClass) {
                String msg = String.format("Error loading class %s", className);
                LOG.error(msg);
                throw new RestException(msg, null);
            }
        }
        exchange.setProperty(RESPONSE_CLASS, sObjectClass);
    }

    // process response entity and set out message in exchange
    protected abstract void processResponse(Exchange exchange, InputStream responseEntity) throws RestException;

}
