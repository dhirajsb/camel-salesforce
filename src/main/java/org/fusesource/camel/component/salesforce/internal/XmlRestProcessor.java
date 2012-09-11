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
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.http.Consts;
import org.fusesource.camel.component.salesforce.SalesforceEndpoint;
import org.fusesource.camel.component.salesforce.api.JodaTimeConverter;
import org.fusesource.camel.component.salesforce.api.RestException;
import org.fusesource.camel.component.salesforce.api.dto.*;

import java.io.*;

import static org.fusesource.camel.component.salesforce.SalesforceEndpointConfig.SOBJECT_NAME;

public class XmlRestProcessor extends AbstractRestProcessor {

    // although XStream is generally thread safe, because of the way we use aliases
    // for GET_BASIC_INFO and GET_DESCRIPTION, we need to use a ThreadLocal
    // not very efficient when both JSON and XML are used together with a single Thread pool
    // but this will do for now
    private static ThreadLocal<XStream> xStream =
        new ThreadLocal<XStream>() {
            @Override
            protected XStream initialValue() {
                // use NoNameCoder to avoid escaping __ in custom field names
                // and CompactWriter to avoid pretty printing
                XStream result = new XStream(new XppDriver(new NoNameCoder()) {
                    @Override
                    public HierarchicalStreamWriter createWriter(Writer out) {
                        return new CompactWriter(out, getNameCoder());
                    }

                });
                result.registerConverter(new JodaTimeConverter());
                return result;
            }
        };

    private static final String RESPONSE_ALIAS = XmlRestProcessor.class.getName() + ".responseAlias";

    public XmlRestProcessor(SalesforceEndpoint endpoint) {
        super(endpoint);

    }

    @Override
    protected void processRequest(Exchange exchange) throws RestException {

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

            case GET_BASIC_INFO:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectBasicInfo.class);

                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    getParameter(SOBJECT_NAME, exchange, USE_IN_BODY, NOT_OPTIONAL));
                break;

            case GET_DESCRIPTION:
                // handle in built response types
                exchange.setProperty(RESPONSE_CLASS, SObjectDescription.class);

                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    getParameter(SOBJECT_NAME, exchange, USE_IN_BODY, NOT_OPTIONAL));
                break;

            case GET_SOBJECT:
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    getParameter(SOBJECT_NAME, exchange, IGNORE_IN_BODY, NOT_OPTIONAL));
                break;

            case CREATE_SOBJECT:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            case GET_SOBJECT_WITH_ID:
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    getParameter(SOBJECT_NAME, exchange, IGNORE_IN_BODY, NOT_OPTIONAL));
                break;

            case UPSERT_SOBJECT:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            case QUERY:
            case QUERY_MORE:
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS,
                    "QueryResult");
                break;

            case SEARCH:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, SearchResults.class);
                break;

        }

    }

    protected InputStream getRequestStream(Exchange exchange) throws RestException {
        try {
            // get request stream from In message
            Message in = exchange.getIn();
            InputStream request = in.getBody(InputStream.class);
            if (request == null) {
                AbstractSObjectBase sObject = in.getBody(AbstractSObjectBase.class);
                if (sObject != null) {
                    // marshall the SObject
                    // first process annotations on the class, for things like alias, etc.
                    xStream.get().processAnnotations(sObject.getClass());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    // make sure we write the XML with the right encoding
                    xStream.get().toXML(sObject, new OutputStreamWriter(out, Consts.UTF_8));
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
        } catch (XStreamException e) {
            String msg = "Error marshaling request: " + e.getMessage();
            throw new RestException(msg, e);
        }
    }

    @Override
    protected void processResponse(Exchange exchange, InputStream responseEntity) throws RestException {
        try {
            // do we need to un-marshal a response
            if (responseEntity != null) {
                final Class<?> responseClass = exchange.getProperty(RESPONSE_CLASS, Class.class);
                // its ok to call this multiple times, as xstream ignores duplicate calls
                xStream.get().processAnnotations(responseClass);
                final String responseAlias = exchange.getProperty(RESPONSE_ALIAS, String.class);
                if (responseAlias != null) {
                    xStream.get().alias(responseAlias, responseClass);
                }
                Object response = responseClass.newInstance();
                xStream.get().fromXML(responseEntity, response);
                exchange.getOut().setBody(response);
            }
            // copy headers and attachments
            exchange.getOut().getHeaders().putAll(exchange.getIn().getHeaders());
            exchange.getOut().getAttachments().putAll(exchange.getIn().getAttachments());
        } catch (XStreamException e) {
            String msg = "Error parsing XML response: " + e.getMessage();
            throw new RestException(msg, e);
        } catch (Exception e) {
            String msg = "Error creating XML response: " + e.getMessage();
            throw new RestException(msg, e);
        }
    }

}
