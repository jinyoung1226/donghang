package com.ebiz.wsb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(){
        return WebClient.builder()
                .baseUrl("http://localhost:8080")
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=utf-8");
                })
                .build();
    }
}
