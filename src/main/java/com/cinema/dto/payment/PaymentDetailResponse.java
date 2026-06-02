package com.cinema.dto.payment;

import java.util.List;

public record PaymentDetailResponse(
        Integer maThanhToan,
        Integer maDonHang,
        Double tongTien,
        String trangThai,
        Integer maSuatChieu,
        Integer maGhe,
        List<Integer> maGheList,
        Long giuDen
) {
}
