#Stormpath is Joining Okta
We are incredibly excited to announce that [Stormpath is joining forces with Okta](https://stormpath.com/blog/stormpaths-new-path?utm_source=github&utm_medium=readme&utm-campaign=okta-announcement). Please visit [the Migration FAQs](https://stormpath.com/oktaplusstormpath?utm_source=github&utm_medium=readme&utm-campaign=okta-announcement) for a detailed look at what this means for Stormpath users.

We're available to answer all questions at [support@stormpath.com](mailto:support@stormpath.com).


# Performance Example

The purpose of this project is to demonstrate the performance with Java with two different tests:

* Performance gain when using Stormpath's [link expansion](http://docs.stormpath.com/java/product-guide/#link-expansion) criteria.
* General performance of token auth operations with and without cache

## setup

Follow the instructions [here](http://docs.stormpath.com/java/quickstart/#get-an-api-key) to create a stormpath account. Make note of where you've saved your `apiKey.properties` file. We will use it below.

Create an Application in the admin UI for your Stormpath account. Create some number of Accounts, Groups and CustomData in the Directory associated with your Application.

## build

`mvn clean package`

## run

There are a number of command line arguments this program uses to control logging and to toggle the use of expansion. Here's how to run the program:

```
STORMPATH_API_KEY_FILE=/path/to/apiKey.properties java \
    [-Dlog.headers=<log level - WARN by default>] \
    [-Dlog.wire=<log level - WARN by default>] \
    [-Dlog.stormpath=<log level - DEBUG by default>] \
  -jar target/stormpath-test-uber-0.1.1.jar \
    --application <application name> \
    --test account | token \
    [--no-expand] \
    [--limit <Account request limit - 50 by default>] \
    [--username <username>] \
    [--password <password>] \
    [--iterations <total # of runs>] \
    [--executors <# of threads>] \
    [--reports csv | json]
```

The only required propertes are `application` and `test`.

Available tests are `account` and `token`. The account test exercises retrieving all accounts with and without expansion. The token test exercises
oauth2 authentication and verification locally and via api, with and without cache.

By default, link expansion is turned on for `customData` and `groups`.
Using the `--no-expand` turns off link expansion.

By default, low level logging of HTTP requests is turned off. To enable logging of all headers, use this switch: `-Dlog.headers=debug`. This is handy in seeing the number of wire requests that  are made when expansion is turned off and turned on.

By default, the program will show the number of accounts found for the application and the total time to iterate over the accounts, groups and customData. This can be turned off using this switch: `-Dlog.stormpath=warn`

Here's a sample run with expansion:

```
STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.remote_test.properties java -jar target/stormpath-test-uber-0.1.1.jar  \
  --application stormpath-seed2 \
  --test account \
  --iterations 1 \
  --executors 1 \
  --limit 100
  
18:58:28.777 [main] DEBUG com.stormpath.StormpathTest - Working with: 500 accounts.
18:58:36.768 [main] DEBUG com.stormpath.StormpathTest - Full Iteration of accounts, customData and groups: 00:00:07.984
```

You can see that it iterated over 500 accounts and the associated customData and groups in under 8 seconds.

Here's the same run without using expansion:

```
STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.remote_test.properties java -jar target/stormpath-test-uber-0.1.1.jar  \
  --application stormpath-seed2 \
  --test account \
  --iterations 1 \
  --executors 1 \
  --limit 100 \
  --no-expand
  
19:01:12.453 [main] DEBUG com.stormpath.StormpathTest - Working with: 500 accounts.
19:02:20.665 [main] DEBUG com.stormpath.StormpathTest - Full Iteration of accounts, customData and groups: 00:01:08.205
```

Without expansion it took an additional minute to retrieve the same data!

Let's take a look at the number of reqeuests being made over the wire in both cases. First, with expansion:

```
STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.remote_test.properties java \
  -Dlog.headers=debug \
-jar target/stormpath-test-uber-0.1.1.jar  \
  --application stormpath-seed2 \
  --test account \
  --iterations 1 \
  --executors 1 \
  --limit 100 | grep GET | wc -l
  
13
```

The above example is counting all of the log lines containing `GET` in them. This is a crude way of counting all the requests. It's making a total of 13 requests to get all the accounts and associated groups and customData.

Now, without expansion:

```
STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.remote_test.properties 
java -Dlog.headers=debug -jar target/stormpath-test-uber-0.1.1.jar  \
  --application stormpath-seed2 \
  --test account \
  --iterations 1 \
  --executors 1 \
  --no-expand \
  --limit 100 | grep GET | wc -l
  
1013
```

This time, it's making an additional 1000 requests! No wonder why it takes so much longer.

You can also produce a nicely formatted `csv` or `json` report of token auth stats. Here's the command you would run:

```
STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.remote_test.properties \
java -Dlog.stormpath=warn -jar target/stormpath-test-uber-0.1.1.jar \
  --application stormpath-seed2 \
  --test token \
  --username <username> \
  --password <password> \
  --iterations 100 \
  --executors 10 \
  --report csv
```

This will produce a report that looks like this (all values in milliseconds):

|cache:oauth|cache:verify&#8209;local|cache:verify&#8209;api|nocache:oauth|nocache:verify&#8209;local|nocache:verify&#8209;api|
|----------:|-----------------:|---------------:|------------:|-------------------:|-----------------:|
1|0|1|124|0|110
2|0|1|108|0|76
2|0|1|76|0|79
2|0|2|111|0|66
1|0|1|113|0|76
1|0|1|120|0|130
1|0|1|85|0|77
1|0|2|100|0|115
1|0|1|100|0|107
...|...|...|...|...|...

## conclusion

Stormpath's [link expansion](http://docs.stormpath.com/java/product-guide/#link-expansion) feature is your friend! Use it when you know in advance the data objects you will be working with.

Have a look at the token auth code - with and without cache. Cacheing is your friedn, too!
