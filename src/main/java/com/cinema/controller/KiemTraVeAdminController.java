package com.cinema.controller;

import com.cinema.dto.KiemTraVeRequest;
import com.cinema.entity.NguoiDung;
import com.cinema.service.KiemTraVeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ve")
public class KiemTraVeAdminController {

    private final KiemTraVeService kiemTraVeService;

    public KiemTraVeAdminController(KiemTraVeService kiemTraVeService) {
        this.kiemTraVeService = kiemTraVeService;
    }

    @PostMapping("/kiem-tra")
    public ResponseEntity<?> kiemTraVe(
            @RequestBody KiemTraVeRequest request,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được kiểm tra vé");
        }

        try {
            return ResponseEntity.ok(kiemTraVeService.traCuu(request.getMaQr()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/xac-nhan")
    public ResponseEntity<?> xacNhanSuDungVe(
            @RequestBody KiemTraVeRequest request,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được xác nhận sử dụng vé");
        }

        try {
            return ResponseEntity.ok(kiemTraVeService.xacNhanSuDung(request.getMaQr()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean isAdmin(HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("user");
        String vaiTro = user != null && user.getVaiTro() != null
                ? user.getVaiTro().getTenVaiTro()
                : "";

        return "ADMIN".equalsIgnoreCase(vaiTro);
    }
}
