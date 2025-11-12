package com.fonestore.user_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;



@Configuration  
public class WebClientConfig {

    @Bean
    public WebClient staffWebClient(@Value("${staff.api.base-url:http://localhost:9090}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // chuyển tiếp Authorization header nếu request hiện tại có
                .filter(authorizationForwardingFilter())
                // optional: log request/response (giúp debug)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction authorizationForwardingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req = attrs.getRequest();                      // <-- dùng var để tránh mismatch javax/jakarta
                String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    ClientRequest newReq = ClientRequest.from(clientRequest)
                            .headers(h -> h.set(HttpHeaders.AUTHORIZATION, auth))
                            .build();
                    return Mono.just(newReq);
                }
            }
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            // ví dụ log đơn giản (tốt nhất dùng logger)
            System.out.println("[WebClient] " + req.method() + " " + req.url());
            return Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            System.out.println("[WebClient] response status: " + resp.statusCode());
            return Mono.just(resp);
        });
    }
}
