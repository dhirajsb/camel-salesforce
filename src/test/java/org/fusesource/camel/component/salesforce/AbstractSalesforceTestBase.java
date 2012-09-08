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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class AbstractSalesforceTestBase extends CamelTestSupport {
    private static final String TEST_LOGIN_PROPERTIES = "/test-login.properties";
    private static final String API_VERSION = "25.0";
    private static final String DEFAULT_FORMAT = "json";
    protected static String testId;

    @Override
    public boolean isCreateCamelContextPerClass() {
        // only create the context once for this class
        return true;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        // create the test component
        createComponent();

        return doCreateRouteBuilder();
    }

    protected abstract RouteBuilder doCreateRouteBuilder() throws Exception;

    protected void createComponent() throws IllegalAccessException, IOException {
        // create the component
        SalesforceComponent component = new SalesforceComponent();
        setLoginProperties(component);

        // default component level payload format
        component.setFormat(DEFAULT_FORMAT);
        // default api version
        component.setApiVersion(API_VERSION);
        // set DTO package
        component.setPackages(new String[] {
            Merchandise__c.class.getPackage().getName()
        });

        // add it to context
        context().addComponent("force", component);
    }

    private void setLoginProperties(SalesforceComponent component) throws IllegalAccessException, IOException {
        // load test-login properties
        Properties properties = new Properties();
        InputStream stream = getClass().getResourceAsStream(TEST_LOGIN_PROPERTIES);
        if (null == stream) {
            throw new IllegalAccessException("Create a properties file named " +
                TEST_LOGIN_PROPERTIES + " with clientId, clientSecret, userName, password and a testId" +
                " for a Salesforce account with the Merchandise object from Salesforce Guides.");
        }
        properties.load(stream);
        component.setClientId(properties.getProperty("clientId"));
        component.setClientSecret(properties.getProperty("clientSecret"));
        component.setUserName(properties.getProperty("userName"));
        component.setPassword(properties.getProperty("password"));

        testId = properties.getProperty("testId");

        assertNotNull("Null clientId", component.getClientId());
        assertNotNull("Null clientSecret", component.getClientSecret());
        assertNotNull("Null userName", component.getUserName());
        assertNotNull("Null password", component.getPassword());

        assertNotNull("Null testId", testId);
    }
}
