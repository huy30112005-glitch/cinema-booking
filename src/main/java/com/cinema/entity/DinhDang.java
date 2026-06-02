package com.cinema.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "DINH_DANG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DinhDang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Dinh_Dang")
    private Integer maDinhDang;

    @Column(name = "Ten_Dinh_Dang")
    private String tenDinhDang;

    @OneToMany(mappedBy = "maDinhDang")
    @JsonIgnore
    private List<Phim> dsPhim;

}