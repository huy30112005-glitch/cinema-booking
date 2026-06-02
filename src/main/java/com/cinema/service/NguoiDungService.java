package com.cinema.service;

import com.cinema.dto.RegisterRequest;
import com.cinema.dto.UpdateProfileRequest;
import com.cinema.entity.NguoiDung;
import com.cinema.entity.VaiTroNguoiDung;
import com.cinema.repository.NguoiDungRepository;
import com.cinema.repository.VaiTroNguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NguoiDungService {

    @Autowired
    private NguoiDungRepository repo;

    @Autowired
    private VaiTroNguoiDungRepository vaiTroRepo;

    public NguoiDung login(String tenNguoiDung, String matKhau) {
        return repo.findByTenNguoiDungAndMatKhau(tenNguoiDung, matKhau);
    }

    public NguoiDung register(RegisterRequest req) {

        String tenNguoiDung = req.getTenNguoiDung() != null
                ? req.getTenNguoiDung().trim()
                : "";
        String email = req.getEmail() != null
                ? req.getEmail().trim()
                : "";
        String matKhau = req.getMatKhau() != null
                ? req.getMatKhau()
                : "";
        String sdtText = req.getSdt() != null
                ? req.getSdt().trim()
                : "";

        if (tenNguoiDung.isBlank() || matKhau.isBlank() || email.isBlank() || sdtText.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin");
        }

        if (tenNguoiDung.length() > 50) {
            throw new IllegalArgumentException("Tên đăng nhập tối đa 50 ký tự");
        }

        if (matKhau.length() > 50) {
            throw new IllegalArgumentException("Mật khẩu tối đa 50 ký tự");
        }

        if (email.length() > 30) {
            throw new IllegalArgumentException("Email tối đa 30 ký tự");
        }

        if (!sdtText.matches("\\d+")) {
            throw new IllegalArgumentException("Số điện thoại chỉ được nhập số");
        }

        Integer sdt;

        try {
            sdt = Integer.parseInt(sdtText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }

        if (repo.existsByTenNguoiDung(tenNguoiDung)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }

        if (repo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        if (repo.existsBySdt(sdt)) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại");
        }

        VaiTroNguoiDung vaiTroUser = vaiTroRepo.findByTenVaiTro("USER")
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò USER"));

        NguoiDung user = new NguoiDung();
        user.setTenNguoiDung(tenNguoiDung);
        user.setMatKhau(matKhau);
        user.setEmail(email);
        user.setSdt(sdt);
        user.setVaiTro(vaiTroUser);

        return repo.save(user);
    }

    public NguoiDung updateProfile(NguoiDung currentUser, UpdateProfileRequest req) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Chưa đăng nhập");
        }

        NguoiDung user = repo.findById(currentUser.getMaNguoiDung())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        String sdtText = req.getSdt() != null ? req.getSdt().trim() : "";

        if (email.isBlank() || sdtText.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ email và số điện thoại");
        }

        if (email.length() > 30) {
            throw new IllegalArgumentException("Email tối đa 30 ký tự");
        }

        if (!sdtText.matches("\\d+")) {
            throw new IllegalArgumentException("Số điện thoại chỉ được nhập số");
        }

        Integer sdt;

        try {
            sdt = Integer.parseInt(sdtText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }

        if (repo.existsByEmailAndMaNguoiDungNot(email, user.getMaNguoiDung())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        if (repo.existsBySdtAndMaNguoiDungNot(sdt, user.getMaNguoiDung())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại");
        }

        user.setEmail(email);
        user.setSdt(sdt);

        return repo.save(user);
    }

    public void changePassword(NguoiDung currentUser, String matKhauCu, String matKhauMoi) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Chưa đăng nhập");
        }

        NguoiDung user = repo.findById(currentUser.getMaNguoiDung())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        if (matKhauCu == null || matKhauCu.isBlank() || matKhauMoi == null || matKhauMoi.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ mật khẩu");
        }

        if (!matKhauCu.equals(user.getMatKhau())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }

        if (matKhauMoi.length() > 50) {
            throw new IllegalArgumentException("Mật khẩu tối đa 50 ký tự");
        }

        user.setMatKhau(matKhauMoi);
        repo.save(user);
    }
}
