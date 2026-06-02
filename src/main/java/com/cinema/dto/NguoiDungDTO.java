package com.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NguoiDungDTO {
    private Integer maNguoiDung;
    private String tenNguoiDung;
    private String email;
    private Integer sdt;
    private String vaiTro;
}
