package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GHE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ghe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Ghe")
    private Integer maGhe;

    @Column(name = "So_Ghe")
    private String soGhe;

    @Column(name = "Trang_Thai")
    private String trangThai;

    @ManyToOne
    @JoinColumn(name = "Ma_Loai_Ghe")
    private LoaiGhe loaiGhe;

    @ManyToOne
    @JoinColumn(name = "Ma_Phong")
    private PhongChieu phong;
}