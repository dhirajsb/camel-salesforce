package org.fusesource.camel.component.salesforce;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.camel.component.salesforce.api.dto.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SalesforceComponentTest extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceComponentTest.class);
    private static final String TEST_LOGIN_PROPERTIES = "/test-login.properties";

    private ObjectMapper objectMapper;
    private static final long TEST_TIMEOUT = 30;
    private String testId;

    @Test
    public void testGetVersions() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetVersions");
        mock.expectedMinimumMessageCount(1);

        // test versions doesn't need a body
        sendBody("direct:testGetVersions", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        List<Version> versions = ex.getIn().getBody(List.class);
        assertNotNull(versions);
        LOG.trace("Versions: {}", versions);

        // test for xml response
        mock = getMockEndpoint("mock:testGetVersionsXml");
        mock.expectedMinimumMessageCount(1);
        sendBody("direct:testGetVersionsXml", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        Versions versions1 = ex.getIn().getBody(Versions.class);
        assertNotNull(versions1);
        LOG.trace("Versions: {}", versions1);
    }

    @Test
    public void testGetResources() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetResources");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetResources", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        RestResources resources = ex.getIn().getBody(RestResources.class);
        assertNotNull(resources);
        LOG.trace("Resources: {}", resources);

        mock = getMockEndpoint("mock:testGetResourcesXml");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetResourcesXml", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        resources = ex.getIn().getBody(RestResources.class);
        assertNotNull(resources);
        LOG.trace("Resources: {}", resources);
    }

    @Test
    public void testGetGlobalObjects() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetGlobalObjects");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetGlobalObjects", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        GlobalObjects globalObjects = ex.getIn().getBody(GlobalObjects.class);
        assertNotNull(globalObjects);
        LOG.trace("GlobalObjects: {}", globalObjects);

        mock = getMockEndpoint("mock:testGetGlobalObjectsXml");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetGlobalObjectsXml", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        globalObjects = ex.getIn().getBody(GlobalObjects.class);
        assertNotNull(globalObjects);
        LOG.trace("GlobalObjects: {}", globalObjects);
    }

    @Test
    public void testGetSObjectBasicInfo() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetSObjectBasicInfo");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetSObjectBasicInfo", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        SObjectBasicInfo objectBasicInfo = ex.getIn().getBody(SObjectBasicInfo.class);
        assertNotNull(objectBasicInfo);
        LOG.trace("SObjectBasicInfo: {}", objectBasicInfo);

        mock = getMockEndpoint("mock:testGetSObjectBasicInfoXml");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetSObjectBasicInfoXml", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        objectBasicInfo = ex.getIn().getBody(SObjectBasicInfo.class);
        assertNotNull(objectBasicInfo);
        LOG.trace("SObjectBasicInfo: {}", objectBasicInfo);
    }

    @Test
    public void testGetSObjectDescription() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetSObjectDescription");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetSObjectDescription", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        SObjectDescription sObjectDescription = ex.getIn().getBody(SObjectDescription.class);
        assertNotNull(sObjectDescription);
        LOG.trace("SObjectDescription: {}", sObjectDescription);

        mock = getMockEndpoint("mock:testGetSObjectDescriptionXml");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetSObjectDescriptionXml", null);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        sObjectDescription = ex.getIn().getBody(SObjectDescription.class);
        assertNotNull(sObjectDescription);
        LOG.trace("SObjectDescription: {}", sObjectDescription);
    }

    @Test
    public void testGetSObjectById() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetSObjectById");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetSObjectById", testId);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        Merchandise__c merchandise = ex.getIn().getBody(Merchandise__c.class);
        assertNotNull(merchandise);
        assertNull(merchandise.getTotal_Inventory__c());
        LOG.trace("SObjectById: {}", merchandise);

        mock = getMockEndpoint("mock:testGetSObjectByIdXml");
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetSObjectByIdXml", testId);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        merchandise = ex.getIn().getBody(Merchandise__c.class);
        assertNotNull(merchandise);
        assertNull(merchandise.getPrice__c());
        LOG.trace("SObjectById: {}", merchandise);
    }

    @Test
    public void testCreateUpdateDeleteById() throws Exception {
        // test JSON endpoints
        doTestCreateUpdateDeleteById(false);

        // test XML endpoints
        doTestCreateUpdateDeleteById(true);
    }

    private void doTestCreateUpdateDeleteById(boolean testXml) throws InterruptedException {
        String suffix = "";
        if (testXml) {
            suffix = "Xml";
            LOG.trace("Testing JSON endpoints");
        } else {
            LOG.trace("Testing XML endpoints");
        }

        MockEndpoint mock = getMockEndpoint("mock:testCreateSObject" + suffix);
        mock.expectedMinimumMessageCount(1);

        Merchandise__c merchandise__c = new Merchandise__c();
        merchandise__c.setName("Wee Wee Wee Plane");
        merchandise__c.setDescription__c("Microlite plane");
        merchandise__c.setPrice__c(2000.0);
        merchandise__c.setTotal_Inventory__c(50.0);
        sendBody("direct:testCreateSObject" + suffix, merchandise__c);
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        assertNull(ex.getException());
        CreateSObjectResult result = ex.getIn().getBody(CreateSObjectResult.class);
        assertNotNull(result);
        assertTrue("Create successful", result.getSuccess());
        LOG.trace("CreateSObject: " + result);

        // test JSON update
        mock = getMockEndpoint("mock:testUpdateSObjectById" + suffix);
        mock.expectedMinimumMessageCount(1);

        merchandise__c = new Merchandise__c();
        // make the plane cheaper
        merchandise__c.setPrice__c(1500.0);
        // change inventory to half
        merchandise__c.setTotal_Inventory__c(25.0);
        template().sendBodyAndHeader("direct:testUpdateSObjectById" + suffix, merchandise__c, SalesforceEndpointConfig.SOBJECT_ID, result.getId());
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        // empty body and no exception
        assertNull(ex.getException());
        assertNull(ex.getOut().getBody());
        LOG.trace("UpdateSObjectById successful");

        // delete the newly created SObject
        mock = getMockEndpoint("mock:testDeleteSObjectById" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testDeleteSObjectById" + suffix, result.getId());
        mock.assertIsSatisfied(TEST_TIMEOUT, TimeUnit.SECONDS);

        // assert expected result
        ex = mock.getExchanges().get(0);
        // empty body and no exception
        assertNull(ex.getException());
        assertNull(ex.getOut().getBody());
        LOG.trace("DeleteSObjectById successful");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        // create a json mapper
        objectMapper = new ObjectMapper();

        // create the component
        SalesforceComponent component = new SalesforceComponent();
        setLoginProperties(component);

        // default component level payload format
        component.setFormat("json");
        // default api version
        component.setApiVersion("25.0");

        // add it to context
        context().addComponent("force", component);

        // create test route
        return new RouteBuilder() {
            public void configure() {

                // testGetVersion
                from("direct:testGetVersions")
                    .to("force://getVersions")
                    .to("mock:testGetVersions");

                // allow overriding format per endpoint
                from("direct:testGetVersionsXml")
                    .to("force://getVersions?format=xml")
                    .to("mock:testGetVersionsXml");

                // testGetResources
                from("direct:testGetResources")
                    .to("force://getResources")
                    .to("mock:testGetResources");

                from("direct:testGetResourcesXml")
                    .to("force://getResources?format=xml")
                    .to("mock:testGetResourcesXml");

                // testGetGlobalObjects
                from("direct:testGetGlobalObjects")
                    .to("force://getGlobalObjects")
                    .to("mock:testGetGlobalObjects");

                from("direct:testGetGlobalObjectsXml")
                    .to("force://getGlobalObjects?format=xml")
                    .to("mock:testGetGlobalObjectsXml");

                // testGetSObjectBasicInfo
                from("direct:testGetSObjectBasicInfo")
                    .to("force://getSObjectBasicInfo?sObjectName=Merchandise__c")
                    .to("mock:testGetSObjectBasicInfo");

                from("direct:testGetSObjectBasicInfoXml")
                    .to("force://getSObjectBasicInfo?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testGetSObjectBasicInfoXml");

                // testGetSObjectDescription
                from("direct:testGetSObjectDescription")
                    .to("force://getSObjectDescription?sObjectName=Merchandise__c")
                    .to("mock:testGetSObjectDescription");

                from("direct:testGetSObjectDescriptionXml")
                    .to("force://getSObjectDescription?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testGetSObjectDescriptionXml");

                // testGetSObjectById
                from("direct:testGetSObjectById")
                    .to("force://getSObjectById?sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c&sObjectFields=Description__c,Price__c")
                    .to("mock:testGetSObjectById");

                from("direct:testGetSObjectByIdXml")
                    .to("force://getSObjectById?format=xml&sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c&sObjectFields=Description__c,Total_Inventory__c")
                    .to("mock:testGetSObjectByIdXml");

                // testCreateSObject
                from("direct:testCreateSObject")
                    .to("force://createSObject?sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c")
                    .to("mock:testCreateSObject");

                from("direct:testCreateSObjectXml")
                    .to("force://createSObject?format=xml&sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c")
                    .to("mock:testCreateSObjectXml");

                // testUpdateSObjectById
                from("direct:testUpdateSObjectById")
                    .to("force://updateSObjectById?sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c")
                    .to("mock:testUpdateSObjectById");

                from("direct:testUpdateSObjectByIdXml")
                    .to("force://updateSObjectById?format=xml&sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c")
                    .to("mock:testUpdateSObjectByIdXml");

                // testDeleteSObjectById
                from("direct:testDeleteSObjectById")
                    .to("force://deleteSObjectById?sObjectName=Merchandise__c")
                    .to("mock:testDeleteSObjectById");

                from("direct:testDeleteSObjectByIdXml")
                    .to("force://deleteSObjectById?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testDeleteSObjectByIdXml");

                // testCreateOrUpdateSObjectByExternalId
                from("direct:testCreateOrUpdateSObjectByExternalId")
                    .to("force://createOrUpdateSObjectByExternalId?sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c")
                    .to("mock:testCreateOrUpdateSObjectByExternalId");

                from("direct:testCreateOrUpdateSObjectByExternalIdXml")
                    .to("force://createOrUpdateSObjectByExternalId?format=xml&sObjectName=Merchandise__c&sObjectClass=org.fusesource.camel.component.salesforce.Merchandise__c")
                    .to("mock:testCreateOrUpdateSObjectByExternalIdXml");

                // testDeleteSObjectByExternalId
                from("direct:testDeleteSObjectByExternalId")
                    .to("force://deleteSObjectByExternalId?sObjectName=Merchandise__c")
                    .to("mock:testDeleteSObjectByExternalId");

                from("direct:testDeleteSObjectByExternalIdXml")
                    .to("force://deleteSObjectByExternalId?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testDeleteSObjectByExternalIdXml");
            }
        };
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
    }

}
