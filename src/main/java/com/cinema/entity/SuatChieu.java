package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "SUAT_CHIEU")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuatChieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Suat_Chieu")
    private Integer maSuatChieu;

    @Column(name = "Thoi_Gian_Bat_Dau")
    private LocalDateTime thoiGianBatDau;

    @Column(name = "Thoi_Gian_Ket_Thuc")
    private LocalDateTime thoiGianKetThuc;

    @ManyToOne
    @JoinColumn(name = "Ma_Phim")
    private Phim phim;

    @ManyToOne
    @JoinColumn(name = "Ma_Phong")
    private PhongChieu phong;

    @OneToMany(mappedBy = "suatChieu")
    private List<Ve> dsVe;
}