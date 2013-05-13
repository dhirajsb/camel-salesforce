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
import org.cometd.bayeux.Channel;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SubscriptionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionHelper.class);
    private static final String SUMMER_11 = "22.0";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 20 * 1000;
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60 * 1000;
    private static final int HANDSHAKE_TIMEOUT = 10 * 1000;
    private static final String EXCEPTION_FIELD = "exception";

    private final SalesforceComponent component;
    private final BayeuxClient client;
    private final boolean isSummer11;

    private String handshakeError;
    private Exception handshakeException;

    private final Map<SalesforceConsumer, ClientSessionChannel.MessageListener> listenerMap;
    private final Set<String> subscriptions;

    private Map<String, String> subscribeError;
    private Map<String, String> unsubscribeError;

    public SubscriptionHelper(SalesforceComponent component) throws Exception {
        this.component = component;

        this.listenerMap = new HashMap<SalesforceConsumer, ClientSessionChannel.MessageListener>();
        this.subscriptions = new HashSet<String>();
        this.subscribeError = new HashMap<String, String>();
        this.unsubscribeError = new HashMap<String, String>();

        this.isSummer11 = component.getConfig().getApiVersion().equals(SUMMER_11);

        // create CometD client
        this.client = createClient();

        // add META channel listeners
        addMetaListeners();
        // connect to Salesforce cometd endpoint
        client.handshake();
        if (!client.waitFor(HANDSHAKE_TIMEOUT, BayeuxClient.State.CONNECTED)) {
            if (handshakeException != null) {
                String msg = String.format("Exception during HANDSHAKE: %s", handshakeException.getMessage());
                throw new CamelException(msg, handshakeException);
            } else {
                String msg = String.format("Error during HANDSHAKE: %s", handshakeError);
                throw new CamelException(msg);
            }
        }
    }

    private void addMetaListeners() {

        // listen for handshake error or exception
        client.getChannel(Channel.META_HANDSHAKE).addListener
            (new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("[CHANNEL:META_HANDSHAKE]: %s", message));
                    }

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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[CHANNEL:META_CONNECT]: " + message);
                    }

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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[CHANNEL:META_SUBSCRIBE]: " + message);
                    }
                    final String channelName = message.get(Message.SUBSCRIPTION_FIELD).toString();

                    boolean success = message.isSuccessful();
                    if (!success) {
                        String error = (String) message.get(Message.ERROR_FIELD);
                        if (error != null) {
                            subscribeError.put(channelName, error);
                        }
                    } else {
                        // remember subscription
                        LOG.info("Subscribed to channel " + channelName);
                        subscriptions.add(channelName);
                    }
                }
            });

        // listen for unsubscribe error
        client.getChannel(Channel.META_UNSUBSCRIBE).addListener(
            new ClientSessionChannel.MessageListener() {
                public void onMessage(ClientSessionChannel channel, Message message) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[CHANNEL:META_UNSUBSCRIBE]: " + message);
                    }
                    String channelName = message.get(Message.SUBSCRIPTION_FIELD).toString();

                    boolean success = message.isSuccessful();
                    if (!success) {
                        String error = (String) message.get(Message.ERROR_FIELD);
                        if (error != null) {
                            unsubscribeError.put(channelName, error);
                        }
                    } else {
                        // forget subscription
                        LOG.info("Unsubscribed from channel " + channelName);
                        subscriptions.remove(channelName);
                    }
                }
            });
    }

    private BayeuxClient createClient() throws Exception {
        // use Jetty client from SalesforceComponent
        final HttpClient httpClient = component.getHttpClient();

        Map<String, Object> options = new HashMap<String, Object>();
        options.put(ClientTransport.TIMEOUT_OPTION, httpClient.getTimeout());

        // check login access token
        String currentToken = component.getSession().getAccessToken();
        if (currentToken == null) {
            // lazy login here!
            currentToken = component.getSession().login(null);
        }
        final String accessToken = currentToken;

        LongPollingTransport transport = new LongPollingTransport(options, httpClient) {
            @Override
            protected void customize(ContentExchange exchange) {
                super.customize(exchange);
                // TODO refresh token on expiry
                // add current security token
                exchange.addRequestHeader("Authorization",
                    "OAuth " + accessToken);
            }
        };
        BayeuxClient client = new BayeuxClient(getEndpointUrl(), transport);
        if (isSummer11) {
            client.setCookie("com.salesforce.LocaleInfo", "us", COOKIE_MAX_AGE);
            client.setCookie("login", component.getLoginConfig().getUserName(), COOKIE_MAX_AGE);
            client.setCookie("sid", accessToken, COOKIE_MAX_AGE);
            client.setCookie("language", "en_US", COOKIE_MAX_AGE);
        }
        return client;
    }

    public void subscribe(final String topicName, final SalesforceConsumer consumer) throws CamelException {
        // create subscription for consumer
        final String channelName = getChannelName(topicName);

        LOG.info(String.format("Subscribing to channel %s...", channelName));
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

        final ClientSessionChannel clientChannel = client.getChannel(channelName);
        clientChannel.subscribe(listener);

        // confirm that a subscription was created
        boolean subscribed = true;
        if (!subscriptions.contains(channelName)) {
            subscribed = false;
            try {
                Thread.sleep(DEFAULT_CONNECTION_TIMEOUT);
                if (!subscriptions.contains(channelName)) {
                    String message = String.format("Error subscribing to topic %s: %s",
                        topicName, subscribeError.remove(channelName));
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

            final String channelName = getChannelName(topicName);
            LOG.info(String.format("Unsubscribing from channel %s...", channelName));

            final ClientSessionChannel clientChannel = client.getChannel(channelName);
            clientChannel.unsubscribe(listener);

            // confirm unsubscribe
            if (subscriptions.contains(channelName)) {
                try {
                    Thread.sleep(DEFAULT_CONNECTION_TIMEOUT);
                    if (subscriptions.contains(channelName)) {
                        String message = String.format("Error unsubscribing from topic %s: %s",
                            topicName, unsubscribeError.remove(channelName));
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
            (isSummer11 ? "/cometd" : "/cometd/" + component.getConfig().getApiVersion());
    }

    public void shutdown() {
        // TODO find and log any disconnect errors
        client.disconnect();
    }

}
