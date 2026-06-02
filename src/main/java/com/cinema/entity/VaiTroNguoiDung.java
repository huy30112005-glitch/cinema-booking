package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "VAI_TRO_NGUOI_DUNG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VaiTroNguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Vai_Tro")
    private Integer maVaiTro;

    @Column(name = "Ten_Vai_Tro")
    private String tenVaiTro;

    @OneToMany(mappedBy = "vaiTro")
    private List<NguoiDung> dsNguoiDung;
}