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

import org.apache.camel.CamelException;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.SynchronousDelegateProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Salesforce endpoint.
 */
public class SalesforceEndpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceEndpoint.class);

    public SalesforceEndpoint(String uri, SalesforceComponent salesforceComponent) {
        super(uri, salesforceComponent);
    }

    public Producer createProducer() throws Exception {
        // producer requires an operation, topicName must be the invalid operation name
        final SalesforceEndpointConfig config = getEndpointConfiguration();
        if (config.getOperationName() == null) {
            String msg = String.format("Invalid Operation %s", config.getTopicName());
            LOG.error(msg);
            throw new CamelException(msg);
        }

        SalesforceProducer producer = new SalesforceProducer(this);
        if (isSynchronous()) {
            return new SynchronousDelegateProducer(producer);
        } else {
            return producer;
        }
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        // consumer requires a topicName, operation name must be the invalid topic name
        final SalesforceEndpointConfig config = getEndpointConfiguration();
        if (config.getTopicName() == null) {
            String msg = String.format("Invalid topic name %s, matches a producer operation name",
                config.getOperationName().value());
            LOG.error(msg);
            throw new CamelException(msg);
        }

        return new SalesforceConsumer(this, processor);
    }

    @Override
    public SalesforceComponent getComponent() {
        return (SalesforceComponent) super.getComponent();
    }

    public boolean isSingleton() {
        return false;
    }

    @Override
    public SalesforceEndpointConfig getEndpointConfiguration() {
        return (SalesforceEndpointConfig) super.getEndpointConfiguration();
    }

}
