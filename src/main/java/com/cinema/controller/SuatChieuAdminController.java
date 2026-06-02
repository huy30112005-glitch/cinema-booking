package com.cinema.controller;

import com.cinema.dto.SuatChieuDTO;
import com.cinema.dto.SuatChieuRequest;
import com.cinema.entity.Phim;
import com.cinema.entity.PhongChieu;
import com.cinema.entity.SuatChieu;
import com.cinema.repository.PhimRepository;
import com.cinema.repository.PhongChieuRepository;
import com.cinema.repository.SuatChieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/suatchieu")
@CrossOrigin("*")
public class SuatChieuAdminController {

    @Autowired
    private SuatChieuRepository suatChieuRepository;

    @Autowired
    private PhimRepository phimRepository;

    @Autowired
    private PhongChieuRepository phongChieuRepository;

    @GetMapping
    public List<SuatChieuDTO> getAllSuatChieu() {

        return suatChieuRepository.findAllByOrderByThoiGianBatDauAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createSuatChieu(@RequestBody SuatChieuRequest req) {

        Phim phim = phimRepository.findById(req.getMaPhim()).orElse(null);
        PhongChieu phong = phongChieuRepository.findById(req.getMaPhong()).orElse(null);

        if (phim == null || phong == null) {
            return ResponseEntity.badRequest().body("Phim hoặc phòng chiếu không tồn tại");
        }

        if (req.getThoiGianBatDau() == null || req.getThoiGianKetThuc() == null) {
            return ResponseEntity.badRequest().body("Vui lòng nhập thời gian bắt đầu và kết thúc");
        }

        if (!req.getThoiGianKetThuc().isAfter(req.getThoiGianBatDau())) {
            return ResponseEntity.badRequest().body("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        SuatChieu suatChieu = new SuatChieu();
        suatChieu.setPhim(phim);
        suatChieu.setPhong(phong);
        suatChieu.setThoiGianBatDau(req.getThoiGianBatDau());
        suatChieu.setThoiGianKetThuc(req.getThoiGianKetThuc());

        return ResponseEntity.ok(toDTO(suatChieuRepository.save(suatChieu)));
    }

    @DeleteMapping("/{id}")
    public void deleteSuatChieu(@PathVariable Integer id) {
        suatChieuRepository.deleteById(id);
    }

    private SuatChieuDTO toDTO(SuatChieu sc) {

        return new SuatChieuDTO(
                sc.getMaSuatChieu(),
                sc.getThoiGianBatDau(),
                sc.getThoiGianKetThuc(),
                sc.getPhong() != null ? sc.getPhong().getMaPhong() : null,
                sc.getPhong() != null && sc.getPhong().getTenPhong() != null
                        ? sc.getPhong().getTenPhong().getTenPhong()
                        : "",
                sc.getPhim() != null ? sc.getPhim().getMaPhim() : null,
                sc.getPhim() != null ? sc.getPhim().getTenPhim() : ""
        );
    }
}
