package com.cinema.repository;

import com.cinema.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Integer> {

    @Query("""
            select b
            from Banner b
            order by
                case when b.thuTu is null then 1 else 0 end,
                b.thuTu,
                b.maBanner
            """)
    List<Banner> findAllOrderByThuTu();
}
