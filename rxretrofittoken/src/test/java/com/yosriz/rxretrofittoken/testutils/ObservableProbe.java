package com.yosriz.rxretrofittoken.testutils;


import org.junit.Assert;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

public class ObservableProbe<T> implements ObservableTransformer<T, T> {

    private AtomicBoolean subscribe = new AtomicBoolean(false);

    @Override
    public ObservableSource<T> apply(Observable<T> observable) {
        return observable.doOnSubscribe(disposable -> subscribe.set(true));
    }

    public void assertSubscribed() {
        Assert.assertTrue(subscribe.get());
    }

    public void assertNotSubscribed() {
        Assert.assertFalse(subscribe.get());
    }

}
