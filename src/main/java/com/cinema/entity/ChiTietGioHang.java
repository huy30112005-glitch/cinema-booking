package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHI_TIET_GIO_HANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietGioHang {

    @EmbeddedId
    private ChiTietGioHangId id;

    @ManyToOne
    @MapsId("maVe")
    @JoinColumn(name = "Ma_Ve")
    private Ve ve;

    @ManyToOne
    @MapsId("maGioHang")
    @JoinColumn(name = "Ma_Gio_Hang")
    private GioHang gioHang;
}