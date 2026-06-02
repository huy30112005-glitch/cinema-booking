package com.cinema.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SuatChieuRequest {
    private Integer maPhim;
    private Integer maPhong;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
}
