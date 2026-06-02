package com.cinema.repository;

import com.cinema.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NguoiDungRepository
        extends JpaRepository<NguoiDung, Integer> {

    NguoiDung findByTenNguoiDungAndMatKhau(
            String tenNguoiDung,
            String matKhau
    );

    boolean existsByTenNguoiDung(String tenNguoiDung);

    boolean existsByEmail(String email);

    boolean existsBySdt(Integer sdt);

    boolean existsByEmailAndMaNguoiDungNot(String email, Integer maNguoiDung);

    boolean existsBySdtAndMaNguoiDungNot(Integer sdt, Integer maNguoiDung);

}
