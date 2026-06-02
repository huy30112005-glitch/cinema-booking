package com.cinema.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ChiTietGioHangId implements Serializable {

    @Column(name = "Ma_Ve")
    private Integer maVe;

    @Column(name = "Ma_Gio_Hang")
    private Integer maGioHang;

    public ChiTietGioHangId() {
    }

    public ChiTietGioHangId(Integer maVe, Integer maGioHang) {
        this.maVe = maVe;
        this.maGioHang = maGioHang;
    }

    public Integer getMaVe() {
        return maVe;
    }

    public void setMaVe(Integer maVe) {
        this.maVe = maVe;
    }

    public Integer getMaGioHang() {
        return maGioHang;
    }

    public void setMaGioHang(Integer maGioHang) {
        this.maGioHang = maGioHang;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChiTietGioHangId)) return false;
        ChiTietGioHangId that = (ChiTietGioHangId) o;
        return Objects.equals(maVe, that.maVe)
                && Objects.equals(maGioHang, that.maGioHang);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maVe, maGioHang);
    }
}