package com.yosriz.rxretrofittoken;


import org.junit.Assert;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;

public class SingleProbe<T> implements SingleTransformer<T, T> {

    private AtomicBoolean subscribe = new AtomicBoolean(false);

    @Override
    public SingleSource<T> apply(Single<T> single) {
        return single
                .doOnSubscribe(disposable -> subscribe.set(true));
    }

    public void assertSubscribed() {
        Assert.assertTrue(subscribe.get());
    }

    public void assertNotSubscribed() {
        Assert.assertFalse(subscribe.get());
    }
}
