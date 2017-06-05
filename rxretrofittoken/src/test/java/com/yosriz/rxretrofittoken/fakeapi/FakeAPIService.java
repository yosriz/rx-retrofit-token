package com.yosriz.rxretrofittoken.fakeapi;


import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FakeAPIService {

    @GET("something/{id}")
    Observable<SomeFakeObject> getSomething(@Path("id") long someId);

    @POST("somethingelse/{id}")
    Observable<SomeFakeObject> getSomethingElse(@Path("id") long someId);

}
