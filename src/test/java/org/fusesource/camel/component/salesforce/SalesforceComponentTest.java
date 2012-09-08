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
    public void testVersions() throws Exception {
        doTestVersions("");
        doTestVersions("Xml");
    }

    private void doTestVersions(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testVersions" + suffix);
        mock.expectedMinimumMessageCount(1);

        // test versions doesn't need a body
        sendBody("direct:testVersions" + suffix, null);
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
    public void testResources() throws Exception {
        doTestResources("");
        doTestResources("Xml");
    }

    private void doTestResources(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testResources" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testResources" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        RestResources resources = ex.getIn().getBody(RestResources.class);
        assertNotNull(resources);
        LOG.trace("Resources: {}", resources);
    }

    @Test
    public void testGlobalObjects() throws Exception {
        doTestGlobalObjects("");
        doTestGlobalObjects("Xml");
    }

    private void doTestGlobalObjects(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testGlobalObjects" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testGlobalObjects" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        GlobalObjects globalObjects = ex.getIn().getBody(GlobalObjects.class);
        assertNotNull(globalObjects);
        LOG.trace("GlobalObjects: {}", globalObjects);
    }

    @Test
    public void testBasicInfo() throws Exception {
        doTestBasicInfo("");
        doTestBasicInfo("Xml");
    }

    private void doTestBasicInfo(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testBasicInfo" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testBasicInfo" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        SObjectBasicInfo objectBasicInfo = ex.getIn().getBody(SObjectBasicInfo.class);
        assertNotNull(objectBasicInfo);
        LOG.trace("SObjectBasicInfo: {}", objectBasicInfo);
    }

    @Test
    public void testDescription() throws Exception {
        doTestDescription("");
        doTestDescription("Xml");
    }

    private void doTestDescription(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testDescription" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testDescription" + suffix, null);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        SObjectDescription sObjectDescription = ex.getIn().getBody(SObjectDescription.class);
        assertNotNull(sObjectDescription);
        LOG.trace("SObjectDescription: {}", sObjectDescription);
    }

    @Test
    public void testRetrieve() throws Exception {
        doTestRetrieve("");
        doTestRetrieve("Xml");
    }

    private void doTestRetrieve(String suffix) throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:testRetrieve" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testRetrieve" + suffix, testId);
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
        MockEndpoint mock = getMockEndpoint("mock:testCreate" + suffix);
        mock.expectedMinimumMessageCount(1);

        Merchandise__c merchandise__c = new Merchandise__c();
        merchandise__c.setName("Wee Wee Wee Plane");
        merchandise__c.setDescription__c("Microlite plane");
        merchandise__c.setPrice__c(2000.0);
        merchandise__c.setTotal_Inventory__c(50.0);
        sendBody("direct:testCreate" + suffix, merchandise__c);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        assertNull(ex.getException());
        CreateSObjectResult result = ex.getIn().getBody(CreateSObjectResult.class);
        assertNotNull(result);
        assertTrue("Create success", result.getSuccess());
        LOG.trace("Create: " + result);

        // test JSON update
        mock = getMockEndpoint("mock:testUpdate" + suffix);
        mock.expectedMinimumMessageCount(1);

        merchandise__c = new Merchandise__c();
        // make the plane cheaper
        merchandise__c.setPrice__c(1500.0);
        // change inventory to half
        merchandise__c.setTotal_Inventory__c(25.0);
        template().sendBodyAndHeader("direct:testUpdate" + suffix,
            merchandise__c, SalesforceEndpointConfig.SOBJECT_ID, result.getId());
        mock.assertIsSatisfied();

        // assert expected result
        ex = mock.getExchanges().get(0);
        // empty body and no exception
        assertNull(ex.getException());
        assertNull(ex.getOut().getBody());
        LOG.trace("Update successful");

        // delete the newly created SObject
        mock = getMockEndpoint("mock:testDelete" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testDelete" + suffix, result.getId());
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
        MockEndpoint mock = getMockEndpoint("mock:testRetrieveWithId" + suffix);
        mock.expectedMinimumMessageCount(1);

        // get line item with Name 1
        sendBody("direct:testRetrieveWithId" + suffix, TEST_LINE_ITEM_ID);
        mock.assertIsSatisfied();

        // assert expected result
        Exchange ex = mock.getExchanges().get(0);
        assertNull(ex.getException());
        Line_Item__c line_item__c = ex.getIn().getBody(Line_Item__c.class);
        assertNotNull(line_item__c);
        LOG.trace("RetrieveWithId: {}", line_item__c);

        // test JSON update
        mock = getMockEndpoint("mock:testUpsert" + suffix);
        mock.expectedMinimumMessageCount(1);

        // change line_item__c to create a new Line Item
        // otherwise we will get an error from Salesforce
        line_item__c.clearBaseFields();
        // set the unit price and sold
        line_item__c.setUnit_Price__c(1000.0);
        line_item__c.setUnits_Sold__c(50.0);
        // update line item with Name NEW_LINE_ITEM_ID
        template().sendBodyAndHeader("direct:testUpsert" + suffix,
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
        template().sendBodyAndHeader("direct:testUpsert" + suffix,
            line_item__c, SalesforceEndpointConfig.SOBJECT_EXT_ID_VALUE, NEW_LINE_ITEM_ID);
        mock.assertIsSatisfied();

        // assert expected result
        ex = mock.getExchanges().get(0);
        result = ex.getIn().getBody(CreateSObjectResult.class);
        assertNotNull(result);
        assertTrue(result.getSuccess());
        LOG.trace("UpdateWithId: {}", result);

        // delete the SObject with Name=2
        mock = getMockEndpoint("mock:testDeleteWithId" + suffix);
        mock.expectedMinimumMessageCount(1);

        sendBody("direct:testDeleteWithId" + suffix, NEW_LINE_ITEM_ID);
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
                from("direct:testVersions")
                    .to("force://versions")
                    .to("mock:testVersions");

                // allow overriding format per endpoint
                from("direct:testVersionsXml")
                    .to("force://versions?format=xml")
                    .to("mock:testVersionsXml");

                // testResources
                from("direct:testResources")
                    .to("force://resources")
                    .to("mock:testResources");

                from("direct:testResourcesXml")
                    .to("force://resources?format=xml")
                    .to("mock:testResourcesXml");

                // testGlobalObjects
                from("direct:testGlobalObjects")
                    .to("force://globalObjects")
                    .to("mock:testGlobalObjects");

                from("direct:testGlobalObjectsXml")
                    .to("force://globalObjects?format=xml")
                    .to("mock:testGlobalObjectsXml");

                // testBasicInfo
                from("direct:testBasicInfo")
                    .to("force://basicInfo?sObjectName=Merchandise__c")
                    .to("mock:testBasicInfo");

                from("direct:testBasicInfoXml")
                    .to("force://basicInfo?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testBasicInfoXml");

                // testDescription
                from("direct:testDescription")
                    .to("force://description?sObjectName=Merchandise__c")
                    .to("mock:testDescription");

                from("direct:testDescriptionXml")
                    .to("force://description?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testDescriptionXml");

                // testRetrieve
                from("direct:testRetrieve")
                    .to("force://retrieve?sObjectName=Merchandise__c&sObjectFields=Description__c,Price__c")
                    .to("mock:testRetrieve");

                from("direct:testRetrieveXml")
                    .to("force://retrieve?format=xml&sObjectName=Merchandise__c&sObjectFields=Description__c,Total_Inventory__c")
                    .to("mock:testRetrieveXml");

                // testCreateSObject
                from("direct:testCreate")
                    .to("force://create?sObjectName=Merchandise__c")
                    .to("mock:testCreate");

                from("direct:testCreateXml")
                    .to("force://create?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testCreateXml");

                // testUpdate
                from("direct:testUpdate")
                    .to("force://update?sObjectName=Merchandise__c")
                    .to("mock:testUpdate");

                from("direct:testUpdateXml")
                    .to("force://update?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testUpdateXml");

                // testDelete
                from("direct:testDelete")
                    .to("force://delete?sObjectName=Merchandise__c")
                    .to("mock:testDelete");

                from("direct:testDeleteXml")
                    .to("force://delete?format=xml&sObjectName=Merchandise__c")
                    .to("mock:testDeleteXml");

                // testRetrieveWithId
                from("direct:testRetrieveWithId")
                    .to("force://retrieveWithId?sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testRetrieveWithId");

                from("direct:testRetrieveWithIdXml")
                    .to("force://retrieveWithId?format=xml&sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testRetrieveWithIdXml");

                // testUpsert
                from("direct:testUpsert")
                    .to("force://upsert?sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testUpsert");

                from("direct:testUpsertXml")
                    .to("force://upsert?format=xml&sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testUpsertXml");

                // testDeleteWithId
                from("direct:testDeleteWithId")
                    .to("force://deleteWithId?sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testDeleteWithId");

                from("direct:testDeleteWithIdXml")
                    .to("force://deleteWithId?format=xml&sObjectName=Line_Item__c&sObjectIdName=Name")
                    .to("mock:testDeleteWithIdXml");

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
