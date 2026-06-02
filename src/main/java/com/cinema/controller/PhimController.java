package com.cinema.controller;

import com.cinema.entity.Phim;
import com.cinema.service.PhimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/phim")
@CrossOrigin("*")
public class PhimController {

    @Autowired
    private PhimService phimService;

    @GetMapping
    public List<PhimResponse> getAllPhim() {
        return phimService.getAllPhim()
                .stream()
                .map(PhimResponse::from)
                .toList();
    }

    @GetMapping("/hot")
    public List<PhimHomeResponse> getHotPhim(
            @RequestParam(defaultValue = "4") int limit) {
        return phimService.getHotPhim(limit)
                .stream()
                .map(PhimHomeResponse::from)
                .toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Phim createPhim(@RequestBody Phim phim) {
        return phimService.savePhim(phim);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Phim createPhimWithPoster(
            @RequestParam String tenPhim,
            @RequestParam String thoiLuong,
            @RequestParam String moTa,
            @RequestParam Integer maDinhDang,
            @RequestParam Integer maTheLoai,
            @RequestParam(required = false) String trailer,
            @RequestParam(required = false) String doTuoi,
            @RequestPart(required = false) MultipartFile poster) {

        return phimService.savePhim(
                tenPhim,
                thoiLuong,
                moTa,
                maDinhDang,
                maTheLoai,
                trailer,
                doTuoi,
                poster
        );
    }

    @DeleteMapping("/{id}")
    public void deletePhim(@PathVariable Integer id) {
        phimService.deletePhim(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Phim updatePhim(@PathVariable Integer id,
            @RequestBody Phim phim) {

        return phimService.updatePhim(id, phim);

    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Phim updatePhimWithPoster(
            @PathVariable Integer id,
            @RequestParam String tenPhim,
            @RequestParam String thoiLuong,
            @RequestParam String moTa,
            @RequestParam Integer maDinhDang,
            @RequestParam Integer maTheLoai,
            @RequestParam(required = false) String trailer,
            @RequestParam(required = false) String doTuoi,
            @RequestPart(required = false) MultipartFile poster) {

        return phimService.updatePhim(
                id,
                tenPhim,
                thoiLuong,
                moTa,
                maDinhDang,
                maTheLoai,
                trailer,
                doTuoi,
                poster
        );
    }

    public record PhimResponse(
            Integer maPhim,
            String tenPhim,
            String thoiLuong,
            String moTa,
            String trailer,
            String doTuoi,
            String anhPoster,
            DinhDangResponse maDinhDang,
            TheLoaiResponse maTheLoai) {

        private static PhimResponse from(Phim phim) {
            return new PhimResponse(
                    phim.getMaPhim(),
                    phim.getTenPhim(),
                    phim.getThoiLuong(),
                    phim.getMoTa(),
                    phim.getTrailer(),
                    phim.getDoTuoi(),
                    phim.getAnhPoster(),
                    phim.getMaDinhDang() != null
                            ? new DinhDangResponse(
                                    phim.getMaDinhDang().getMaDinhDang(),
                                    phim.getMaDinhDang().getTenDinhDang())
                            : null,
                    phim.getMaTheLoai() != null
                            ? new TheLoaiResponse(
                                    phim.getMaTheLoai().getMaTheLoai(),
                                    phim.getMaTheLoai().getTenTheLoai())
                            : null
            );
        }
    }

    public record DinhDangResponse(
            Integer maDinhDang,
            String tenDinhDang) {
    }

    public record TheLoaiResponse(
            Integer maTheLoai,
            String tenTheLoai) {
    }

    public record PhimHomeResponse(
            Integer maPhim,
            String tenPhim,
            String thoiLuong,
            String anhPoster) {

        private static PhimHomeResponse from(Phim phim) {
            return new PhimHomeResponse(
                    phim.getMaPhim(),
                    phim.getTenPhim(),
                    phim.getThoiLuong(),
                    phim.getAnhPoster()
            );
        }
    }
}
