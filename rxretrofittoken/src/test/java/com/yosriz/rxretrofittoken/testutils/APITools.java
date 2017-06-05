package com.yosriz.rxretrofittoken.testutils;

import com.google.gson.Gson;

import android.support.annotation.NonNull;

import com.yosriz.rxretrofittoken.fakeapi.SomeFakeObject;

import okhttp3.mockwebserver.MockResponse;

public class APITools {

    @NonNull
    public static SomeFakeObject getSomeFakeObject() {
        SomeFakeObject someFakeObject = new SomeFakeObject();
        someFakeObject.someData = "my_fake_data";
        someFakeObject.anotherData = 42;
        return someFakeObject;
    }

    public static MockResponse getFakeObjectResponse(SomeFakeObject someFakeObject) {
        Gson gson = new Gson();
        return new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(someFakeObject));
    }
}
