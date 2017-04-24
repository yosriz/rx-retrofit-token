package com.yosriz.rxretrofittoken;

import android.support.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Predicate;


class TokenManager {


    private final AtomicBoolean expired = new AtomicBoolean(true);

    private final Single<String> source;
    private final Single<String> cachedTokenObservable;
    private Single<String> current;
    private Predicate<Throwable> expirationPredicate;
    private final Object tokenLock = new Object();
    private String token;
    private final int maxRetries;
    private TokenProvider tokenProvider = new TokenProvider() {
        @Override
        public String getToken() {
            synchronized (tokenLock) {
                return token;
            }
        }
    };

    TokenManager(Single<String> tokenObservable,
                 Predicate<Throwable> expirationPredicate, int maxRetries) {
        this.maxRetries = maxRetries;
        source = tokenObservable;
        current = tokenObservable;
        this.expirationPredicate = expirationPredicate;

        cachedTokenObservable = Single.defer(() -> {
            if (expired.compareAndSet(true, false)) {
                current = source.doOnSuccess(token -> {
                    synchronized (tokenLock) {
                        this.token = token;
                    }
                })
                        .doOnError(throwable -> expired.set(true))
                        .cache();
            }
            return current;
        });
    }

    TokenProvider getTokenProvider() {
        return tokenProvider;
    }

    Observable getTokenizedObservable(Observable<?> requestObservable) {
        return getCachedTokenCompletable()
                .andThen(requestObservable)
                .retry(createTokenRetryPredicate());
    }

    private Completable getCachedTokenCompletable() {
        return cachedTokenObservable
                .toCompletable();
    }

    @NonNull
    private BiPredicate<Integer, Throwable> createTokenRetryPredicate() {
        return (integer, throwable) -> {
            if (integer <= maxRetries && expirationPredicate.test(throwable)) {
                expired.set(true);
                return true;
            }
            return false;
        };
    }

    Completable getTokenizedCompletable(Completable requestCompletable) {
        return getCachedTokenCompletable()
                .andThen(requestCompletable)
                .retry(createTokenRetryPredicate());
    }

    Flowable<?> getTokenizedFlowable(Flowable<?> requestFlowable) {
        return getCachedTokenCompletable()
                .andThen(requestFlowable)
                .retry(createTokenRetryPredicate());
    }

    Maybe<?> getTokenizedMaybe(Maybe<?> requestMaybe) {
        return getCachedTokenCompletable()
                .andThen(requestMaybe)
                .retry(createTokenRetryPredicate());
    }

    Single<?> getTokenizedSingle(Single<?> requestSingle) {
        return getCachedTokenCompletable()
                .andThen(requestSingle)
                .retry(createTokenRetryPredicate());
    }
}
