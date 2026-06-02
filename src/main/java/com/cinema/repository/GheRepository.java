package com.cinema.repository;

import com.cinema.dto.GheDatVeView;
import com.cinema.entity.Ghe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GheRepository extends JpaRepository<Ghe, Integer> {
    List<Ghe> findByPhong_MaPhongOrderBySoGheAsc(Integer maPhong);

    @Query(value = """
            SELECT
                g.Ma_Ghe AS maGhe,
                g.So_Ghe AS soGhe,
                CAST(
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM VE v
                            WHERE v.Ma_Suat_Chieu = :maSuatChieu
                              AND v.Ma_Ghe = g.Ma_Ghe
                              AND UPPER(v.Trang_Thai) = 'DA_DAT'
                        )
                        THEN 1
                        ELSE 0
                    END
                AS bit) AS daDat
            FROM GHE g
            JOIN SUAT_CHIEU sc ON sc.Ma_Phong = g.Ma_Phong
            WHERE sc.Ma_Suat_Chieu = :maSuatChieu
            ORDER BY g.So_Ghe
            """, nativeQuery = true)
    List<GheDatVeView> findGheDatVeBySuatChieu(@Param("maSuatChieu") Integer maSuatChieu);
}
