package com.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VeDTO {
    private Integer maVe;
    private Double gia;
    private Boolean trangThai;
    private String maQR;
    private String soGhe;
    private Integer maSuatChieu;
    private String tenPhim;
    private String tenPhong;
    private String thoiGianBatDau;
}
