package org.fusesource.camel.component.salesforce.internal.streaming;

import org.apache.camel.CamelException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.http.HttpStatus;
import org.fusesource.camel.component.salesforce.SalesforceEndpointConfig;
import org.fusesource.camel.component.salesforce.api.SalesforceException;
import org.fusesource.camel.component.salesforce.api.dto.CreateSObjectResult;
import org.fusesource.camel.component.salesforce.internal.client.RestClient;
import org.fusesource.camel.component.salesforce.internal.client.SyncResponseCallback;
import org.fusesource.camel.component.salesforce.internal.dto.PushTopic;
import org.fusesource.camel.component.salesforce.internal.dto.QueryRecordsPushTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PushTopicHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PushTopicHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PUSH_TOPIC_OBJECT_NAME = "PushTopic";
    private static final long API_TIMEOUT = 60; // Rest API call timeout
    private final SalesforceEndpointConfig config;
    private final String topicName;
    private final RestClient restClient;

    public PushTopicHelper(SalesforceEndpointConfig config, String topicName, RestClient restClient) {
        this.config = config;
        this.topicName = topicName;
        this.restClient = restClient;
    }

    public void createOrUpdateTopic() throws CamelException {
        final String query = config.getSObjectQuery();

        final SyncResponseCallback callback = new SyncResponseCallback();
        // lookup Topic first
        try {
            // use SOQL to lookup Topic, since Name is not an external ID!!!
            restClient.query("SELECT Id, Name, Query, ApiVersion, IsActive, " +
                "NotifyForFields, NotifyForOperations, Description " +
                "FROM PushTopic WHERE Name = '" + topicName + "'",
                callback);

            if (!callback.await(API_TIMEOUT, TimeUnit.SECONDS)) {
                throw new SalesforceException("API call timeout!", null);
            }
            if (callback.getException() != null) {
                throw callback.getException();
            }
            QueryRecordsPushTopic records = objectMapper.readValue(callback.getResponse(),
                QueryRecordsPushTopic.class);
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
            throw new CamelException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Un-marshaling error retrieving Topic %s: %s", topicName, e.getMessage());
            throw new CamelException(msg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = String.format("Un-marshaling error retrieving Topic %s: %s", topicName, e.getMessage());
            throw new CamelException(msg, e);
        } finally {
            // close stream to close HttpConnection
            if (callback.getResponse() != null) {
                try {
                    callback.getResponse().close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void createTopic() throws CamelException {
        final PushTopic topic = new PushTopic();
        topic.setName(topicName);
        topic.setApiVersion(Double.valueOf(config.getApiVersion()));
        topic.setQuery(config.getSObjectQuery());
        topic.setDescription("Topic created by Camel Salesforce component");
        topic.setNotifyForFields(config.getNotifyForFields());
        topic.setNotifyForOperations(config.getNotifyForOperations());

        LOG.info(String.format("Creating Topic %s: %s", topicName, topic));
        final SyncResponseCallback callback = new SyncResponseCallback();
        try {
            restClient.createSObject(PUSH_TOPIC_OBJECT_NAME,
                new ByteArrayInputStream(objectMapper.writeValueAsBytes(topic)), callback);

            if (!callback.await(API_TIMEOUT, TimeUnit.SECONDS)) {
                throw new SalesforceException("API call timeout!", null);
            }
            if (callback.getException() != null) {
                throw callback.getException();
            }

            CreateSObjectResult result = objectMapper.readValue(callback.getResponse(), CreateSObjectResult.class);
            if (!result.getSuccess()) {
                String msg = String.format("Error creating Topic %s: %s", topicName, result.getErrors());
                final SalesforceException salesforceException = new SalesforceException(result.getErrors(),
                    HttpStatus.BAD_REQUEST_400);
                throw new CamelException(msg, salesforceException);
            }
        } catch (SalesforceException e) {
            String msg = String.format("Error creating Topic %s: %s", topicName, e.getMessage());
            throw new CamelException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Un-marshaling error creating Topic %s: %s", topicName, e.getMessage());
            throw new CamelException(msg, e);
        } catch (InterruptedException e) {
            String msg = String.format("Un-marshaling error creating Topic %s: %s", topicName, e.getMessage());
            throw new CamelException(msg, e);
        } finally {
            if (callback.getResponse() != null) {
                try {
                    callback.getResponse().close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void updateTopic(String topicId) throws CamelException {
        final String query = config.getSObjectQuery();
        LOG.info(String.format("Updating Topic %s with Query [%s]", topicName, query));

        final SyncResponseCallback callback = new SyncResponseCallback();
        try {
            // update the query, notifyForFields and notifyForOperations fields
            final PushTopic topic = new PushTopic();
            topic.setQuery(query);
            topic.setNotifyForFields(config.getNotifyForFields());
            topic.setNotifyForOperations(config.getNotifyForOperations());

            restClient.updateSObject("PushTopic", topicId,
                new ByteArrayInputStream(objectMapper.writeValueAsBytes(topic)),
                callback);

            if (!callback.await(API_TIMEOUT, TimeUnit.SECONDS)) {
                throw new SalesforceException("API call timeout!", null);
            }
            if (callback.getException() != null) {
                throw callback.getException();
            }

        } catch (SalesforceException e) {
            String msg = String.format("Error updating topic %s with query [%s] : %s",
                topicName, query, e.getMessage());
            throw new CamelException(msg, e);
        } catch (InterruptedException e) {
            // reset interrupt status
            Thread.currentThread().interrupt();
            String msg = String.format("Error updating topic %s with query [%s] : %s",
                topicName, query, e.getMessage());
            throw new CamelException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Error updating topic %s with query [%s] : %s",
                topicName, query, e.getMessage());
            throw new CamelException(msg, e);
        } finally {
            if (callback.getResponse() != null) {
                try {
                    callback.getResponse().close();
                } catch (IOException ignore) {
                }
            }
        }
    }

}