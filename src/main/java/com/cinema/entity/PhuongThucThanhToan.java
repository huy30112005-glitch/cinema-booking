package com.cinema.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "PHUONG_THUC_THANH_TOAN")
public class PhuongThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Phuong_Thuc")
    private Integer maPhuongThuc;

    @Column(name = "Ten_Phuong_Thuc")
    private String tenPhuongThuc;

    public Integer getMaPhuongThuc() {
        return maPhuongThuc;
    }

    public void setMaPhuongThuc(Integer maPhuongThuc) {
        this.maPhuongThuc = maPhuongThuc;
    }

    public String getTenPhuongThuc() {
        return tenPhuongThuc;
    }

    public void setTenPhuongThuc(String tenPhuongThuc) {
        this.tenPhuongThuc = tenPhuongThuc;
    }
}