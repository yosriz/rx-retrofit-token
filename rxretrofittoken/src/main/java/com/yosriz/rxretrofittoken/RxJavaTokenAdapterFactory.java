package com.yosriz.rxretrofittoken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class RxJavaTokenAdapterFactory extends CallAdapter.Factory {

    RxJava2CallAdapterFactory wrappedCallAdapterFactory;
    Single<?> accessTokenReteriver;

    private RxJavaTokenAdapterFactory() {
    }

    public static RxJavaTokenAdapterFactory create() {
        return new RxJavaTokenAdapterFactory();
    }

    @Override
    public CallAdapter<?, Observable<?>> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        CallAdapter<?, ?> callAdapter = wrappedCallAdapterFactory.get(returnType, annotations, retrofit);
        if (callAdapter == null)
            return null;
        
        Class<?> rawType = getRawType(returnType);
        String canonicalName = rawType.getCanonicalName();
        boolean isSingle = "rx.Single".equals(canonicalName);
        boolean isCompletable = "rx.Completable".equals(canonicalName);
        if (isSingle) {

        }

        if (isCompletable) {

        }
        return new RxJavaTokenCallAdapter((CallAdapter<?, Observable<?>>) callAdapter);
    }

    private static class RxJavaTokenCallAdapter<R> implements CallAdapter<R, Observable<R>> {

        CallAdapter<R, Observable<R>> wrapped;

        public RxJavaTokenCallAdapter(CallAdapter<R, Observable<R>> callAdapter) {
            wrapped = callAdapter;
        }

        @Override
        public Type responseType() {
            return wrapped.responseType();
        }

        @Override
        public Observable<R> adapt(Call<R> call) {
            Observable<R> sourceObservable = wrapped.adapt(call);
            return sourceObservable;
        }

    }


}
