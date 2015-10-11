package com.stormpath;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.AccountCriteria;
import com.stormpath.sdk.account.AccountList;
import com.stormpath.sdk.account.Accounts;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.application.Applications;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.client.Clients;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormpathTest {
    private static Logger logger = LoggerFactory.getLogger(StormpathTest.class);

    public static void main(String[] args) {
        if (System.getProperty("application") == null) {
            System.out.println("An applicaiton name is required. Pass it in as: ");
            System.out.println("\t-Dapplication=<application name>");
            System.exit(1);
        }

        Client client = Clients.builder().build();

        Application application = client.getApplications(
            Applications.where(Applications.name().eqIgnoreCase(System.getProperty("application")))
        ).iterator().next();

        AccountCriteria criteria = Accounts.criteria().limitTo(50);
        if (System.getProperty("expand") == null || !"false".equals(System.getProperty("expand").toLowerCase())) {
            criteria = criteria.withCustomData().withGroups();
        }

        AccountList accounts = application.getAccounts(criteria);

        int numAccounts = accounts.getSize();
        logger.debug("Working with: " + numAccounts + " accounts.");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (Account account : accounts) {
            // force wire if not expanded
            account.getGroups().iterator();
            account.getCustomData().getCreatedAt();
        }
        stopWatch.stop();

        logger.debug("Full Iteration of accounts, customData and groups: " + stopWatch.toString());
    }
}
