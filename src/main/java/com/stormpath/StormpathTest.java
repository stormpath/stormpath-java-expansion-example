package com.stormpath;

import com.google.common.collect.Sets;
import com.stormpath.tests.AccountTest;
import com.stormpath.tests.TokenTest;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StormpathTest {
    private static Logger logger = LoggerFactory.getLogger(StormpathTest.class);

    private enum ExitCode {
        NORMAL,
        ERROR;
    }

    private static void usage(Options commandLineOptions, ExitCode exitCode) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("stormpath-test", commandLineOptions);
        System.exit(exitCode.ordinal());
    }

    private static void requireOptions(String[] optionNames, CommandLine commandLine, Options commandLineOptions) {
        for (String optionName : optionNames) {
            if (!commandLine.hasOption(optionName)) {
                usage(commandLineOptions, ExitCode.ERROR);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Options commandLineOptions = new Options();
        commandLineOptions.addOption("t", "test", true, "Which test to run: account | token (required)");
        commandLineOptions.addOption("a", "application", true, "Application name. (required)");
        commandLineOptions.addOption("l", "limit", true, "Request limit. (optional - default=50)");
        commandLineOptions.addOption("n", "no-expand", false, "Don't use link expansion. (optional - default=false)");
        commandLineOptions.addOption("c", "load-custom-data", false, "Populate custom data on all accounts. (optional)");
        commandLineOptions.addOption("w", "wipe-custom-data", false, "Delete custom data on all accounts. (optional)");
        commandLineOptions.addOption("u", "username", true, "Username to use for token auth. (optional)");
        commandLineOptions.addOption("p", "password", true, "Password to use for token auth. (optional)");
        commandLineOptions.addOption("i", "iterations", true, "Number of times to run the test. (optional - default=20)");
        commandLineOptions.addOption("e", "executors", true, "Number of threads. (optional - default=5)");
        commandLineOptions.addOption("h", "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(commandLineOptions, args);
        } catch(ParseException pe) {
            System.out.println("Unexpected exception:" + pe.getMessage());
            System.exit(1);
        }

        if (commandLine.hasOption("help")) {
            usage(commandLineOptions, ExitCode.NORMAL);
        } else if (!commandLine.hasOption("test")) {
            usage(commandLineOptions, ExitCode.ERROR);
        }
        String test = commandLine.getOptionValue("test").toLowerCase();

        int iterations = 20;
        if (commandLine.getOptionValue("iterations") != null) {
            try {
                iterations = Integer.parseInt(commandLine.getOptionValue("iterations"));
            } catch (NumberFormatException nfe) {
                logger.error(commandLine.getOptionValue("iterations") + ", does not parse as an int.");
            }
        }

        int executors = 5;
        if (commandLine.getOptionValue("executors") != null) {
            try {
                executors = Integer.parseInt(commandLine.getOptionValue("executors"));
            } catch (NumberFormatException nfe) {
                logger.error(commandLine.getOptionValue("executors") + ", does not parse as an int.");
            }
        }

        ExecutorService pool = Executors.newFixedThreadPool(executors);
        Set<Future<Map<String, Long>>> set = Sets.newHashSet();

        for (int i=0; i<iterations; i++) {
            Callable<Map<String, Long>> c = null;
            if ("account".equals(test)) {
                requireOptions(new String[]{"application"}, commandLine, commandLineOptions);
                c = new AccountTest(commandLine);
            } else if ("token".equals(test)) {
                requireOptions(new String[]{"application", "username", "password"}, commandLine, commandLineOptions);
                c = new TokenTest(commandLine);
            }
            Future<Map<String, Long>> future = pool.submit(c);
            set.add(future);
        }

        for (Future<Map<String, Long>> future : set) {
            JSONObject j = new JSONObject(future.get());
            System.err.println(j.toString());
        }
        System.exit(0);
    }
}
