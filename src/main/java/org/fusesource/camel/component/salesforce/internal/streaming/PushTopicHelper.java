package org.fusesource.camel.component.salesforce.internal.streaming;

import org.apache.camel.CamelException;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.camel.component.salesforce.SalesforceEndpointConfig;
import org.fusesource.camel.component.salesforce.api.SalesforceException;
import org.fusesource.camel.component.salesforce.api.dto.CreateSObjectResult;
import org.fusesource.camel.component.salesforce.internal.client.RestClient;
import org.fusesource.camel.component.salesforce.internal.dto.PushTopic;
import org.fusesource.camel.component.salesforce.internal.dto.QueryRecordsPushTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PushTopicHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PushTopicHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PUSH_TOPIC_OBJECT_NAME = "PushTopic";
    private final SalesforceEndpointConfig config;
    private final String topicName;
    private final RestClient restClient;

    public PushTopicHelper(SalesforceEndpointConfig config, String topicName, RestClient restClient) {
        this.config = config;
        this.topicName = topicName;
        this.restClient = restClient;
    }

    public void createOrUpdateTopic() throws CamelException {
        String query = config.getSObjectQuery();

        // lookup Topic first
        InputStream stream = null;
        try {
            // use SOQL to lookup Topic, since Name is not an external ID!!!
            stream = restClient.query("SELECT Id, Name, Query, ApiVersion, IsActive, " +
                "NotifyForFields, NotifyForOperations, Description " +
                "FROM PushTopic WHERE Name = '" + topicName + "'");
            QueryRecordsPushTopic records = objectMapper.readValue(stream, QueryRecordsPushTopic.class);
            if (records.getTotalSize() == 1) {

                PushTopic topic = records.getRecords().get(0);
                LOG.info(String.format("Found existing topic %s: %s", topicName, topic));

                // check if we need to update topic query, notifyForFields or notifyForOperations
                if (!query.equals(topic.getQuery()) ||
                    (config.getNotifyForFields() != null &&
                        !config.getNotifyForFields().equals(topic.getNotifyForFields())) ||
                    (config.getNotifyForOperations() != null &&
                        !config.getNotifyForOperations().equals(topic.getNotifyForOperations()))
                    ) {

                    if (!config.isUpdateTopic()) {
                        String msg = "Query doesn't match existing Topic and updateTopic is set to false";
                        LOG.error(msg);
                        throw new CamelException(msg);
                    }

                    // otherwise update the topic
                    updateTopic(topic.getId());
                }

            } else {
                createTopic();
            }

        } catch (SalesforceException e) {
            String msg = String.format("Error retrieving Topic %s: %s", topicName, e.getMessage());
            LOG.error(msg, e);
            throw new CamelException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Un-marshaling error retrieving Topic %s: %s", topicName, e.getMessage());
            LOG.error(msg, e);
            throw new CamelException(msg, e);
        } finally {
            // close stream to close HttpConnection
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void createTopic() throws CamelException {
        PushTopic topic = new PushTopic();
        topic.setName(topicName);
        topic.setApiVersion(Double.valueOf(config.getApiVersion()));
        topic.setQuery(config.getSObjectQuery());
        topic.setDescription("Topic created by Camel Salesforce component");
        topic.setNotifyForFields(config.getNotifyForFields());
        topic.setNotifyForOperations(config.getNotifyForOperations());

        LOG.info(String.format("Creating Topic %s: %s", topicName, topic));
        InputStream stream = null;
        try {
            stream = restClient.createSObject(PUSH_TOPIC_OBJECT_NAME,
                new ByteArrayInputStream(objectMapper.writeValueAsBytes(topic)));
            CreateSObjectResult result = objectMapper.readValue(stream, CreateSObjectResult.class);
            if (!result.getSuccess()) {
                String msg = String.format("Error creating Topic %s: %s", topicName, result.getErrors());
                final SalesforceException salesforceException = new SalesforceException(result.getErrors(), HttpStatus.SC_BAD_REQUEST);
                LOG.error(msg, salesforceException);
                throw new CamelException(msg, salesforceException);
            }
        } catch (SalesforceException e) {
            String msg = String.format("Error creating Topic %s: %s", topicName, e.getMessage());
            LOG.error(msg, e);
            throw new CamelException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Un-marshaling error creating Topic %s: %s", topicName, e.getMessage());
            LOG.error(msg, e);
            throw new CamelException(msg, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void updateTopic(String topicId) throws CamelException {
        String query = config.getSObjectQuery();
        LOG.info(String.format("Updating Topic %s with Query [%s]", topicName, query));

        try {
            // update the query, notifyForFields and notifyForOperations fields
            PushTopic topic = new PushTopic();
            topic.setQuery(query);
            topic.setNotifyForFields(config.getNotifyForFields());
            topic.setNotifyForOperations(config.getNotifyForOperations());

            restClient.updateSObject("PushTopic", topicId,
                new ByteArrayInputStream(objectMapper.writeValueAsBytes(topic)));
        } catch (SalesforceException e) {
            String msg = String.format("Error updating topic %s with query [%s] : %s",
                topicName, query, e.getMessage());
            LOG.error(msg, e);
            throw new CamelException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Marshaling error updating topic %s with query [%s] : %s",
                topicName, query, e.getMessage());
            LOG.error(msg, e);
            throw new CamelException(msg, e);
        }
    }

}