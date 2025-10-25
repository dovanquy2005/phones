package com.fonestore.user_api.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class StaffApiClient {
    private final WebClient staffWebClient;

    public StaffApiClient(WebClient staffWebClient) {
        this.staffWebClient = staffWebClient;
    }

    public Mono<String> getProductsRaw(Map<String, String> params) {
        return staffWebClient.get()
                .uri(uri -> {
                    var b = uri.path("/api/products");
                    if (params != null) params.forEach(b::queryParam);
                    return b.build();
                })
                .retrieve()
                .bodyToMono(String.class);
    }
}
