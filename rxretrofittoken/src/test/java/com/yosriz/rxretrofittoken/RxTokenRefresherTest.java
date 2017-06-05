package com.yosriz.rxretrofittoken;

import com.yosriz.rxretrofittoken.fakeapi.FakeAPIService;
import com.yosriz.rxretrofittoken.fakeapi.SomeFakeObject;
import com.yosriz.rxretrofittoken.testutils.SingleProbe;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.yosriz.rxretrofittoken.testutils.APITools.getFakeObjectResponse;
import static com.yosriz.rxretrofittoken.testutils.APITools.getSomeFakeObject;

public class RxTokenRefresherTest {

    private Retrofit retrofit;
    private MockWebServer mockWebServer;
    private SingleProbe<String> refreshTokenProbe = new SingleProbe<>();
    private Single<String> refreshTokenSingle = Single.just("fake_refresh_token")
            .compose(refreshTokenProbe);


    @Before
    public void setup() {

        RetrofitTokenRefresher tokenRefresher = new RetrofitTokenRefresher.Builder()
                .setTokenExpirationChecker(throwable -> throwable instanceof HttpException && ((HttpException) throwable).code() == 401)
                .setMaxRetries(3)
                .setAuth(RetrofitTokenRefresher.Builder.AuthMethod.HEADERS, "auth")
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
    public void no_auth_error_succeed() throws InterruptedException {
        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        SomeFakeObject someFakeObject = getSomeFakeObject();
        mockWebServer.enqueue(
                getFakeObjectResponse(someFakeObject)
        );
        fakeAPIService.getSomething(42)
                .test()
                .await()
                .assertValue(result -> someFakeObject.anotherData == result.anotherData && someFakeObject.someData.equals(result.someData));
    }

    @Test
    public void unauthorized_retry_count_below_threshold_expect_value() throws InterruptedException {
        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        SomeFakeObject someFakeObject = getSomeFakeObject();
        MockResponse mockUnauthorizedResponse = new MockResponse()
                .setResponseCode(401);
        MockResponse mockErrorResponse = new MockResponse()
                .setResponseCode(404);

        mockWebServer.enqueue(mockUnauthorizedResponse);
        mockWebServer.enqueue(mockErrorResponse);
        mockWebServer.enqueue(mockErrorResponse);
        mockWebServer.enqueue(getFakeObjectResponse(someFakeObject));

        fakeAPIService.getSomething(42)
                .test()
                .await()
                .assertValue(result -> someFakeObject.anotherData == result.anotherData && someFakeObject.someData.equals(result.someData));
    }


    @Test
    public void unauthorized_retry_count_above_threshold_expect_error() throws InterruptedException {
        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        MockResponse mockUnauthorizedResponse = new MockResponse()
                .setResponseCode(401);
        MockResponse mockErrorResponse = new MockResponse()
                .setResponseCode(404);

        mockWebServer.enqueue(mockUnauthorizedResponse);
        mockWebServer.enqueue(mockErrorResponse);
        mockWebServer.enqueue(mockErrorResponse);
        mockWebServer.enqueue(mockErrorResponse);

        fakeAPIService.getSomething(42)
                .test()
                .await()
                .assertError(throwable -> throwable instanceof HttpException && ((HttpException) throwable).code() == 404);
    }

    @Test
    public void unauthorized_twice_expect_error() throws InterruptedException {
        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        MockResponse mockUnauthorizedResponse = new MockResponse()
                .setResponseCode(401);

        mockWebServer.enqueue(mockUnauthorizedResponse);
        mockWebServer.enqueue(mockUnauthorizedResponse);

        fakeAPIService.getSomething(42)
                .test()
                .await()
                .assertError(throwable -> throwable instanceof HttpException && ((HttpException) throwable).code() == 401);
    }

    @Test
    public void first_query_expect_token_refresh() {
        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        SomeFakeObject someFakeObject = getSomeFakeObject();
        mockWebServer.enqueue(getFakeObjectResponse(someFakeObject));

        fakeAPIService.getSomething(42)
                .subscribe();
        refreshTokenProbe.assertSubscribed();
    }

    @Test
    public void multiple_requests_expect_cache_use() {
        FakeAPIService fakeAPIService = retrofit.create(FakeAPIService.class);
        SomeFakeObject someFakeObject = getSomeFakeObject();
        mockWebServer.enqueue(getFakeObjectResponse(someFakeObject));
        mockWebServer.enqueue(getFakeObjectResponse(someFakeObject));

        fakeAPIService.getSomething(42)
                .subscribe();
        refreshTokenProbe.assertSubscribed();
        fakeAPIService.getSomethingElse(42)
                .subscribe();
        refreshTokenProbe.assertSubscribedCount(1);
    }


}