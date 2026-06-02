package com.cinema.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePaymentRequest {
    private Integer maSuatChieu;
    private Integer maGhe;
    private List<Integer> maGheList;
}
