package com.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "NGUOI_DUNG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Nguoi_Dung")
    private Integer maNguoiDung;

    @Column(name = "Ten_Nguoi_Dung")
    private String tenNguoiDung;

    @Column(name = "Mat_Khau")
    private String matKhau;

    @Column(name = "Email")
    private String email;

    @Column(name = "SDT")
    private Integer sdt;

    @ManyToOne
    @JoinColumn(name = "Ma_Vai_Tro")
    @JsonIgnore
    private VaiTroNguoiDung vaiTro;

    @OneToMany(mappedBy = "nguoiDung")
    @JsonIgnore
    private List<GioHang> dsGioHang;

    @OneToMany(mappedBy = "nguoiDung")
    @JsonIgnore
    private List<DonHang> dsDonHang;
}
