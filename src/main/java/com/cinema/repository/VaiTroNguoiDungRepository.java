package com.cinema.repository;

import com.cinema.entity.VaiTroNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaiTroNguoiDungRepository extends JpaRepository<VaiTroNguoiDung, Integer> {
    Optional<VaiTroNguoiDung> findByTenVaiTro(String tenVaiTro);
}
