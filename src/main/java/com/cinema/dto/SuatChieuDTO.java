package com.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SuatChieuDTO {
    private Integer maSuatChieu;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private Integer maPhong;
    private String tenPhong;
    private Integer maPhim;
    private String tenPhim;
}
