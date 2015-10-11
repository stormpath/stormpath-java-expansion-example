# Expansion Optimization Example

The purpose of this project is to demonstrate the performance gain when using Stormpath's [link expansion](http://docs.stormpath.com/java/product-guide/#link-expansion) criteria.

## setup

Follow the instructions [here](http://docs.stormpath.com/java/quickstart/#get-an-api-key) to create a stormpath account. Make note of where you've saved your `apiKey.properties` file. We will use it below.

Create an Application in the admin UI for your Stormpath account. Create some number of Accounts, Groups and CustomData in the Directory associated with your Application.

## build

`mvn clean package`

## run

There are a number of system properties this program uses to control logging and to toggle the use of expansion. Here's how to run the program:

```
STORMPATH_API_KEY_FILE=/path/to/apiKey.properties java \
  -Dapplication=<application name> \
  [-Dexpand=false] \
  [-Dlog.headers=<log level - WARN by default>] \
  [-Dlog.wire=<log level - WARN by default>] \
  [-Dlog.stormpath=<log level - DEBUG by default>] \
  -jar target/stormpath-test.jar
```

The only required property is an `application` name.

By default, link expansion is turned on for `customData` and `groups`.
Using the `-Dexpand=false` turns off link expansion.

By default, low level logging of HTTP requests is turned off. To enable logging of all headers, use this switch: `-Dlog.headers=debug`. This is handy in seeing the number of wire requests that  are made when expansion is turned off and turned on.

By default, the program will show the number of accounts found for the application and the total time to iterate over the accounts, groups and customData. This can be turned off using this switch: `-Dlog.stormpath=warn`

Here's a sample run with expansion:

```
➥ STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.properties java \
  -Dapplication=stormpath-seed \
  -jar target/stormpath-test.jar
  
18:58:28.777 [main] DEBUG com.stormpath.StormpathTest - Working with: 500 accounts.
18:58:36.768 [main] DEBUG com.stormpath.StormpathTest - Full Iteration of accounts, customData and groups: 00:00:07.984
```

You can see that it iterated over 500 accounts and the associated customData and groups in under 8 seconds.

Here's the same run without using expansion:

```
➥ STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.properties java \
  -Dapplication=stormpath-seed \
  -Dexpand=false \
  -jar target/stormpath-test.jar
  
19:01:12.453 [main] DEBUG com.stormpath.StormpathTest - Working with: 500 accounts.
19:02:20.665 [main] DEBUG com.stormpath.StormpathTest - Full Iteration of accounts, customData and groups: 00:01:08.205
```

Without expansion it took an additional minute to retrieve the same data!

Let's take a look at the number of reqeuests being made over the wire in both cases. First, with expansion:

```
➥ STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.properties java \
  -Dapplication=stormpath-seed \
  -Dlog.headers=debug \
  -jar target/stormpath-test.jar  | grep GET | wc -l
  
13
```

The above example is counting all of the log lines containing `GET` in them. This is a crude way of counting all the requests. It's making a total of 13 requests to get all the accounts and associated groups and customData.

Now, without expansion:

```
➥ STORMPATH_API_KEY_FILE=~/.stormpath/apiKey.properties java \
  -Dapplication=stormpath-seed \
  -Dlog.headers=debug \
  -Dexpand=false \
  -jar target/stormpath-test.jar  | grep GET | wc -l
  
1013
```

This time, it's making an additional 1000 requests! No wonder why it takes so much longer.

## conclusion

Stormpath's [link expansion](http://docs.stormpath.com/java/product-guide/#link-expansion) feature is your friend! Use it when you know in advance the data objects you will be working with.