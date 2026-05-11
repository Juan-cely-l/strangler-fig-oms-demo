package com.demo.gateway.service;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.dto.OrderItemRequest;
import com.demo.gateway.config.BackendProperties;
import com.demo.gateway.config.MigrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RoutingServiceTest {
    @Test
    void routesOrdersToLegacyWhenMigrationDisabled() {
        Fixture fixture = new Fixture(false);
        fixture.server.expect(once(), requestTo("http://legacy.test/api/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "corr-1"))
                .andRespond(withSuccess("{\"backend\":\"LEGACY\"}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.TRANSFER_ENCODING, "chunked"));

        var response = fixture.routingService.createOrder(request(), "corr-1");
        String body = response.getBody();

        assertThat(body).contains("LEGACY");
        assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.TRANSFER_ENCODING);
        fixture.server.verify();
    }

    @Test
    void routesOrdersToModernWhenMigrationEnabled() {
        Fixture fixture = new Fixture(true);
        fixture.server.expect(once(), requestTo("http://modern.test/api/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Correlation-Id", "corr-2"))
                .andRespond(withSuccess("{\"backend\":\"MODERN_ORDER\"}", MediaType.APPLICATION_JSON));

        String body = fixture.routingService.createOrder(request(), "corr-2").getBody();

        assertThat(body).contains("MODERN_ORDER");
        fixture.server.verify();
    }

    @Test
    void routesOrderLookupToModernWhenMigrationEnabled() {
        Fixture fixture = new Fixture(true);
        fixture.server.expect(once(), requestTo("http://modern.test/api/orders/42"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Correlation-Id", "corr-3"))
                .andRespond(withSuccess("{\"id\":42,\"backend\":\"MODERN_ORDER\",\"status\":\"CONFIRMED\"}", MediaType.APPLICATION_JSON));

        String body = fixture.routingService.getOrder(42L, "corr-3").getBody();

        assertThat(body).contains("MODERN_ORDER", "CONFIRMED");
        fixture.server.verify();
    }

    private CreateOrderRequest request() {
        return CreateOrderRequest.builder()
                .customerId("CUST-001")
                .items(List.of(OrderItemRequest.builder()
                        .sku("MOUSE-001")
                        .quantity(1)
                        .unitPrice(new BigDecimal("25.00"))
                        .build()))
                .build();
    }

    private static class Fixture {
        private final MockRestServiceServer server;
        private final RoutingService routingService;

        private Fixture(boolean ordersMigrationEnabled) {
            RestClient.Builder builder = RestClient.builder();
            this.server = MockRestServiceServer.bindTo(builder).build();
            BackendProperties backends = new BackendProperties();
            backends.setLegacyUrl("http://legacy.test");
            backends.setModernOrderUrl("http://modern.test");
            backends.setInventoryUrl("http://inventory.test");
            MigrationProperties properties = new MigrationProperties();
            properties.getOrders().setEnabled(ordersMigrationEnabled);
            this.routingService = new RoutingService(builder.build(), new MigrationState(properties), backends);
        }
    }
}
