package com.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KiemTraVeResponse {
    private boolean hopLe;
    private String trangThai;
    private String message;
    private Integer maVe;
    private String tenPhim;
    private String soGhe;
    private String tenPhong;
    private String thoiGianBatDau;
}
