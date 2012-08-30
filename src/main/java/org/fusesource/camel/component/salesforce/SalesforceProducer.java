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
package org.fusesource.camel.component.salesforce;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.fusesource.camel.component.salesforce.api.DefaultRestClient;
import org.fusesource.camel.component.salesforce.api.RestClient;
import org.fusesource.camel.component.salesforce.internal.AbstractRestProcessor;
import org.fusesource.camel.component.salesforce.internal.JsonRestProcessor;
import org.fusesource.camel.component.salesforce.internal.PayloadFormat;
import org.fusesource.camel.component.salesforce.internal.XmlRestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;

/**
 * The Salesforce producer.
 */
public class SalesforceProducer extends DefaultAsyncProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(SalesforceProducer.class);

    private AbstractRestProcessor processor;

    private RestClient restClient;
    private final SalesforceEndpointConfig endpointConfig;

    public SalesforceProducer(SalesforceEndpoint endpoint,
                              SalesforceEndpointConfig endpointConfig) {
        super(endpoint);

        this.endpointConfig = endpointConfig;

        final SalesforceComponent component = (SalesforceComponent) endpoint.getComponent();
        final PayloadFormat payloadFormat = endpointConfig.getPayloadFormat();
        restClient = new DefaultRestClient(component.getHttpClient(), endpointConfig.getApiVersion(),
            payloadFormat.toString().toLowerCase(), component.getSession());

        // set the default format
        switch (payloadFormat) {
            case JSON:
                // create a JSON exchange processor
                processor = new JsonRestProcessor(restClient, endpoint.getApiName(),
                    component.getExecutor(),
                    endpointConfig.toValueMap());
                break;
            case XML:
                processor = new XmlRestProcessor(restClient, endpoint.getApiName(),
                    component.getExecutor(),
                    endpointConfig.toValueMap());
                break;
        }
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (!isRunAllowed()) {
            if (exchange.getException() == null) {
                exchange.setException(new RejectedExecutionException());
            }
            callback.done(true);
            return true;
        }

        LOG.debug("Processing {}", ((SalesforceEndpoint)getEndpoint()).getApiName());
        return processor.process(exchange, callback);
    }

}
