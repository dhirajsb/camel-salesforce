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
package org.fusesource.camel.component.salesforce.internal.streaming;

import org.apache.camel.CamelException;
import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.fusesource.camel.component.salesforce.SalesforceComponent;
import org.fusesource.camel.component.salesforce.SalesforceConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SubscriptionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionHelper.class);
    private static final String SUMMER_11 = "22.0";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 20 * 1000;
    private static final long DEFAULT_READ_TIMEOUT = 120 * 1000;
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60 * 1000;
    private static final int HANDSHAKE_TIMEOUT = 10 * 1000;
    private static final String EXCEPTION_FIELD = "exception";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SalesforceComponent component;
    private final BayeuxClient client;
    private final boolean isSummer11;

    private String handshakeError;
    private Exception handshakeException;

    private final Map<SalesforceConsumer, ClientSessionChannel.MessageListener> listenerMap;

    public SubscriptionHelper(SalesforceComponent component) throws Exception {
        this.component = component;
        this.listenerMap = new HashMap<SalesforceConsumer, ClientSessionChannel.MessageListener>();

        this.isSummer11 = component.getApiVersion().equals(SUMMER_11);

        // create CometD client
        this.client = createClient();

        // add META channel listeners
        addMetaListeners();
        // connect to Salesforce cometd endpoint
        client.handshake();
        if (!client.waitFor(HANDSHAKE_TIMEOUT, BayeuxClient.State.CONNECTED)) {
            if (handshakeException != null) {
                String msg = String.format("Exception during HANDSHAKE: %s", handshakeException.getMessage());
                LOG.error(msg, handshakeException);
                throw new CamelException(msg, handshakeException);
            } else {
                String msg = String.format("Error during HANDSHAKE: %s", handshakeError);
                LOG.error(msg);
                throw new CamelException(msg);
            }
        }
    }

    private void addMetaListeners() {

        // listen for handshake error or exception
        client.getChannel(Channel.META_HANDSHAKE).addListener
            (new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    LOG.info(String.format("[CHANNEL:META_HANDSHAKE]: %s", message));
                    boolean success = message.isSuccessful();
                    if (!success) {
                        String error = (String) message.get(Message.ERROR_FIELD);
                        if (error != null) {
                            handshakeError = error;
                        }
                        Exception exception = (Exception) message.get(EXCEPTION_FIELD);
                        if (exception != null) {
                            handshakeException = exception;
                        }
                    }
                }
            });

        // listen for connect error
        client.getChannel(Channel.META_CONNECT).addListener(
            new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    LOG.info("[CHANNEL:META_CONNECT]: " + message);
                    boolean success = message.isSuccessful();
                    if (!success) {
                        String error = (String) message.get(Message.ERROR_FIELD);
                        if (error != null) {
                            LOG.error(String.format("Error during CONNECT: %s", error));
                        }
                    }
                }
            });

        // listen for subscribe error
        client.getChannel(Channel.META_SUBSCRIBE).addListener(
            new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    LOG.info("[CHANNEL:META_SUBSCRIBE]: " + message);
                    boolean success = message.isSuccessful();
                    if (!success) {
                        String error = (String) message.get(Message.ERROR_FIELD);
                        if (error != null) {
                            // we are only interested in the last segment
                            // which is the Salesforce topic name
                            final ChannelId channelId = channel.getChannelId();
                            String topicName = channelId.getSegment(channelId.depth() - 1);
                            LOG.error(String.format("Error during SUBSCRIBE for %s: %s", topicName, error));
                        }
                    }
                }
            });
    }

    private BayeuxClient createClient() throws Exception {
        // TODO change SalesforceComponent to use Jetty client instead of Apache HttpClient
//        HttpClient httpClient = component.getHttpClient();
        final HttpClient httpClient = new HttpClient();
        httpClient.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
        httpClient.setTimeout(DEFAULT_READ_TIMEOUT);
        httpClient.start();

        Map<String, Object> options = new HashMap<String, Object>();
        options.put(ClientTransport.TIMEOUT_OPTION, httpClient.getTimeout());
        LongPollingTransport transport = new LongPollingTransport(options, httpClient) {
            @Override
            protected void customize(ContentExchange exchange) {
                super.customize(exchange);
                // TODO handle refreshing token on expiry
                // add current security token
                exchange.addRequestHeader("Authorization",
                    "OAuth " + component.getSession().getAccessToken());
            }
        };
        BayeuxClient client = new BayeuxClient(getEndpointUrl(), transport);
        if (isSummer11) {
            client.setCookie("com.salesforce.LocaleInfo", "us", COOKIE_MAX_AGE);
            client.setCookie("login", component.getSession().getUserName(), COOKIE_MAX_AGE);
            client.setCookie("sid", component.getSession().getAccessToken(), COOKIE_MAX_AGE);
            client.setCookie("language", "en_US", COOKIE_MAX_AGE);
        }
        return client;
    }

    public void subscribe(final String topicName, final SalesforceConsumer consumer) throws CamelException {
        // create subscription for consumer
        final String channel = getChannelName(topicName);

        LOG.info("Subscribing to channel: " + channel);
        final ClientSessionChannel.MessageListener listener = new ClientSessionChannel.MessageListener() {

            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Received Message: %s", message));
                }
                // convert CometD message to Camel Message
                consumer.processMessage(channel, message);
            }

        };

        final ClientSessionChannel clientChannel = client.getChannel(channel);
        clientChannel.subscribe(listener);

        // confirm that a subscription was created
        boolean subscribed = true;
        if (!clientChannel.getSubscribers().contains(listener)) {
            subscribed = false;
            try {
                Thread.sleep(DEFAULT_CONNECTION_TIMEOUT);
                if (!clientChannel.getSubscribers().contains(listener)) {
                    String message = String.format("Unable to subscribe to %s", topicName);
                    throw new CamelException(message);
                } else {
                    subscribed = true;
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
                // probably shutting down, so forget subscription
            }
        }

        if (subscribed) {
            listenerMap.put(consumer, listener);
        }
    }

    private String getChannelName(String topicName) {
        return isSummer11 ? "/" + topicName : "/topic/" + topicName;
    }

    public void unsubscribe(String topicName, SalesforceConsumer consumer) throws CamelException {
        // unsubscribe from channel
        final ClientSessionChannel.MessageListener listener = listenerMap.remove(consumer);
        if (listener != null) {
            final ClientSessionChannel clientChannel = client.getChannel(getChannelName(topicName));
            clientChannel.unsubscribe(listener);
            // confirm unsubscribe
            if (clientChannel.getSubscribers().contains(listener)) {
                try {
                    Thread.sleep(DEFAULT_CONNECTION_TIMEOUT);
                    if (clientChannel.getSubscribers().contains(listener)) {
                        String message = String.format("Unable to remove subscription to %s", topicName);
                        throw new CamelException(message);
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    // probably shutting down, forget unsubscribe and return
                }
            }
        }
    }

    public String getEndpointUrl() {
        return component.getSession().getInstanceUrl() +
            (isSummer11 ? "/cometd" : "/cometd/" + component.getApiVersion());
    }

    public void shutdown() {
        // TODO handle false return if the disconnect didn't happen in the given timeout
        client.disconnect(DEFAULT_CONNECTION_TIMEOUT);
    }

}
