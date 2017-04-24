package com.yosriz.rxretrofittoken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class RxJava2TokenAdapterFactory extends CallAdapter.Factory {

    private RxJava2CallAdapterFactory wrappedCallAdapterFactory;
    private TokenManager tokenManager;

    RxJava2TokenAdapterFactory(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.wrappedCallAdapterFactory = RxJava2CallAdapterFactory.create();
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations,
                                 Retrofit retrofit) {
        CallAdapter<?, ?> callAdapter = wrappedCallAdapterFactory.get(returnType, annotations, retrofit);
        if (callAdapter == null)
            return null;

        Class<?> rawType = getRawType(returnType);

        boolean isCompletable = rawType == Completable.class;
        boolean isFlowable = rawType == Flowable.class;
        boolean isSingle = rawType == Single.class;
        boolean isMaybe = rawType == Maybe.class;


        return new TokenCallAdapter(tokenManager, callAdapter, isCompletable, isFlowable, isMaybe, isSingle);
    }

    private static class TokenCallAdapter<R> implements CallAdapter<R, Object> {

        private TokenManager tokenManager;
        private CallAdapter wrapped;
        private final boolean isCompletable;
        private final boolean isFlowable;
        private final boolean isMaybe;
        private final boolean isSingle;

        TokenCallAdapter(TokenManager tokenManager,
                                CallAdapter callAdapter, boolean isCompletable,
                                boolean isFlowable, boolean isMaybe, boolean isSingle) {
            this.tokenManager = tokenManager;
            wrapped = callAdapter;
            this.isCompletable = isCompletable;
            this.isFlowable = isFlowable;
            this.isMaybe = isMaybe;
            this.isSingle = isSingle;
        }

        @Override
        public Type responseType() {
            return wrapped.responseType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object adapt(Call<R> call) {
            if (isCompletable) {
                return tokenManager.getTokenizedCompletable((Completable) wrapped.adapt(call));
            }
            if (isFlowable) {
                return tokenManager.getTokenizedFlowable((Flowable) wrapped.adapt(call));
            }
            if (isMaybe) {
                return tokenManager.getTokenizedMaybe((Maybe) wrapped.adapt(call));
            }
            if (isSingle) {
                return tokenManager.getTokenizedSingle((Single) wrapped.adapt(call));
            }
            return tokenManager.getTokenizedObservable((Observable) wrapped.adapt(call));
        }

    }

}
