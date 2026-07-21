package com.buffturf.buffturf_backend.service;

import java.util.Map;

public interface PaymentService {
    Map<String, Object> createOrder(Double amount);
    boolean verifyPayment(String orderId, String paymentId, String signature);
}