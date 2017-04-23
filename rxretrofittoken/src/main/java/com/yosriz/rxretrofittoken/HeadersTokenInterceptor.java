package com.yosriz.rxretrofittoken;


import java.io.IOException;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Predicate;
import okhttp3.Request;
import okhttp3.Response;

public class HeadersTokenInterceptor extends TokenInterceptor {

    private final String authHeader;

    HeadersTokenInterceptor(@NonNull String authHeader, @NonNull TokenProvider tokenProvider,
                            @Nullable Predicate<Request> requestFilter) {
        super(tokenProvider, requestFilter);
        this.authHeader = authHeader;
    }

    @Override
    public Response addToken(Chain chain, String token) throws IOException {
        Request tokenizedRequest = chain.request()
                .newBuilder()
                .header(authHeader, token)
                .build();
        return chain.proceed(tokenizedRequest);
    }
}
