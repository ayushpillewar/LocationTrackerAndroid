package com.majboormajdoor.locationtracker.services;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

 class CustomHttpClientConfig {

    public static CloseableHttpClient createHttpClient() {

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofDays(5000))
                .setResponseTimeout(Timeout.ofDays(5000))
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }
}