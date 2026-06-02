package com.cinema.dto.payment;

public record MomoIpnResponse(
        String partnerCode,
        String orderId,
        String requestId,
        int resultCode,
        String message
) {
}
