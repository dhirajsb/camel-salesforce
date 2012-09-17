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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultConsumer;
import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.fusesource.camel.component.salesforce.internal.client.DefaultRestClient;
import org.fusesource.camel.component.salesforce.internal.streaming.PushTopicHelper;
import org.fusesource.camel.component.salesforce.internal.client.RestClient;
import org.fusesource.camel.component.salesforce.internal.streaming.SubscriptionHelper;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * The Salesforce consumer.
 */
public class SalesforceConsumer extends DefaultConsumer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String EVENT_PROPERTY = "event";
    private static final String TYPE_PROPERTY = "type";
    private static final String CREATED_DATE_PROPERTY = "createdDate";
    private static final String SOBJECT_PROPERTY = "sobject";

    private final SalesforceEndpoint endpoint;
    public final SubscriptionHelper subscriptionHelper;

    private final String topicName;
    private final Class<?> sObjectClass;
    private boolean subscribed;

    public SalesforceConsumer(SalesforceEndpoint endpoint, Processor processor, SubscriptionHelper helper) {
        super(endpoint, processor);
        this.endpoint = endpoint;

        this.topicName = endpoint.getTopicName();
        this.subscriptionHelper = helper;

        // get sObjectClass to convert to
        final String className = endpoint.getEndpointConfiguration().getSObjectClass();
        if (className != null) {
            sObjectClass = endpoint.getComponent().getCamelContext().getClassResolver().resolveClass(className);
            if (sObjectClass == null) {
                String msg = String.format("SObject Class not found %s", className);
                log.error(msg);
                throw new RuntimeCamelException(msg);
            }
        } else {
            sObjectClass = null;
        }

    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final SalesforceEndpointConfig config = endpoint.getEndpointConfiguration();

        // is a query configured in the endpoint?
        if (config.getSObjectQuery() != null) {
            // Note that we don't lookup topic if the query is not specified
            // create REST client for PushTopic operations
            SalesforceComponent component = endpoint.getComponent();
            RestClient restClient = new DefaultRestClient(
                component.getHttpClient(),
                endpoint.getEndpointConfiguration().getApiVersion(),
                "json",
                component.getSession());
            PushTopicHelper helper = new PushTopicHelper(config, topicName, restClient);
            helper.createOrUpdateTopic();
        }

        // subscribe to topic
        subscriptionHelper.subscribe(topicName, this);
        subscribed = true;
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (subscribed) {
            // unsubscribe from topic
            subscriptionHelper.unsubscribe(topicName, this);
        }
    }

    public void processMessage(ClientSessionChannel channel, Message message) {
        final Exchange exchange = endpoint.createExchange();
        org.apache.camel.Message in = exchange.getIn();
        setHeaders(in, message);

        // get event data
        // TODO do we need to add NPE checks for message/data.get***???
        Map<String, Object> data = message.getDataAsMap();

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) data.get(EVENT_PROPERTY);
        Object eventType = event.get(TYPE_PROPERTY);
        Object createdDate = event.get(CREATED_DATE_PROPERTY);
        log.debug(String.format("Received event %s created on %s", eventType, createdDate));

        in.setHeader("CamelSalesforceEventType", eventType);
        in.setHeader("CamelSalesforceCreatedDate", createdDate);

        // get SObject
        @SuppressWarnings("unchecked")
        final Map<String, Object> sObject = (Map<String, Object>) data.get(SOBJECT_PROPERTY);
        try {

            final String sObjectString = objectMapper.writeValueAsString(sObject);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Received SObject: %s", sObjectString));
            }

            if (sObjectClass == null) {
                // return sobject map as exchange body
                in.setBody(sObject);
            } else {
                // create the expected SObject
                in.setBody(objectMapper.readValue(
                    new StringReader(sObjectString), sObjectClass));
            }
        } catch (IOException e) {
            final String msg = String.format("Error parsing message [%s] from Topic %s: %s",
                message, topicName, e.getMessage());
            log.error(msg, e);
            handleException(msg, new RuntimeCamelException(msg, e));
        }

        try {
            getProcessor().process(exchange);
        } catch (Exception e) {
            String msg = String.format("Error processing %s: %s", exchange, e.getMessage());
            log.error(msg, e);
            handleException(msg, e);
        } finally {
            Exception ex = exchange.getException();
            if (ex != null) {
                String msg = String.format("Unhandled exception: %s", ex.getMessage());
                log.error(msg, ex);
                handleException(msg, ex);
            }
        }
    }

    private void setHeaders(org.apache.camel.Message in, Message message) {
        Map<String, Object> headers = new HashMap<String, Object>();
        // set topic name
        headers.put("CamelSalesforceTopicName", topicName);
        // set message properties as headers
        headers.put("CamelSalesforceChannel", message.getChannel());
        headers.put("CamelSalesforceClientId", message.getClientId());

        in.setHeaders(headers);
    }

}
