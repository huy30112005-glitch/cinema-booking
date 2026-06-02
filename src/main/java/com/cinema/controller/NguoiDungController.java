package com.cinema.controller;

import com.cinema.dto.ChangePasswordRequest;
import com.cinema.dto.LoginRequest;
import com.cinema.dto.NguoiDungDTO;
import com.cinema.dto.RegisterRequest;
import com.cinema.dto.UpdateProfileRequest;
import com.cinema.entity.NguoiDung;
import com.cinema.service.NguoiDungService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/nguoidung")
@CrossOrigin("*")
public class NguoiDungController {

    @Autowired
    private NguoiDungService service;

@PostMapping("/login")
public ResponseEntity<?> login(
        @RequestBody LoginRequest req,
        HttpSession session) {

    NguoiDung u = service.login(req.getTenNguoiDung(), req.getMatKhau());

    if (u == null) {
        return ResponseEntity.status(401)
                .body("Sai tài khoản hoặc mật khẩu");
    }

    session.setAttribute("user", u);

    return ResponseEntity.ok(toDTO(u));
}

@PostMapping("/register")
public ResponseEntity<?> register(
        @RequestBody RegisterRequest req,
        HttpSession session) {

    try {
        NguoiDung u = service.register(req);
        session.setAttribute("user", u);
        return ResponseEntity.ok(toDTO(u));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (DataIntegrityViolationException e) {
        return ResponseEntity.badRequest().body("Thông tin đăng ký đã tồn tại hoặc không hợp lệ");
    }
}

@GetMapping("/me")
public ResponseEntity<?> me(HttpSession session) {
    NguoiDung user = (NguoiDung) session.getAttribute("user");

    if (user == null) {
        return ResponseEntity.status(401).body("Chưa đăng nhập");
    }

    return ResponseEntity.ok(toDTO(user));
}

@PostMapping("/logout")
public ResponseEntity<?> logout(HttpSession session) {
    session.invalidate();
    return ResponseEntity.ok("Đăng xuất thành công");
}

@PutMapping("/me")
public ResponseEntity<?> updateProfile(
        @RequestBody UpdateProfileRequest req,
        HttpSession session) {

    NguoiDung user = (NguoiDung) session.getAttribute("user");

    if (user == null) {
        return ResponseEntity.status(401).body("Chưa đăng nhập");
    }

    try {
        NguoiDung updated = service.updateProfile(user, req);
        session.setAttribute("user", updated);
        return ResponseEntity.ok(toDTO(updated));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (DataIntegrityViolationException e) {
        return ResponseEntity.badRequest().body("Thông tin cập nhật đã tồn tại hoặc không hợp lệ");
    }
}

@PutMapping("/password")
public ResponseEntity<?> changePassword(
        @RequestBody ChangePasswordRequest req,
        HttpSession session) {

    NguoiDung user = (NguoiDung) session.getAttribute("user");

    if (user == null) {
        return ResponseEntity.status(401).body("Chưa đăng nhập");
    }

    try {
        service.changePassword(user, req.getMatKhauCu(), req.getMatKhauMoi());
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

private NguoiDungDTO toDTO(NguoiDung u) {
    return new NguoiDungDTO(
            u.getMaNguoiDung(),
            u.getTenNguoiDung(),
            u.getEmail(),
            u.getSdt(),
            u.getVaiTro() != null ? u.getVaiTro().getTenVaiTro() : null
    );
}
}
