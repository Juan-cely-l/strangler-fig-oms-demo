package com.demo.legacy.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentStubService {
    public void authorize(String customerId, String correlationId) {
        log.info("[LEGACY] paymentStub=AUTHORIZED customerId={} correlationId={}", customerId, correlationId);
    }
}
