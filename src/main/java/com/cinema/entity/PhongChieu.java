package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "PHONG_CHIEU")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhongChieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Phong")
    private Integer maPhong;

    @Column(name = "Tong_Cho")
    private Integer tongCho;

    @ManyToOne
    @JoinColumn(name = "Ma_Ten_Phong")
    private TenPhong tenPhong;

    @OneToMany(mappedBy = "phong")
    private List<Ghe> dsGhe;

    @OneToMany(mappedBy = "phong")
    private List<SuatChieu> dsSuatChieu;
}