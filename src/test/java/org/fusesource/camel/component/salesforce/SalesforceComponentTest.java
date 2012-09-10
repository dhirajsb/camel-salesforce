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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.camel.component.salesforce.api.dto.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SalesforceComponentTest extends AbstractSalesforceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceComponentTest.class);
    private static final String TEST_LINE_ITEM_ID = "1";
    private static final String NEW_LINE_ITEM_ID = "100";

    private ObjectMapper objectMapper;

    @Test
    public void testGetVersions() throws Exception {
        doTestGetVersions("");
        doTestGetVersions("Xml");
    }

    private void doTestGetVersions(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetVersions" + suffix);
        mock.expectedMinimumMessageCount(1);

        // test getVersions doesn't need a body
        sendBody("direct:testGetVersions" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        List<Version> versions = null;
        Versions versions1 = ex.getIn().getBody(Versions.class);
        if (versions1 == null) {
            versions = ex.getIn().getBody(List.class);
        } else {
            versions = versions1.getVersions();
        }
        assertNotNull(versions);
        LOG.trace("Versions: {}", versions);
    }

    @Test
    public void testGetResources() throws Exception {
        doTestGetResources("");
        doTestGetResources("Xml");
    }

    private void doTestGetResources(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetResources" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetResources" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        RestResources resources = ex.getIn().getBody(RestResources.class);
        assertNotNull(resources);
        LOG.trace("Resources: {}", resources);
    }

    @Test
    public void testGetGlobalObjects() throws Exception {
        doTestGetGlobalObjects("");
        doTestGetGlobalObjects("Xml");
    }

    private void doTestGetGlobalObjects(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetGlobalObjects" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetGlobalObjects" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        GlobalObjects globalObjects = ex.getIn().getBody(GlobalObjects.class);
        assertNotNull(globalObjects);
        LOG.trace("GlobalObjects: {}", globalObjects);
    }

    @Test
    public void testGetBasicInfo() throws Exception {
        doTestGetBasicInfo("");
        doTestGetBasicInfo("Xml");
    }

    private void doTestGetBasicInfo(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetBasicInfo" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetBasicInfo" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        SObjectBasicInfo objectBasicInfo = ex.getIn().getBody(SObjectBasicInfo.class);
        assertNotNull(objectBasicInfo);
        LOG.trace("SObjectBasicInfo: {}", objectBasicInfo);
    }

    @Test
    public void testGetDescription() throws Exception {
        doTestGetDescription("");
        doTestGetDescription("Xml");
    }

    private void doTestGetDescription(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetDescription" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetDescription" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        SObjectDescription sObjectDescription = ex.getIn().getBody(SObjectDescription.class);
        assertNotNull(sObjectDescription);
        LOG.trace("SObjectDescription: {}", sObjectDescription);
    }

    @Test
    public void testGetSObject() throws Exception {
        doTestGetSObject("");
        doTestGetSObject("Xml");
    }

    private void doTestGetSObject(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGetSObject" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGetSObject" + suffix, testId);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        Merchandise__c merchandise = ex.getIn().getBody(Merchandise__c.class);
        assertNotNull(merchandise);
        if (suffix.isEmpty()) {
            assertNull(merchandise.getTotal_Inventory__c());
            assertNotNull(merchandise.getPrice__c());
        } else {
            assertNotNull(merchandise.getTotal_Inventory__c());
            assertNull(merchandise.getPrice__c());
        }
        LOG.trace("SObjectById: {}", merchandise);
    }

    @Test
    public void testCreateUpdateDelete() throws Exception {
        doTestCreateUpdateDelete("");
        doTestCreateUpdateDelete("Xml");
    }

    private void doTestCreateUpdateDelete(String suffix) throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:testCreateSObject" + suffix);
        mock.expectedMinimumMessageCount(1);

        Merchandise__c merchandise__c = new Merchandise__c();
        merchandise__c.setName("Wee Wee Wee Plane");
        merchandise__c.setDescription__c("Microlite plane");
        merchandise__c.setPrice__c(2000.0);
        merchandise__c.setTotal_Inventory__c(50.0);
        sendBody("direct:testCreateSObject" + suffix, merchandise__c);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        assertNull(ex.getException());
        CreateSObjectResult result = ex.getIn().getBody(CreateSObjectResult.class);
        assertNotNull(result);
        assertTrue("Create success", result.getSuccess());
        LOG.trace("Create: " + result);

        // test JSON update
        mock = getMockEndpoint("mock:testUpdateSObject" + suffix);
        mock.expectedMinimumMessageCount(1);

        merchandise__c = new Merchandise__c();
        // make the plane cheaper
        merchandise__c.setPrice__c(1500.0);
        // change inventory to half
        merchandise__c.setTotal_Inventory__c(25.0);
        template().sendBodyAndHeader("direct:testUpdateSObject" + suffix,
            merchandise__c, SalesforceEndpointConfig.SOBJECT_ID, result.getId());
        mock.assertIsSatisfied();

        // assert expected result
        ex = mock.getExchanges().get(0);
        // empty body and no exception
        assertNull(ex.getException());
        assertNull(ex.getOut().getBody());
        LOG.trace("Update successful");

        // delete the newly created SObject
        mock = getMockEndpoint("mock:testDeleteSObject" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testDeleteSObject" + suffix, result.getId());
        mock.assertIsSatisfied();

        // assert expected result
        ex = mock.getExchanges().get(0);
        // empty body and no exception
        assertNull(ex.getException());
        assertNull(ex.getOut().getBody());
        LOG.trace("Delete successful");
    }

    @Test
    public void testCreateUpdateDeleteWithId() throws Exception {
        doTestCreateUpdateDeleteWithId("");
        doTestCreateUpdateDeleteWithId("Xml");
    }

    private void doTestCreateUpdateDeleteWithId(String suffix) throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:testGetSObjectWithId" + suffix);
        mock.expectedMinimumMessageCount(1);

        // get line item with Name 1
        sendBody("direct:testGetSObjectWithId" + suffix, TEST_LINE_ITEM_ID);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        assertNull(ex.getException());
        Line_Item__c line_item__c = ex.getIn().getBody(Line_Item__c.class);
        assertNotNull(line_item__c);
        LOG.trace("GetWithId: {}", line_item__c);

        // test JSON update
        mock = getMockEndpoint("mock:testUpsertSObject" + suffix);
        mock.expectedMinimumMessageCount(1);

        // change line_item__c to create a new Line Item
        // otherwise we will get an error from Salesforce
        line_item__c.clearBaseFields();
        // set the unit price and sold
        line_item__c.setUnit_Price__c(1000.0);
        line_item__c.setUnits_Sold__c(50.0);
        // update line item with Name NEW_LINE_ITEM_ID
        template().sendBodyAndHeader("direct:testUpsertSObject" + suffix,
            line_item__c, SalesforceEndpointConfig.SOBJECT_EXT_ID_VALUE, NEW_LINE_ITEM_ID);
        mock.assertIsSatisfied();

        // assert expected result
        ex = mock.getExchanges().get(0);
        CreateSObjectResult result = ex.getIn().getBody(CreateSObjectResult.class);
        assertNotNull(result);
        assertTrue(result.getSuccess());
        LOG.trace("CreateWithId: {}", result);

        // change line_item__c to update existing Line Item
        // otherwise we will get an error from Salesforce
        line_item__c.clearBaseFields();
        // clear read only parent type fields
        line_item__c.setInvoice_Statement__c(null);
        line_item__c.setMerchandise__c(null);
        // change the units sold
        line_item__c.setUnits_Sold__c(25.0);

        // update line item with Name NEW_LINE_ITEM_ID
        template().sendBodyAndHeader("direct:testUpsertSObject" + suffix,
            line_item__c, SalesforceEndpointConfig.SOBJECT_EXT_ID_VALUE, NEW_LINE_ITEM_ID);
        mock.assertIsSatisfied();

        // assert expected result
        ex = mock.getExchanges().get(0);
        result = ex.getIn().getBody(CreateSObjectResult.class);
        assertNotNull(result);
        assertTrue(result.getSuccess());
        LOG.trace("UpdateWithId: {}", result);

        // delete the SObject with Name=2
        mock = getMockEndpoint("mock:testDeleteSObjectWithId" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testDeleteSObjectWithId" + suffix, NEW_LINE_ITEM_ID);
        mock.assertIsSatisfied();

        // assert expected result
        ex = mock.getExchanges().get(0);
        // empty body and no exception
        assertNull(ex.getException());
        assertNull(ex.getOut().getBody());
        LOG.trace("DeleteWithId successful");
    }

    @Test
    public void testQuery() throws Exception {
        doTestQuery("");
        doTestQuery("Xml");
    }

    private void doTestQuery(String suffix) throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:testQuery" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testQuery" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        assertNull(ex.getException());
        QueryRecordsLine_Item__c queryRecords = ex.getIn().getBody(QueryRecordsLine_Item__c.class);
        assertNotNull(queryRecords);
        LOG.trace("ExecuteQuery: {}", queryRecords);
    }


    @Test
    public void testSearch() throws Exception {
        doTestSearch("");
        doTestSearch("Xml");
    }

    private void doTestSearch(String suffix) throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:testSearch" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testSearch" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        assertNull(ex.getException());
        SearchResults queryRecords = ex.getIn().getBody(SearchResults.class);
        List<SearchResult> searchResults = null;
        if (queryRecords != null) {
            searchResults = queryRecords.getResults();
        } else {
            searchResults = ex.getIn().getBody(List.class);
        }
        assertNotNull(searchResults);
        LOG.trace("ExecuteSearch: {}", searchResults);
    }

    @Override
    protected RouteBuilder doCreateRouteBuilder() throws Exception {

        // create a json mapper
        objectMapper = new ObjectMapper();

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

                // testGetBasicInfo
                from("direct:testGetBasicInfo")
                    .to("force://getBasicInfo?sObjectName=Merchandise__c")
                    .to("mock:testGetBasicInfo");

                from("direct:testGetBasicInfoXml")
                    .to("force://getBasicInfo?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testGetBasicInfoXml");

                // testGetDescription
                from("direct:testGetDescription")
                    .to("force://getDescription?sObjectName=Merchandise__c")
                    .to("mock:testGetDescription");

                from("direct:testGetDescriptionXml")
                    .to("force://getDescription?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testGetDescriptionXml");

                // testGetSObject
                from("direct:testGetSObject")
                    .to("force://getSObject?sObjectName=Merchandise__c&sObjectFields=Description__c,Price__c")
                    .to("mock:testGetSObject");

                from("direct:testGetSObjectXml")
                    .to("force://getSObject?format=xml&sObjectName=Merchandise__c&sObjectFields=Description__c,Total_Inventory__c")
                    .to("mock:testGetSObjectXml");

                // testCreateSObject
                from("direct:testCreateSObject")
                    .to("force://createSObject?sObjectName=Merchandise__c")
                    .to("mock:testCreateSObject");

                from("direct:testCreateSObjectXml")
                    .to("force://createSObject?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testCreateSObjectXml");

                // testUpdateSObject
                from("direct:testUpdateSObject")
                    .to("force://updateSObject?sObjectName=Merchandise__c")
                    .to("mock:testUpdateSObject");

                from("direct:testUpdateSObjectXml")
                    .to("force://updateSObject?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testUpdateSObjectXml");

                // testDeleteSObject
                from("direct:testDeleteSObject")
                    .to("force://deleteSObject?sObjectName=Merchandise__c")
                    .to("mock:testDeleteSObject");

                from("direct:testDeleteSObjectXml")
                    .to("force://deleteSObject?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testDeleteSObjectXml");

                // testGetSObjectWithId
                from("direct:testGetSObjectWithId")
                    .to("force://getSObjectWithId?sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testGetSObjectWithId");

                from("direct:testGetSObjectWithIdXml")
                    .to("force://getSObjectWithId?format=xml&sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testGetSObjectWithIdXml");

                // testUpsertSObject
                from("direct:testUpsertSObject")
                    .to("force://upsertSObject?sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testUpsertSObject");

                from("direct:testUpsertSObjectXml")
                    .to("force://upsertSObject?format=xml&sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testUpsertSObjectXml");

                // testDeleteSObjectWithId
                from("direct:testDeleteSObjectWithId")
                    .to("force://deleteSObjectWithId?sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testDeleteSObjectWithId");

                from("direct:testDeleteSObjectWithIdXml")
                    .to("force://deleteSObjectWithId?format=xml&sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testDeleteSObjectWithIdXml");

                // testQuery
                from("direct:testQuery")
                    .to("force://query?sObjectQuery=SELECT name from Line_Item__c&sObjectClass=org.fusesource.camel.component.salesforce.QueryRecordsLine_Item__c")
                    .to("mock:testQuery");

                from("direct:testQueryXml")
                    .to("force://query?format=xml&sObjectQuery=SELECT name from Line_Item__c&sObjectClass=org.fusesource.camel.component.salesforce.QueryRecordsLine_Item__c")
                    .to("mock:testQueryXml");

                // testSearch
                from("direct:testSearch")
                    .to("force://search?sObjectSearch=FIND {Wee}")
                    .to("mock:testSearch");

                from("direct:testSearchXml")
                    .to("force://search?format=xml&sObjectSearch=FIND {Wee}")
                    .to("mock:testSearchXml");
            }
        };
    }

}
