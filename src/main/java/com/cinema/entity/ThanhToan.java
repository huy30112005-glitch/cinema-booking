package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "THANH_TOAN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Thanh_Toan")
    private Integer maThanhToan;

    @Column(name = "Thoi_Gian_Thanh_Toan")
    private LocalDateTime thoiGianThanhToan;

    @Column(name = "Trang_Thai")
    private Boolean trangThai;

    @ManyToOne
    @JoinColumn(name = "Ma_Phuong_Thuc")
    private PhuongThucThanhToan phuongThuc;

    @ManyToOne
    @JoinColumn(name = "Ma_Don_Hang")
    private DonHang donHang;
}
