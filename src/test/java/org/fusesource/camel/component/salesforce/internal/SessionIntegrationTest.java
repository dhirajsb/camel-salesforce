package org.fusesource.camel.component.salesforce.internal;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.RedirectListener;
import org.fusesource.camel.component.salesforce.LoginConfigHelper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dbokde
 */
public class SessionIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SessionIntegrationTest.class);

    @Test
    public void testLogin() throws Exception {

        final HttpClient httpClient = new HttpClient();
        httpClient.registerListener(RedirectListener.class.getName());
        httpClient.start();

        final SalesforceSession session = new SalesforceSession(
            httpClient, LoginConfigHelper.getLoginConfig());

        try {
            String loginToken = session.login(session.getAccessToken());
            LOG.info("First token " + loginToken);

            // refresh token, also causes logout
            loginToken = session.login(loginToken);
            LOG.info("Refreshed token " + loginToken);
        } finally {
            // logout finally
            session.logout();
        }
    }

}
