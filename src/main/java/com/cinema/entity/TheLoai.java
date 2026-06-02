package com.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "THE_LOAI")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheLoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_The_Loai")
    private Integer maTheLoai;

    @Column(name = "Ten_The_Loai")
    private String tenTheLoai;

    @OneToMany(mappedBy = "maTheLoai")
    @JsonIgnore
    private List<Phim> dsPhim;

}