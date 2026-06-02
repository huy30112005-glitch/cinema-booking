package com.cinema.repository;

import com.cinema.entity.Ve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VeRepository extends JpaRepository<Ve, Integer> {
    List<Ve> findBySuatChieu_MaSuatChieu(Integer maSuatChieu);

    boolean existsBySuatChieu_MaSuatChieuAndGhe_MaGhe(
            Integer maSuatChieu,
            Integer maGhe
    );

}
