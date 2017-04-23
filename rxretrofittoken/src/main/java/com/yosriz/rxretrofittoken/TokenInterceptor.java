package com.yosriz.rxretrofittoken;

import java.io.IOException;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Predicate;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public abstract class TokenInterceptor implements Interceptor {

    private final TokenProvider tokenProvider;
    private final Predicate<Request> requestFilter;

    TokenInterceptor(@NonNull TokenProvider tokenProvider,
                     @Nullable Predicate<Request> requestFilter) {
        this.tokenProvider = tokenProvider;
        this.requestFilter = requestFilter;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (shouldInterceptRequest(chain)) {
            return addToken(chain, tokenProvider.getToken());
        }
        return chain.proceed(chain.request());
    }

    private boolean shouldInterceptRequest(Chain chain) {
        try {
            return requestFilter == null || requestFilter.test(chain.request());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public abstract Response addToken(Chain chain, String token) throws IOException;
}
