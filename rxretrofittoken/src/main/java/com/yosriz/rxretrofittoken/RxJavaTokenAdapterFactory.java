package com.yosriz.rxretrofittoken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class RxJavaTokenAdapterFactory extends CallAdapter.Factory {

    private RxJava2CallAdapterFactory wrappedCallAdapterFactory;
    private TokenManager tokenManager;

    private RxJavaTokenAdapterFactory(Single<String> tokenSingle,
                                      Predicate<Throwable> tokenExpirationPredicate,
                                      int maxRetries) {
        tokenManager = new TokenManager(tokenSingle, tokenExpirationPredicate, maxRetries);
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

    public static class Builder {
        private Single<String> tokenSingle;
        private Predicate<Throwable> tokenExpirationPredicate;
        private int maxRetries;

        public Builder setTokenReteriver(Single<String> reteriver) {
            tokenSingle = reteriver;
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

        public RxJavaTokenAdapterFactory build() {
            return new RxJavaTokenAdapterFactory(tokenSingle, tokenExpirationPredicate, maxRetries);
        }
    }

    private static class TokenCallAdapter<R> implements CallAdapter<R, Object> {

        private TokenManager tokenManager;
        private CallAdapter wrapped;
        private final boolean isCompletable;
        private final boolean isFlowable;
        private final boolean isMaybe;
        private final boolean isSingle;

        public TokenCallAdapter(TokenManager tokenManager,
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

        @Override
        public Object adapt(Call<R> call) {
            if (isCompletable) {
                return tokenManager.getTokenizedObservable((Completable) wrapped.adapt(call));
            }
            if (isFlowable) {
                return tokenManager.getTokenizedObservable((Flowable) wrapped.adapt(call));
            }
            if (isMaybe) {
                return tokenManager.getTokenizedObservable((Maybe) wrapped.adapt(call));
            }
            if (isSingle) {
                return tokenManager.getTokenizedObservable((Single) wrapped.adapt(call));
            }
            return tokenManager.getTokenizedObservable((Observable) wrapped.adapt(call));
        }

    }

}
