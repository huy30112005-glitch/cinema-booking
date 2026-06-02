package com.cinema.repository;

import com.cinema.entity.PhuongThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, Integer> {
    Optional<PhuongThucThanhToan> findByTenPhuongThuc(String tenPhuongThuc);
}
