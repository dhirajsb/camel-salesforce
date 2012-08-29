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
import org.apache.camel.Exchange;
import org.fusesource.camel.component.salesforce.api.dto.ForceVersions;
import org.fusesource.camel.component.salesforce.api.RestClient;
import org.fusesource.camel.component.salesforce.api.dto.RestResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;

public class XmlRestProcessor extends AbstractRestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(XmlRestProcessor.class);

    private final XStream xStream;
    private static final String RESPONSE_CLASS = XmlRestProcessor.class.getName() + ".responseClass";

    public XmlRestProcessor(RestClient restClient,
                            RestClientHelper.ApiName apiName, Executor executor,
                            Map<String, String> endpointConfig) {
        super(restClient, apiName, executor, endpointConfig);

        xStream = new XStream();
    }

    @Override
    protected InputStream processRequest(Exchange exchange) {
        // TODO process XML request parameters
        switch (getApiName()) {
            case GET_VERSIONS:
                exchange.setProperty(RESPONSE_CLASS, ForceVersions.class);
                break;

            case GET_RESOURCES:
                exchange.setProperty(RESPONSE_CLASS, RestResources.class);
                break;
        }
        return null;
    }

    @Override
    protected void processResponse(Exchange exchange, InputStream responseEntity) {
        try {
            Class<?> responseClass = exchange.getProperty(RESPONSE_CLASS, Class.class);
            xStream.processAnnotations(responseClass);
            Object response = responseClass.newInstance();
            xStream.fromXML(responseEntity, response);
            exchange.getIn().setBody(response);
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
