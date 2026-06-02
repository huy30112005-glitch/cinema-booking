package com.cinema.dto.payment;

public record CreatePaymentResponse(
        Integer maThanhToan,
        Integer maDonHang,
        Double tongTien,
        String paymentUrl,
        Long giuDen
) {
}
