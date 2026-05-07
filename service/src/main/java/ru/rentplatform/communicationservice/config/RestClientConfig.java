package ru.rentplatform.communicationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient catalogServiceRestClient(
            @Value("${app.catalog-service-url:http://localhost:8082}") String url) {
        return RestClient.create(url);
    }

    @Bean
    public RestClient userServiceRestClient(
            @Value("${app.user-service-url:http://localhost:8081}") String url) {
        return RestClient.create(url);
    }
}
