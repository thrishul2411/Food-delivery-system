package com.food.delivery.apigateway.filter;

import com.food.delivery.apigateway.util.JwtUtil; // Your JwtUtil
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public static final List<String> publicApiEndpoints = List.of(
            "/user-service/api/auth/register",
            "/user-service/api/auth/login",
            "/actuator" // Allow actuator base path
    );

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            System.out.println("API Gateway Auth Filter: Path = " + request.getURI().getPath());


            Predicate<ServerHttpRequest> isPublicApi = r -> publicApiEndpoints.stream()
                    .anyMatch(uri -> r.getURI().getPath().startsWith(uri) || r.getURI().getPath().startsWith("/actuator/"));


            if (isPublicApi.test(request)) {
                System.out.println("API Gateway Auth Filter: Public endpoint, skipping token validation.");
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                System.out.println("API Gateway Auth Filter: Authorization header missing.");
                return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }


            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("API Gateway Auth Filter: Authorization header is invalid or not Bearer token.");
                return onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (jwtUtil.validateToken(token)) {
                    System.out.println("API Gateway Auth Filter: Token validated successfully.");
                    return chain.filter(exchange);
                } else {
                    System.out.println("API Gateway Auth Filter: Token validation failed (invalid/expired).");
                    return onError(exchange, "JWT Token is invalid or expired", HttpStatus.UNAUTHORIZED);
                }
            } catch (Exception e) {
                System.err.println("API Gateway Auth Filter: Error during token validation: " + e.getMessage());
                return onError(exchange, "Error validating token", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        return response.setComplete();
    }

    public static class Config {
    }
}