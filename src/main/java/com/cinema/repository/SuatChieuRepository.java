package com.cinema.repository;

import com.cinema.entity.SuatChieu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuatChieuRepository extends JpaRepository<SuatChieu, Integer> {
    List<SuatChieu> findByPhim_MaPhimOrderByThoiGianBatDauAsc(Integer maPhim);

    List<SuatChieu> findAllByOrderByThoiGianBatDauAsc();
}
