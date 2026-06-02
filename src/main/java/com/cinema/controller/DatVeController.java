package com.cinema.controller;

import com.cinema.dto.DatVeRequest;
import com.cinema.dto.GheDTO;
import com.cinema.dto.SuatChieuDTO;
import com.cinema.dto.VeDTO;
import com.cinema.entity.Ghe;
import com.cinema.entity.SuatChieu;

import com.cinema.repository.GheRepository;
import com.cinema.repository.SuatChieuRepository;
import com.cinema.service.SeatHoldService;
import com.cinema.service.VeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.cinema.entity.NguoiDung;

import java.util.List;


@RestController
@RequestMapping("/datve")
@CrossOrigin("*")
public class DatVeController {

    @Autowired
    private SuatChieuRepository suatChieuRepository;

    @Autowired
    private GheRepository gheRepository;

    @Autowired
    private VeService veService;

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/suatchieu/{maPhim}")
    public List<SuatChieuDTO> getSuatChieuTheoPhim(@PathVariable Integer maPhim) {

        return suatChieuRepository.findByPhim_MaPhimOrderByThoiGianBatDauAsc(maPhim)
                .stream()
                .map(sc -> new SuatChieuDTO(
                        sc.getMaSuatChieu(),
                        sc.getThoiGianBatDau(),
                        sc.getThoiGianKetThuc(),
                        sc.getPhong() != null ? sc.getPhong().getMaPhong() : null,
                        sc.getPhong() != null && sc.getPhong().getTenPhong() != null
                                ? sc.getPhong().getTenPhong().getTenPhong()
                                : "",
                        sc.getPhim() != null ? sc.getPhim().getMaPhim() : null,
                        sc.getPhim() != null ? sc.getPhim().getTenPhim() : ""
                ))
                .toList();
    }

    @GetMapping("/ghe/{maSuatChieu}")
    public ResponseEntity<?> getGheTheoSuatChieu(
            @PathVariable Integer maSuatChieu,
            HttpSession session) {

        try {

            Integer soLuongSuatChieu = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM SUAT_CHIEU WHERE Ma_Suat_Chieu = ?",
                    Integer.class,
                    maSuatChieu
            );

            if (soLuongSuatChieu == null || soLuongSuatChieu == 0) {
                return ResponseEntity.badRequest().body("Suất chiếu không tồn tại");
            }

            NguoiDung user = (NguoiDung) session.getAttribute("user");

            List<GheDTO> dsGhe = jdbcTemplate.query(
                    """
                            SELECT
                                g.Ma_Ghe,
                                g.So_Ghe,
                                CASE
                                    WHEN v.Ma_Ve IS NULL THEN 0
                                    ELSE 1
                                END AS Da_Dat
                            FROM GHE g
                            INNER JOIN SUAT_CHIEU sc
                                ON sc.Ma_Phong = g.Ma_Phong
                            LEFT JOIN VE v
                                ON v.Ma_Ghe = g.Ma_Ghe
                               AND v.Ma_Xuat_Chieu = sc.Ma_Suat_Chieu
                            WHERE sc.Ma_Suat_Chieu = ?
                            ORDER BY g.So_Ghe
                            """,
                    (rs, rowNum) -> new GheDTO(
                            rs.getInt("Ma_Ghe"),
                            rs.getString("So_Ghe"),
                            "",
                            rs.getInt("Da_Dat") == 1,
                            seatHoldService.isHeldByOtherUser(
                                    maSuatChieu,
                                    rs.getInt("Ma_Ghe"),
                                    user
                            ),
                            null
                    ),
                    maSuatChieu
            );

            return ResponseEntity.ok(dsGhe);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi tải ghế: " + e.getMessage());
        }
    }

    @PostMapping
public ResponseEntity<?> datVe(
        @RequestBody DatVeRequest req,
        HttpSession session) {

    NguoiDung user =
            (NguoiDung) session.getAttribute("user");

    if (user == null) {
        return ResponseEntity.status(401)
                .body(new ApiResponse(false,
                        "Vui lòng đăng nhập để đặt vé"));
    }

    SuatChieu suatChieu =
            suatChieuRepository.findById(req.getMaSuatChieu()).orElse(null);

    Ghe ghe =
            gheRepository.findById(req.getMaGhe()).orElse(null);

    if (suatChieu == null || ghe == null) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse(false,
                        "Suất chiếu hoặc ghế không tồn tại"));
    }

    try {
        VeDTO result = veService.emitTicket(suatChieu, ghe);
        return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage()));
    }
}

    public record ApiResponse(boolean success, String message) {
    }

}
