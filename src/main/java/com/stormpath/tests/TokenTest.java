package com.stormpath.tests;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.api.ApiRequestAuthenticator;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.application.Applications;
import com.stormpath.sdk.authc.AuthenticationRequest;
import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.authc.UsernamePasswordRequest;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.client.Clients;
import com.stormpath.sdk.http.HttpMethod;
import com.stormpath.sdk.http.HttpRequest;
import com.stormpath.sdk.http.HttpRequestBuilder;
import com.stormpath.sdk.http.HttpRequests;
import com.stormpath.sdk.impl.util.Base64;
import com.stormpath.sdk.oauth.AccessTokenResult;
import io.jsonwebtoken.Jwts;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.stormpath.sdk.cache.Caches.newDisabledCacheManager;

public class TokenTest implements Callable {
    private static Logger logger = LoggerFactory.getLogger(TokenTest.class);

    private CommandLine commandLine;

    public TokenTest(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    public Map<String, Long> call() throws Exception {

        Client client = Clients.builder().build();
        Map<String, Long> cacheMap = doIt(client, "cache", "Exercising token auth with default cache settings.");

        logger.debug("");

        client = Clients.builder().setCacheManager(newDisabledCacheManager()).build();
        Map<String, Long> noCacheMap = doIt(client, "nocache", "Exercising token auth with cache disabled.");

        Map<String, Long> retMap = new HashMap<>(cacheMap);
        retMap.putAll(noCacheMap);

        return retMap;
    }

    private Map<String, Long> doIt(Client client, String key, String message) throws Exception {
        Map<String, Long> retMap = Maps.newHashMap();

        logger.debug(message);

        String username = commandLine.getOptionValue("username");
        String password = commandLine.getOptionValue("password");
        String applicationName = commandLine.getOptionValue("application");

        Application application = client.getApplications(
            Applications.where(Applications.name().eqIgnoreCase(applicationName))
        ).iterator().next();

        AuthenticationRequest usernamePasswordRequest = UsernamePasswordRequest.builder()
            .setUsernameOrEmail(username)
            .setPassword(password)
            .build();

        AuthenticationResult authenticationResult = application.authenticateAccount(usernamePasswordRequest);
        Account account = authenticationResult.getAccount();
        ApiKey apiKey = account.createApiKey();

        String auth = "Basic " + Base64.encodeBase64String(
            (apiKey.getId() + ":" + apiKey.getSecret()).getBytes("UTF-8")
        );
        HttpRequest request = getHttpRequestBuilder(auth, "application/x-www-form-urlencoded")
            .addParameter("grant_type", new String[]{"client_credentials"})
            .build();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        AccessTokenResult result = Applications.oauthRequestAuthenticator(application)
            .withTtl(3600)
            .authenticate(request);

        stopWatch.stop();
        logger.debug("Time to oauth authenticate: " + stopWatch.getTime());
        retMap.put(key + ":oauth", stopWatch.getTime());

        String accessToken = result.getTokenResponse().getAccessToken();

        stopWatch.reset();
        stopWatch.start();

        Jwts.parser().setSigningKey(client.getApiKey().getSecret().getBytes("UTF-8")).parseClaimsJws(accessToken);

        stopWatch.stop();
        logger.debug("Time to verify token locally: " + stopWatch.getTime());
        retMap.put(key + ":verify-local", stopWatch.getTime());

        auth = "Bearer " + accessToken;
        request = getHttpRequestBuilder(auth, "application/json").build();

        stopWatch.reset();
        stopWatch.start();

        ApiRequestAuthenticator apiRequestAuthenticator = Applications.apiRequestAuthenticator(application);

        apiRequestAuthenticator.authenticate(request);

        stopWatch.stop();
        logger.debug("Time to verify token via api: " + stopWatch.getTime());
        retMap.put(key + ":verify-api", stopWatch.getTime());

        stopWatch.reset();
        stopWatch.start();

        apiKey.delete();

        stopWatch.stop();
        logger.debug("Time to delete api key: " + stopWatch.getTime());
        return retMap;
    }

    private HttpRequestBuilder getHttpRequestBuilder(String authorization, String contentType) {
        Map<String, String[]> headers = ImmutableMap.of(
            "content-type", new String[]{contentType},
            "authorization", new String[]{authorization}
        );

        return HttpRequests.method(HttpMethod.POST).headers(headers);
    }
}