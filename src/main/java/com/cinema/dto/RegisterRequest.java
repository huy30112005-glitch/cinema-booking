package com.cinema.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String tenNguoiDung;
    private String matKhau;
    private String email;
    private String sdt;
}
