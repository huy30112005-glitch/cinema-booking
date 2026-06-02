package com.cinema.controller;

import com.cinema.entity.Banner;
import com.cinema.entity.NguoiDung;
import com.cinema.service.BannerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/banner")
@CrossOrigin("*")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public List<Banner> getAllBanner() {
        return bannerService.getAllBanner();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createBanner(
            @RequestBody Banner banner,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được thêm banner");
        }

        try {
            return ResponseEntity.ok(bannerService.saveBanner(banner));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBannerWithImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam(required = false) Integer thuTu,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được thêm banner");
        }

        try {
            return ResponseEntity.ok(bannerService.saveBanner(image, thuTu));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBanner(
            @PathVariable Integer id,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được xóa banner");
        }

        bannerService.deleteBanner(id);
        return ResponseEntity.ok("Xóa banner thành công");
    }

    private boolean isAdmin(HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("user");
        String vaiTro = user != null && user.getVaiTro() != null
                ? user.getVaiTro().getTenVaiTro()
                : "";

        return "ADMIN".equalsIgnoreCase(vaiTro);
    }
}
