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
import org.apache.camel.Service;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.fusesource.camel.component.salesforce.SalesforceComponent;
import org.fusesource.camel.component.salesforce.SalesforceConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cometd.bayeux.Channel.*;
import static org.cometd.bayeux.Message.ERROR_FIELD;
import static org.cometd.bayeux.Message.SUBSCRIPTION_FIELD;

public class SubscriptionHelper implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionHelper.class);
    private static final String SUMMER_11 = "22.0";

    private static final int CONNECT_TIMEOUT = 110;
    private static final int CHANNEL_TIMEOUT = 40;

    private static final int COOKIE_MAX_AGE = 24 * 60 * 60 * 1000;
    private static final String EXCEPTION_FIELD = "exception";

    private final SalesforceComponent component;
    private final BayeuxClient client;
    private final boolean isSummer11;

    private final Map<SalesforceConsumer, ClientSessionChannel.MessageListener> listenerMap;
    private final Set<String> subscriptions;

    private boolean started;

    public SubscriptionHelper(SalesforceComponent component) throws Exception {
        this.component = component;

        this.subscriptions = new ConcurrentHashSet<String>();
        this.listenerMap = new ConcurrentHashMap<SalesforceConsumer, ClientSessionChannel.MessageListener>();

        this.isSummer11 = component.getConfig().getApiVersion().equals(SUMMER_11);

        // create CometD client
        this.client = createClient();
    }

    @Override
    public void start() throws Exception {
        if (started) {
            // no need to start again
            return;
        }

        // listener for handshake error or exception
        final String[] handshakeError = {null};
        final Exception[] handshakeException = {null};
        final ClientSessionChannel.MessageListener handshakeListener = new ClientSessionChannel.MessageListener() {
            public void onMessage(ClientSessionChannel channel, Message message) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("[CHANNEL:META_HANDSHAKE]: %s", message));
                }

                boolean success = message.isSuccessful();
                if (!success) {
                    String error = (String) message.get(ERROR_FIELD);
                    if (error != null) {
                        handshakeError[0] = error;
                    }
                    Exception exception = (Exception) message.get(EXCEPTION_FIELD);
                    if (exception != null) {
                        handshakeException[0] = exception;
                    }
                }
            }
        };
        client.getChannel(META_HANDSHAKE).addListener(handshakeListener);

        // listener for connect error
        final String[] connectError = {null};
        final ClientSessionChannel.MessageListener connectListener = new ClientSessionChannel.MessageListener() {
            public void onMessage(ClientSessionChannel channel, Message message) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[CHANNEL:META_CONNECT]: " + message);
                }

                boolean success = message.isSuccessful();
                if (!success) {
                    String error = (String) message.get(ERROR_FIELD);
                    if (error != null) {
                        LOG.error(String.format("Error during CONNECT: %s", error));
                        connectError[0] = error;
                    }
                }
            }
        };
        client.getChannel(META_CONNECT).addListener(
            connectListener);

        try {
            // TODO support auto-reconnects to Salesforce
            // connect to Salesforce cometd endpoint
            client.handshake();

            final long waitMs = MILLISECONDS.convert(CONNECT_TIMEOUT, SECONDS);
            if (!client.waitFor(waitMs, BayeuxClient.State.CONNECTED)) {
                if (handshakeException[0] != null) {
                    String msg = String.format("Exception during HANDSHAKE: %s", handshakeException[0].getMessage());
                    throw new CamelException(msg, handshakeException[0]);
                } else if (handshakeError[0] != null) {
                    String msg = String.format("Error during HANDSHAKE: %s", handshakeError[0]);
                    throw new CamelException(msg);
                } else if (connectError[0] != null) {
                    String msg = String.format("Error during CONNECT: %s", connectError[0]);
                    throw new CamelException(msg);
                } else {
                    String msg = String.format("Handshake request timeout after %s seconds", CONNECT_TIMEOUT);
                    throw new CamelException(msg);
                }
            }
        } finally {
            // cleanup event listeners
            client.getChannel(META_CONNECT).removeListener(connectListener);
            client.getChannel(META_HANDSHAKE).removeListener(handshakeListener);
        }

        started = true;

    }

    @Override
    public void stop() {
        if (started) {
            started = false;
            // TODO find and log any disconnect errors
            client.disconnect();
        }
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

        // channel message listener
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

        // listener for subscribe error
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] subscribeError = {null};
        final ClientSessionChannel.MessageListener subscriptionListener = new ClientSessionChannel.MessageListener() {
            public void onMessage(ClientSessionChannel channel, Message message) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[CHANNEL:META_SUBSCRIBE]: " + message);
                }
                final String subscribedChannelName = message.get(SUBSCRIPTION_FIELD).toString();
                if (channelName.equals(subscribedChannelName)) {

                    boolean success = message.isSuccessful();
                    if (!success) {
                        String error = (String) message.get(ERROR_FIELD);
                        if (error != null) {
                            subscribeError[0] = error;
                        }
                    } else {
                        // remember subscription
                        LOG.info("Subscribed to channel " + subscribedChannelName);
                        subscriptions.add(subscribedChannelName);
                    }
                    latch.countDown();
                }
            }
        };
        client.getChannel(META_SUBSCRIBE).addListener(subscriptionListener);

        try {
            clientChannel.subscribe(listener);

            // confirm that a subscription was created
            boolean subscribed = true;
            if (!subscriptions.contains(channelName)) {
                subscribed = false;
                try {
                    latch.await(CHANNEL_TIMEOUT, SECONDS);
                    if (!subscriptions.contains(channelName)) {
                        String message;
                        if (subscribeError[0] != null) {
                            message = String.format("Error subscribing to topic %s: %s",
                                topicName, subscribeError[0]);
                        } else {
                            message = String.format("Timeout error subscribing to topic %s after %s seconds",
                                topicName, CHANNEL_TIMEOUT);
                        }
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

        } finally {
            client.getChannel(META_SUBSCRIBE).removeListener(subscriptionListener);
        }
    }

    private String getChannelName(String topicName) {
        return isSummer11 ? "/" + topicName : "/topic/" + topicName;
    }

    public void unsubscribe(String topicName, SalesforceConsumer consumer) throws CamelException {

        // listen for unsubscribe error
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] unsubscribeError = {null};
        final ClientSessionChannel.MessageListener unsubscribeListener = new ClientSessionChannel.MessageListener() {
            public void onMessage(ClientSessionChannel channel, Message message) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[CHANNEL:META_UNSUBSCRIBE]: " + message);
                }
                String channelName = message.get(SUBSCRIPTION_FIELD).toString();

                boolean success = message.isSuccessful();
                if (!success) {
                    String error = (String) message.get(ERROR_FIELD);
                    if (error != null) {
                        unsubscribeError[0] = error;
                    }
                } else {
                    // forget subscription
                    LOG.info("Unsubscribed from channel " + channelName);
                    subscriptions.remove(channelName);
                }
                latch.countDown();
            }
        };
        client.getChannel(META_UNSUBSCRIBE).addListener(unsubscribeListener);

        try {
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
                        latch.await(CHANNEL_TIMEOUT, SECONDS);
                        if (subscriptions.contains(channelName)) {
                            String message;
                            if (unsubscribeError[0] != null) {
                                message = String.format("Error unsubscribing from topic %s: %s",
                                    topicName, unsubscribeError[0]);
                            } else {
                                message = String.format("Timeout error unsubscribing from topic %s after %s seconds",
                                    topicName, CHANNEL_TIMEOUT);
                            }
                            throw new CamelException(message);
                        }
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        // probably shutting down, forget unsubscribe and return
                    }
                }

            }
        } finally {
            client.getChannel(META_UNSUBSCRIBE).removeListener(unsubscribeListener);
        }
    }

    public String getEndpointUrl() {
        return component.getSession().getInstanceUrl() +
            (isSummer11 ? "/cometd" : "/cometd/" + component.getConfig().getApiVersion());
    }

}
