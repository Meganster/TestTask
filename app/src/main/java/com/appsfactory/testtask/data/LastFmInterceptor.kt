package com.appsfactory.testtask.data

import okhttp3.Interceptor
import okhttp3.Response

class LastFmInterceptor(
    private val apiKey: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val url = original.url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("format", "json")
            .build()

        // Request customization: add request headers
        val requestBuilder = original
            .newBuilder()
            .url(url)

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}