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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.camel.Exchange;
import org.fusesource.camel.component.salesforce.api.RestClient;
import org.fusesource.camel.component.salesforce.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.fusesource.camel.component.salesforce.SalesforceEndpointConfig.*;

public class XmlRestProcessor extends AbstractRestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(XmlRestProcessor.class);

    private final XStream xStream;
    private static final String RESPONSE_CLASS = XmlRestProcessor.class.getName() + ".responseClass";
    private static final String RESPONSE_ALIAS = XmlRestProcessor.class.getName() + ".responseAlias";

    public XmlRestProcessor(RestClient restClient,
                            RestClientHelper.ApiName apiName, Executor executor,
                            Map<String, String> endpointConfig) {
        super(restClient, apiName, executor, endpointConfig);

        // use NoNameCoder to avoid escaping __ in custom field names
        xStream = new XStream(new XppDriver(new NoNameCoder()));
    }

    @Override
    protected InputStream processRequest(Exchange exchange) {
        // TODO process XML request parameters
        InputStream request = null;

        switch (getApiName()) {
            case GET_VERSIONS:
                exchange.setProperty(RESPONSE_CLASS, Versions.class);
                break;

            case GET_RESOURCES:
                exchange.setProperty(RESPONSE_CLASS, RestResources.class);
                break;

            case GET_GLOBAL_OBJECTS:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, GlobalObjects.class);
                break;

            case GET_SOBJECT_BASIC_INFO:
                // get parameters and set them in exchange
                if (!setParameter(SOBJECT_NAME, exchange, USE_IN_BODY, NOT_OPTIONAL)) {
                    return null;
                }

                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectBasicInfo.class);
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    getParameter(SOBJECT_NAME, exchange, USE_IN_BODY, NOT_OPTIONAL));
                break;

            case GET_SOBJECT_DESCRIPTION:
                // get parameters and set them in exchange
                if (!setParameter(SOBJECT_NAME, exchange, USE_IN_BODY, NOT_OPTIONAL)) {
                    return null;
                }

                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectDescription.class);
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    getParameter(SOBJECT_NAME, exchange, USE_IN_BODY, NOT_OPTIONAL));
                break;

            case GET_SOBJECT_BY_ID:
                // get parameters and set them in exchange
                if (!setParameter(SOBJECT_NAME, exchange, IGNORE_IN_BODY, NOT_OPTIONAL) ||
                    !setParameter(SOBJECT_ID, exchange, USE_IN_BODY, NOT_OPTIONAL)) {
                    return null;
                }

                // get optional field list
                String fieldsValue = getParameter(SOBJECT_FIELDS, exchange, IGNORE_IN_BODY, IS_OPTIONAL);
                if (fieldsValue != null) {
                    String[] fields = fieldsValue.split(",");
                    exchange.setProperty(SOBJECT_FIELDS, fields);
                }

                // use custom response class property
                final String className = getParameter(SOBJECT_CLASS, exchange, IGNORE_IN_BODY, NOT_OPTIONAL);
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
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    getParameter(SOBJECT_NAME, exchange, IGNORE_IN_BODY, NOT_OPTIONAL));
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
        try {
            final Class<?> responseClass = exchange.getProperty(RESPONSE_CLASS, Class.class);
            // its ok to call this multiple times, as xstream ignores duplicate calls
            xStream.processAnnotations(responseClass);
            // TODO this is not really thread safe, fix it later
            final String responseAlias = exchange.getProperty(RESPONSE_ALIAS, String.class);
            if (responseAlias != null) {
                xStream.alias(responseAlias, responseClass);
            }
            Object response = responseClass.newInstance();
            xStream.fromXML(responseEntity, response);
            exchange.getOut().setBody(response);
            // copy headers and attachments
            exchange.getOut().getHeaders().putAll(exchange.getIn().getHeaders());
            exchange.getOut().getAttachments().putAll(exchange.getIn().getAttachments());
        } catch (XStreamException e) {
            String msg = "Error parsing XML response: " + e.getMessage();
            LOG.error(msg, e);
            exchange.setException(e);
        } catch (Exception e) {
            String msg = "Error creating XML response: " + e.getMessage();
            LOG.error(msg, e);
            exchange.setException(e);
        }
    }

}
