package com.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "DON_HANG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Don_Hang")
    private Integer maDonHang;

    @Column(name = "Thoi_Gian_Tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "Tong_Tien")
    private Double tongTien;

    @Column(name = "Trang_Thai")
    private Boolean trangThai;

    @ManyToOne
    @JoinColumn(name = "Ma_Nguoi_Dung")
    private NguoiDung nguoiDung;

    @OneToMany(mappedBy = "donHang")
    private List<ThanhToan> dsThanhToan;
}
