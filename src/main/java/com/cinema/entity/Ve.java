package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "VE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Ve")
    private Integer maVe;

    @Column(name = "Gia")
    private Double gia;

    @Column(name = "Ma_QR")
    private byte[] maQR;

    @Column(name = "Trang_Thai")
    private Boolean trangThai;

    @ManyToOne
    @JoinColumn(name = "Ma_Ghe")
    private Ghe ghe;

    @ManyToOne
    @JoinColumn(name = "Ma_Xuat_Chieu")
    private SuatChieu suatChieu;
}
