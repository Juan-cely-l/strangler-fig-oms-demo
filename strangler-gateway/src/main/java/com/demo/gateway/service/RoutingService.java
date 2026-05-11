package com.demo.gateway.service;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.util.CorrelationConstants;
import com.demo.gateway.config.BackendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    private final RestClient restClient;
    private final MigrationState migrationState;
    private final BackendProperties backendProperties;

    public ResponseEntity<String> createOrder(CreateOrderRequest request, String correlationId) {
        if (migrationState.isOrdersEnabled()) {
            return routeWithFallback(HttpMethod.POST, "/orders", "/api/orders", request, correlationId,
                    Backend.MODERN_ORDER, Backend.LEGACY);
        }
        return routeWithoutFallback(HttpMethod.POST, "/orders", "/api/orders", request, correlationId, Backend.LEGACY);
    }

    public ResponseEntity<String> getOrder(Long id, String correlationId) {
        if (migrationState.isOrdersEnabled()) {
            return routeWithFallback(HttpMethod.GET, "/orders/" + id, "/api/orders/" + id, null, correlationId,
                    Backend.MODERN_ORDER, Backend.LEGACY);
        }
        return routeWithoutFallback(HttpMethod.GET, "/orders/" + id, "/api/orders/" + id, null, correlationId, Backend.LEGACY);
    }

    public ResponseEntity<String> getInventory(String sku, String correlationId) {
        if (migrationState.isInventoryEnabled()) {
            return routeWithFallback(HttpMethod.GET, "/inventory/" + sku, "/api/inventory/" + sku, null, correlationId,
                    Backend.INVENTORY, Backend.LEGACY);
        }
        return routeWithoutFallback(HttpMethod.GET, "/inventory/" + sku, "/api/inventory/" + sku, null, correlationId, Backend.LEGACY);
    }

    private ResponseEntity<String> routeWithFallback(
            HttpMethod method,
            String publicRoute,
            String backendPath,
            Object body,
            String correlationId,
            Backend primary,
            Backend fallback) {
        long start = System.nanoTime();
        Backend chosen = primary;
        try {
            ResponseEntity<String> response = forward(method, primary, backendPath, body, correlationId);
            logRoute(publicRoute, chosen, correlationId, start);
            return response;
        } catch (RestClientResponseException ex) {
            if (!ex.getStatusCode().is5xxServerError()) {
                logRoute(publicRoute, chosen, correlationId, start);
                return responseFrom(ex, correlationId);
            }
            log.warn("[GATEWAY] route={} backend={} correlationId={} fallbackReason=status{}",
                    publicRoute, primary, correlationId, ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            log.warn("[GATEWAY] route={} backend={} correlationId={} fallbackReason=unreachable",
                    publicRoute, primary, correlationId);
        } catch (RestClientException ex) {
            log.warn("[GATEWAY] route={} backend={} correlationId={} fallbackReason={}",
                    publicRoute, primary, correlationId, ex.getClass().getSimpleName());
        }

        chosen = fallback;
        ResponseEntity<String> response = forward(method, fallback, backendPath, body, correlationId);
        logRoute(publicRoute, chosen, correlationId, start);
        return response;
    }

    private ResponseEntity<String> routeWithoutFallback(
            HttpMethod method,
            String publicRoute,
            String backendPath,
            Object body,
            String correlationId,
            Backend backend) {
        long start = System.nanoTime();
        try {
            ResponseEntity<String> response = forward(method, backend, backendPath, body, correlationId);
            logRoute(publicRoute, backend, correlationId, start);
            return response;
        } catch (RestClientResponseException ex) {
            logRoute(publicRoute, backend, correlationId, start);
            return responseFrom(ex, correlationId);
        }
    }

    private ResponseEntity<String> forward(
            HttpMethod method,
            Backend backend,
            String path,
            Object body,
            String correlationId) {
        RestClient.RequestBodySpec spec = restClient.method(method)
                .uri(baseUrl(backend) + path)
                .accept(MediaType.APPLICATION_JSON)
                .header(CorrelationConstants.HEADER_NAME, correlationId);
        if (body != null) {
            spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return sanitizeResponse(spec.retrieve().toEntity(String.class), correlationId);
    }

    private ResponseEntity<String> responseFrom(RestClientResponseException ex, String correlationId) {
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .header(CorrelationConstants.HEADER_NAME, correlationId)
                .body(ex.getResponseBodyAsString());
    }

    private ResponseEntity<String> sanitizeResponse(ResponseEntity<String> response, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        response.getHeaders().forEach((name, values) -> {
            if (!isHopByHopHeader(name) && !CorrelationConstants.HEADER_NAME.equalsIgnoreCase(name)) {
                headers.addAll(name, values);
            }
        });
        headers.set(CorrelationConstants.HEADER_NAME, correlationId);
        return ResponseEntity.status(response.getStatusCode())
                .headers(headers)
                .body(response.getBody());
    }

    private boolean isHopByHopHeader(String name) {
        return HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    private String baseUrl(Backend backend) {
        return switch (backend) {
            case LEGACY -> backendProperties.getLegacyUrl();
            case MODERN_ORDER -> backendProperties.getModernOrderUrl();
            case INVENTORY -> backendProperties.getInventoryUrl();
        };
    }

    private void logRoute(String route, Backend backend, String correlationId, long start) {
        log.info("[GATEWAY] route={} backend={} correlationId={} latencyMs={}",
                route, backend, correlationId, (System.nanoTime() - start) / 1_000_000);
    }

    public enum Backend {
        LEGACY,
        MODERN_ORDER,
        INVENTORY
    }
}
