package com.cinema.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PHIM")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Phim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Phim")
    private Integer maPhim;

    @Column(name = "Ten_Phim")
    private String tenPhim;

    @Column(name = "Thoi_Luong")
    private String thoiLuong;

    @Column(name = "Mo_Ta")
    private String moTa;

    @Column(name = "Trailer")
    private String trailer;

    @Column(name = "Do_Tuoi")
    private String doTuoi;

    @Column(name = "Anh_Poster")
    private String anhPoster;

    @ManyToOne
    @JoinColumn(name = "Ma_Dinh_Dang")
    private DinhDang maDinhDang;

    @ManyToOne
    @JoinColumn(name = "Ma_The_Loai")
    private TheLoai maTheLoai;

}
