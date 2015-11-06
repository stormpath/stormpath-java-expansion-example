package com.stormpath.tests;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.AccountCriteria;
import com.stormpath.sdk.account.AccountList;
import com.stormpath.sdk.account.Accounts;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.application.Applications;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.client.Clients;
import com.stormpath.sdk.directory.CustomData;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

public class AccountTest implements Callable {
    private static Logger logger = LoggerFactory.getLogger(AccountTest.class);

    private CommandLine commandLine;

    public AccountTest(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    public Map<String, Long> call() throws Exception {
        Client client = Clients.builder().build();

        Application application = client.getApplications(
            Applications.where(Applications.name().eqIgnoreCase(commandLine.getOptionValue("application")))
        ).iterator().next();

        int limit = 50;
        if (commandLine.getOptionValue("limit") != null) {
            try {
                limit = Integer.parseInt(commandLine.getOptionValue("limit"));
            } catch (NumberFormatException nfe) {
                logger.error(commandLine.getOptionValue("limit") + ", does not parse as an int.");
            }
        }

        AccountCriteria criteria = Accounts.criteria().limitTo(limit);
        if (!commandLine.hasOption("no-expand")) {
            criteria = criteria.withCustomData().withGroups();
        }

        AccountList accounts = application.getAccounts(criteria);

        int numAccounts = accounts.getSize();
        logger.debug("Working with: " + numAccounts + " accounts. Limit is: " + limit);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (Account account : accounts) {
            // force wire if not expanded
            account.getGroups().iterator();
            CustomData cd = account.getCustomData();
            if (commandLine.hasOption("load-custom-data")) {
                for (int i = 0; i<100; i++) {
                    cd.put("key"+i, i);
                }
                cd.save();
            } else if (commandLine.hasOption("wipe-custom-data")) {
                cd.clear();
                cd.save();
            }
            cd.getCreatedAt();
        }
        stopWatch.stop();

        logger.debug("Full Iteration of accounts, customData and groups: " + stopWatch.toString());
        return null;
    }
}
