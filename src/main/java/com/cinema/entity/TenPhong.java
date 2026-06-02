package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "TEN_PHONG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenPhong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Ten_Phong")
    private Integer maTenPhong;

    @Column(name = "Ten_Phong")
    private String tenPhong;

    @OneToMany(mappedBy = "tenPhong")
    private List<PhongChieu> dsPhong;
}