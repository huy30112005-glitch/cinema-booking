package com.cinema.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dùng nội bộ khi phát hành vé sau thanh toán.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEmitTicketRequest {
    private Integer maSuatChieu;
    private Integer maGhe;
}

