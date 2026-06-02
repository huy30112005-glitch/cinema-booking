package com.cinema.controller;

import com.cinema.entity.NguoiDung;
import com.cinema.service.AdminReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public ResponseEntity<?> report(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được xem báo cáo");
        }

        return ResponseEntity.ok(adminReportService.getReport(fromDate, toDate));
    }

    private boolean isAdmin(HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("user");
        String vaiTro = user != null && user.getVaiTro() != null
                ? user.getVaiTro().getTenVaiTro()
                : "";

        return "ADMIN".equalsIgnoreCase(vaiTro);
    }
}
