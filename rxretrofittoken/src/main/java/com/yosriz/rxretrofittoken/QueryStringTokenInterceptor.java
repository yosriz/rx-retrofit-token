package com.yosriz.rxretrofittoken;

import java.io.IOException;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Predicate;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class QueryStringTokenInterceptor extends TokenInterceptor {

    private final String authParam;

    QueryStringTokenInterceptor(@NonNull String authParam, @NonNull TokenProvider tokenProvider,
                                @Nullable Predicate<Request> requestFilter) {
        super(tokenProvider, requestFilter);
        this.authParam = authParam;
    }

    @Override
    public Response addToken(Chain chain, String token) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url()
                .newBuilder()
                .addQueryParameter(authParam, token)
                .build();
        request = request.newBuilder()
                .url(url)
                .build();
        return chain.proceed(request);
    }

}
