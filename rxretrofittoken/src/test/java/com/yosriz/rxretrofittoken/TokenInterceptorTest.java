package com.yosriz.rxretrofittoken;


import com.yosriz.rxretrofittoken.fakeapi.FakeAPIService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.yosriz.rxretrofittoken.testutils.APITools.getFakeObjectResponse;
import static com.yosriz.rxretrofittoken.testutils.APITools.getSomeFakeObject;

@RunWith(Parameterized.class)
public class TokenInterceptorTest {

    private Retrofit retrofit;
    private MockWebServer mockWebServer;
    private Single<String> refreshTokenSingle;

    @Parameterized.Parameters
    public static Collection primeNumbers() {
        return Arrays.asList(
                new Object[]{RetrofitTokenRefresher.Builder.AuthMethod.HEADERS,
                        "auth",
                        (Function<RecordedRequest, String>) req -> req.getHeader("auth")},
                new Object[]{RetrofitTokenRefresher.Builder.AuthMethod.QUERY_PARAMS,
                        "auth",
                        (Function<RecordedRequest, String>) req -> req.getRequestUrl().queryParameter("auth")}
        );
    }

    private final RetrofitTokenRefresher.Builder.AuthMethod authMethod;
    private final Function<RecordedRequest, String> requestAuthExtractor;
    private final String authParamName;

    public TokenInterceptorTest(
            RetrofitTokenRefresher.Builder.AuthMethod authMethod, String authParamName,
            Function<RecordedRequest, String> requestAuthExtractor) {
        this.authMethod = authMethod;
        this.requestAuthExtractor = requestAuthExtractor;
        this.authParamName = authParamName;
    }

    @Before
    public void setup() {
        Single<String> refreshTokenSingle = Single.defer(this::getTokenRefreshSingle);
        RetrofitTokenRefresher tokenRefresher = new RetrofitTokenRefresher.Builder()
                .setTokenExpirationChecker(throwable -> throwable instanceof HttpException && ((HttpException) throwable).code() == 401)
                .setMaxRetries(3)
                .setAuth(authMethod, authParamName, request -> !request.url().url().toString().contains("somethingelse"))
                .setTokenRequest(refreshTokenSingle)
                .build();

        mockWebServer = new MockWebServer();

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(tokenRefresher.rxJava2TokenAdapterFactory())
                .baseUrl(mockWebServer.url("/"))
                .client(new OkHttpClient.Builder()
                        .addInterceptor(tokenRefresher.tokenInterceptor())
                        .build())
                .build();
    }

    @Test
    public void when_request_do_not_pass_request_filter_expect_no_auth_in_request() throws Exception {
        mockWebServer.enqueue(getFakeObjectResponse(getSomeFakeObject()));

        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        fakeAPIService.getSomethingElse(42)
                .subscribe();

        String actualAuthToken = requestAuthExtractor.apply(mockWebServer.takeRequest());
        Assert.assertNull(actualAuthToken);
    }


    @Test
    public void when_request_expect_provided_auth_in_request() throws Exception {
        refreshTokenSingle = Single.just("auth_token_for_test");
        mockWebServer.enqueue(getFakeObjectResponse(getSomeFakeObject()));

        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        fakeAPIService.getSomething(42)
                .subscribe();

        String actualAuthToken = requestAuthExtractor.apply(mockWebServer.takeRequest());
        Assert.assertEquals("auth_token_for_test", actualAuthToken);
    }

    @Test
    public void when_auth_changed_expect_correct_auth_in_request() throws Exception {
        refreshTokenSingle = createValueOnEachSubscribeSingle("firstToken", "secondToken");
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        mockWebServer.enqueue(getFakeObjectResponse(getSomeFakeObject()));

        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        fakeAPIService.getSomething(42)
                .subscribe();

        String actualAuthToken = requestAuthExtractor.apply(mockWebServer.takeRequest());
        Assert.assertEquals("firstToken", actualAuthToken);

        actualAuthToken = requestAuthExtractor.apply(mockWebServer.takeRequest());
        Assert.assertEquals("secondToken", actualAuthToken);
    }

    @Test
    public void when_auth_changed_expect_correct_auth_in_request_after_second_query() throws Exception {
        refreshTokenSingle = createValueOnEachSubscribeSingle("firstToken", "secondToken");
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        mockWebServer.enqueue(getFakeObjectResponse(getSomeFakeObject()));
        mockWebServer.enqueue(getFakeObjectResponse(getSomeFakeObject()));

        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        fakeAPIService.getSomething(42)
                .subscribe();
        fakeAPIService.getSomething(42)
                .subscribe();

        String actualAuthToken = requestAuthExtractor.apply(mockWebServer.takeRequest());
        Assert.assertEquals("firstToken", actualAuthToken);

        actualAuthToken = requestAuthExtractor.apply(mockWebServer.takeRequest());
        Assert.assertEquals("secondToken", actualAuthToken);

        actualAuthToken = requestAuthExtractor.apply(mockWebServer.takeRequest());
        Assert.assertEquals("secondToken", actualAuthToken);
    }

    private Single<String> getTokenRefreshSingle() {
        return refreshTokenSingle;
    }

    @SafeVarargs
    private static <T> Single<T> createValueOnEachSubscribeSingle(T... values) {
        return Single.defer(new Callable<SingleSource<T>>() {
            private int subscriptionCount = 0;

            @Override
            public SingleSource<T> call() throws Exception {
                T retVal;
                if (values.length > subscriptionCount) {
                    retVal = values[subscriptionCount];
                } else {
                    retVal = values[values.length - 1];
                }
                subscriptionCount++;
                return Single.just(retVal);
            }
        });
    }
}
