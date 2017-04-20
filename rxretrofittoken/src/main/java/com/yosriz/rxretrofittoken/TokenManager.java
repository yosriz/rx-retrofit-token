package com.yosriz.rxretrofittoken;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;


public class TokenManager<T> {


    private final AtomicBoolean expired = new AtomicBoolean(true);
    private final Observable<T> source;
    private final Observable<T> cachedTokenObservable;
    private Observable<T> current;
    private Predicate<Throwable> expirationPredicate;

    TokenManager(Observable<T> tokenObservable,
                 Predicate<Throwable> expirationPredicate) {
        source = tokenObservable;
        current = tokenObservable;
        this.expirationPredicate = expirationPredicate;

        cachedTokenObservable = Observable.defer(() -> {
            if (expired.compareAndSet(true, false)) {
                current = source.cache();
            }
            return current;
        });
    }

    <R> Observable<R> getTokenizedObservable(Observable<R> requestObservable) {
        return requestObservable.retryWhen(errors ->
                errors.<T>flatMap(throwable -> {
                    try {
                        if (expirationPredicate.test(throwable)) {
                            expired.set(true);
                            return cachedTokenObservable;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return Observable.error(throwable);
                }));
    }

}
