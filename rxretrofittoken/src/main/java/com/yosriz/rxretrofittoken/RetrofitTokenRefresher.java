package com.yosriz.rxretrofittoken;

import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Predicate;
import okhttp3.Interceptor;
import okhttp3.Request;

public class RetrofitTokenRefresher {

    private final RxJava2TokenAdapterFactory rxJava2CallAdapterFactory;
    private Interceptor interceptor;

    private RetrofitTokenRefresher(@NonNull Single<String> tokenSingle,
                                   @NonNull Predicate<Throwable> tokenExpirationPredicate,
                                   @NonNull int maxRetries, @NonNull Builder.AuthMethod authMethod,
                                   @NonNull String authParamName,
                                   @Nullable Predicate<Request> requestFilter) {
        TokenManager tokenManager = new TokenManager(tokenSingle, tokenExpirationPredicate, maxRetries);

        switch (authMethod) {
            case HEADERS:
                interceptor = new HeadersTokenInterceptor(authParamName, tokenManager.getTokenProvider(), requestFilter);
                break;
            case QUERY_PARAMS:
                interceptor = new QueryParamsTokenInterceptor(authParamName, tokenManager.getTokenProvider(), requestFilter);
                break;
        }
        rxJava2CallAdapterFactory = new RxJava2TokenAdapterFactory(tokenManager);
    }

    public RxJava2TokenAdapterFactory rxJava2TokenAdapterFactory() {
        return rxJava2CallAdapterFactory;
    }

    public Interceptor tokenInterceptor() {
        return interceptor;
    }

    public static class Builder {

        public enum AuthMethod {
            HEADERS, QUERY_PARAMS
        }

        private AuthMethod authMethod;
        private String authParamName;
        private Predicate<Request> requestPredicate;
        private Single<String> tokenSingle;
        private Predicate<Throwable> tokenExpirationPredicate;
        private int maxRetries = 1;

        public Builder setTokenRequest(Single<String> requestSingle) {
            tokenSingle = requestSingle;
            return this;
        }

        public Builder setTokenExpirationChecker(Predicate<Throwable> checker) {
            tokenExpirationPredicate = checker;
            return this;
        }

        public Builder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder setAuth(AuthMethod method, String paramName) {
            return setAuth(method, paramName, null);
        }

        public Builder setAuth(AuthMethod method, String paramName,
                               @Nullable Predicate<Request> requestPredicate) {
            this.authMethod = method;
            this.authParamName = paramName;
            this.requestPredicate = requestPredicate;
            return this;
        }

        public RetrofitTokenRefresher build() {
            if (tokenSingle == null) {
                throw new IllegalStateException("Token retriever cannot be null!");
            }
            if (tokenExpirationPredicate == null) {
                throw new IllegalStateException("Token expiration checker be null!");
            }
            if (authMethod == null) {
                throw new IllegalStateException("Auth method cannot be null!");
            }
            if (authParamName == null) {
                throw new IllegalStateException("Auth param name cannot be null!");
            }
            return new RetrofitTokenRefresher(tokenSingle, tokenExpirationPredicate, maxRetries, authMethod, authParamName, requestPredicate);
        }
    }
}
