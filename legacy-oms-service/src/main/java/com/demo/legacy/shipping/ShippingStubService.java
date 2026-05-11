package com.demo.legacy.shipping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ShippingStubService {
    public void prepareShipment(String correlationId) {
        log.info("[LEGACY] shippingStub=PREPARED correlationId={}", correlationId);
    }
}
