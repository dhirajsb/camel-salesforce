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
import org.apache.camel.Message;
import org.apache.http.Consts;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.type.TypeReference;
import org.fusesource.camel.component.salesforce.api.RestClient;
import org.fusesource.camel.component.salesforce.api.RestException;
import org.fusesource.camel.component.salesforce.api.dto.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class JsonRestProcessor extends AbstractRestProcessor {

    // it is ok to use a single thread safe ObjectMapper
    private final ObjectMapper objectMapper;
    private static final String RESPONSE_TYPE = JsonRestProcessor.class.getName() + ".responseType";

    public JsonRestProcessor(RestClient restClient,
                             ApiName apiName, Executor executor,
                             Map<String, String> endpointConfig, Map<String, Class<?>> classMap) {
        super(restClient, apiName, executor, endpointConfig, classMap);

        this.objectMapper = new ObjectMapper();
        // enable date time support including Joda DateTime
        this.objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    protected void processRequest(Exchange exchange) {

        switch (getApiName()) {
            case VERSIONS:
                // handle in built response types
                exchange.setProperty(RESPONSE_TYPE, new TypeReference<List<Version>>() {});
                break;

            case RESOURCES:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, RestResources.class);
                break;

            case GLOBAL_OBJECTS:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, GlobalObjects.class);
                break;

            case BASIC_INFO:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectBasicInfo.class);
                break;

            case DESCRIPTION:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectDescription.class);
                break;

            case CREATE:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            case UPSERT:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            case SEARCH:
                // handle known response type
                exchange.setProperty(RESPONSE_TYPE, new TypeReference<List<SearchResult>>() {});
                break;

        }
    }

    @Override
    protected InputStream getRequestStream(Exchange exchange) throws RestException {
        try {
            InputStream request;
            Message in = exchange.getIn();
            request = in.getBody(InputStream.class);
            if (request == null) {
                AbstractSObjectBase sObject = in.getBody(AbstractSObjectBase.class);
                if (sObject != null) {
                    // marshall the SObject
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    objectMapper.writeValue(out, sObject);
                    request = new ByteArrayInputStream(out.toByteArray());
                } else {
                    // if all else fails, get body as String
                    final String body = in.getBody(String.class);
                    if (null == body) {
                        String msg = "Unsupported request message body " +
                            (in.getBody() == null ? null : in.getBody().getClass());
                        throw new RestException(msg, null);
                    } else {
                        request = new ByteArrayInputStream(body.getBytes(Consts.UTF_8));
                    }
                }
            }

            return request;

        } catch (IOException e) {
            String msg = "Error marshaling request: " + e.getMessage();
            throw new RestException(msg, e);
        }
    }

    @Override
    protected void processResponse(Exchange exchange, InputStream responseEntity) throws RestException {
        // process JSON response for TypeReference
        try {
            // do we need to un-marshal a response
            if (responseEntity != null) {
                Object response = null;
                Class<?> responseClass = exchange.getProperty(RESPONSE_CLASS, Class.class);
                if (responseClass != null) {
                    response = objectMapper.readValue(responseEntity, responseClass);
                } else {
                    TypeReference<?> responseType = exchange.getProperty(RESPONSE_TYPE, TypeReference.class);
                    response = objectMapper.readValue(responseEntity, responseType);
                }
                exchange.getOut().setBody(response);
            }
            // copy headers and attachments
            exchange.getOut().getHeaders().putAll(exchange.getIn().getHeaders());
            exchange.getOut().getAttachments().putAll(exchange.getIn().getAttachments());
        } catch (IOException e) {
            String msg = "Error parsing JSON response: " + e.getMessage();
            throw new RestException(msg, e);
        }
    }

}
