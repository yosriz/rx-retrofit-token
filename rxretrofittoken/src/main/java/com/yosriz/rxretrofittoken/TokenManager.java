package com.yosriz.rxretrofittoken;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
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

    String getToken() {
        synchronized (tokenLock) {
            return token;
        }
    }

    Observable getTokenizedObservable(Observable<?> requestObservable) {
        return cachedTokenObservable
                .toCompletable()
                .andThen(requestObservable)
                .retry((integer, throwable) -> {
                    if (integer <= maxRetries && expirationPredicate.test(throwable)) {
                        expired.set(true);
                        return true;
                    }
                    return false;
                });
    }

    public Completable getTokenizedObservable(Completable requestCompletable) {
        return cachedTokenObservable
                .toCompletable()
                .andThen(requestCompletable)
                .retry((integer, throwable) -> {
                    if (integer <= maxRetries && expirationPredicate.test(throwable)) {
                        expired.set(true);
                        return true;
                    }
                    return false;
                });
    }

    public Flowable<?> getTokenizedObservable(Flowable<?> requestFlowable) {
        return cachedTokenObservable
                .toCompletable()
                .andThen(requestFlowable)
                .retry((integer, throwable) -> {
                    if (integer <= maxRetries && expirationPredicate.test(throwable)) {
                        expired.set(true);
                        return true;
                    }
                    return false;
                });
    }

    public Maybe<?> getTokenizedObservable(Maybe<?> requestMaybe) {
        return cachedTokenObservable
                .toCompletable()
                .andThen(requestMaybe)
                .retry((integer, throwable) -> {
                    if (integer <= maxRetries && expirationPredicate.test(throwable)) {
                        expired.set(true);
                        return true;
                    }
                    return false;
                });
    }

    public Single<?> getTokenizedObservable(Single<?> requestSingle) {
        return cachedTokenObservable
                .toCompletable()
                .andThen(requestSingle)
                .retry((integer, throwable) -> {
                    if (integer <= maxRetries && expirationPredicate.test(throwable)) {
                        expired.set(true);
                        return true;
                    }
                    return false;
                });
    }
}
