package com.cinema.repository;

import com.cinema.entity.Phim;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PhimRepository extends JpaRepository<Phim, Integer> {

    @Query("""
            SELECT p
            FROM Phim p
            LEFT JOIN SuatChieu sc ON sc.phim = p
            LEFT JOIN Ve v ON v.suatChieu = sc
            GROUP BY p
            ORDER BY COUNT(v) DESC, p.maPhim ASC
            """)
    List<Phim> findHotPhim(Pageable pageable);
}
