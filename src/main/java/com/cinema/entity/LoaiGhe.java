package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "LOAI_GHE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoaiGhe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Loai_Ghe")
    private Integer maLoaiGhe;

    @Column(name = "Ten_Loai_Ghe")
    private String tenLoaiGhe;

    @OneToMany(mappedBy = "loaiGhe")
    private List<Ghe> dsGhe;
}