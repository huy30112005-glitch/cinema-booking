package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "GIO_HANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Gio_Hang")
    private Integer maGioHang;

    @ManyToOne
    @JoinColumn(name = "Ma_Nguoi_Dung")
    private NguoiDung nguoiDung;

    @OneToMany(mappedBy = "gioHang")
    private List<ChiTietGioHang> chiTietGioHang;
}