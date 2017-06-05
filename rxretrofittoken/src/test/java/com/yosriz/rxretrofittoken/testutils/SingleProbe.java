package com.yosriz.rxretrofittoken.testutils;

import android.annotation.SuppressLint;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;

import static org.junit.Assert.assertTrue;

@SuppressLint("DefaultLocale")
public class SingleProbe<T> implements SingleTransformer<T, T> {

    private AtomicInteger subscribes = new AtomicInteger(0);

    @Override
    public SingleSource<T> apply(Single<T> single) {
        return single
                .doOnSubscribe(disposable -> subscribes.incrementAndGet());
    }

    public void assertSubscribed() {
        assertTrue("no subscription happened", subscribes.get() > 0);
    }

    public void assertNotSubscribed() {
        int actual = subscribes.get();
        assertTrue(String.format("expected no subscriptions, but got %d", actual), actual == 0);
    }


    public void assertSubscribedCount(int count) {
        int actual = subscribes.get();
        assertTrue(String.format("expected %d subscriptions, but got %d", count, actual), actual == count);
    }
}
